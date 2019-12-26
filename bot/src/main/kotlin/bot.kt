package com.seventeenthshard.harmony.bot

import com.seventeenthshard.harmony.events.MessageDeletion
import com.seventeenthshard.harmony.events.MessageEdit
import com.seventeenthshard.harmony.events.NewMessage
import com.seventeenthshard.harmony.events.ServerInfo
import com.sksamuel.avro4k.Avro
import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.EventDispatcher
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.guild.GuildUpdateEvent
import discord4j.core.event.domain.message.MessageBulkDeleteEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.MessageDeleteEvent
import discord4j.core.event.domain.message.MessageUpdateEvent
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.serializers.subject.RecordNameStrategy
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.serializer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.StringSerializer
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.Properties
import java.util.function.BiFunction

@ImplicitReflectionSerializer
inline fun <reified T : Any> producerRecord(
    topic: String,
    id: String,
    value: T
): ProducerRecord<String, GenericRecord> =
    ProducerRecord(topic, id, Avro.default.toRecord(T::class.serializer(), value))

@ImplicitReflectionSerializer
inline fun <reified DiscordEvent : Event, reified EmittedEvent : Any> EventDispatcher.map(
    producer: KafkaProducer<String, GenericRecord>,
    topic: String,
    noinline mapper: (DiscordEvent) -> Pair<Snowflake, Mono<EmittedEvent>>
) {
    flatMap<DiscordEvent, EmittedEvent>(producer, topic) {
        val (id, emitted) = mapper(it)
        Flux.zip(
            Mono.just(id),
            emitted,
            BiFunction { a: Snowflake, b: EmittedEvent -> a to b }
        )
    }
}

@ImplicitReflectionSerializer
inline fun <reified DiscordEvent : Event, reified EmittedEvent : Any> EventDispatcher.flatMap(
    producer: KafkaProducer<String, GenericRecord>,
    topic: String,
    noinline mapper: (DiscordEvent) -> Flux<Pair<Snowflake, EmittedEvent>>
) {
    on(DiscordEvent::class.java)
        .flatMap { mapper(it) }
        .map {
            try {
                producer.send(
                    producerRecord(
                        topic,
                        it.first.asString(),
                        it.second
                    )
                )
            } catch (e: SerializationException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        .subscribe()
}

@ImplicitReflectionSerializer
fun main() {
    val client = DiscordClientBuilder(
        System.getenv("BOT_TOKEN")
            ?: throw IllegalArgumentException("Bot token must be provided via BOT_TOKEN environment variable")
    ).build()

    val props = Properties()
    props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
    props[ProducerConfig.ACKS_CONFIG] = "all"
    props[ProducerConfig.RETRIES_CONFIG] = 0
    props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
    props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java
    props[KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY] = RecordNameStrategy::class.java
    props[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "http://localhost:8081"
    val producer = KafkaProducer<String, GenericRecord>(props)

    client.eventDispatcher.map<GuildCreateEvent, ServerInfo>(
        producer,
        "servers"
    ) {
        it.guild.id to ServerInfo.of(it.guild)
    }

    client.eventDispatcher.map<GuildUpdateEvent, ServerInfo>(
        producer,
        "servers"
    ) {
        it.current.id to ServerInfo.of(it.current)
    }

    client.eventDispatcher.map<MessageCreateEvent, NewMessage>(
        producer,
        "messages"
    ) {
        it.message.id to NewMessage.of(it.message)
    }

    client.eventDispatcher.map<MessageUpdateEvent, MessageEdit>(
        producer,
        "messages"
    ) {
        it.messageId to Mono.justOrEmpty(it.currentContent).zipWith(it.message)
            .flatMap { tuple ->
                MessageEdit.of(tuple.t1, tuple.t2.editedTimestamp.orElse(Instant.now()))
            }
    }

    client.eventDispatcher.map<MessageDeleteEvent, MessageDeletion>(
        producer,
        "messages"
    ) {
        it.messageId to MessageDeletion.of(Instant.now())
    }

    client.eventDispatcher.flatMap<MessageBulkDeleteEvent, MessageDeletion>(
        producer,
        "messages"
    ) { event ->
        Flux.fromIterable(event.messageIds).flatMap { id ->
            MessageDeletion.of(Instant.now()).map { id to it }
        }
    }

    client.login().block()
}
