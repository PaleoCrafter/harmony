package com.seventeenthshard.harmony.bot

import com.seventeenthshard.harmony.events.ChannelDeletion
import com.seventeenthshard.harmony.events.ChannelInfo
import com.seventeenthshard.harmony.events.MessageDeletion
import com.seventeenthshard.harmony.events.MessageEdit
import com.seventeenthshard.harmony.events.NewMessage
import com.seventeenthshard.harmony.events.RoleDeletion
import com.seventeenthshard.harmony.events.RoleInfo
import com.seventeenthshard.harmony.events.ServerDeletion
import com.seventeenthshard.harmony.events.ServerInfo
import com.seventeenthshard.harmony.events.UserNicknameChange
import com.sksamuel.avro4k.Avro
import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.entity.GuildMessageChannel
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.EventDispatcher
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.channel.NewsChannelCreateEvent
import discord4j.core.event.domain.channel.NewsChannelDeleteEvent
import discord4j.core.event.domain.channel.NewsChannelUpdateEvent
import discord4j.core.event.domain.channel.TextChannelCreateEvent
import discord4j.core.event.domain.channel.TextChannelDeleteEvent
import discord4j.core.event.domain.channel.TextChannelUpdateEvent
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.guild.GuildDeleteEvent
import discord4j.core.event.domain.guild.GuildUpdateEvent
import discord4j.core.event.domain.guild.MemberUpdateEvent
import discord4j.core.event.domain.message.MessageBulkDeleteEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.MessageDeleteEvent
import discord4j.core.event.domain.message.MessageUpdateEvent
import discord4j.core.event.domain.role.RoleCreateEvent
import discord4j.core.event.domain.role.RoleDeleteEvent
import discord4j.core.event.domain.role.RoleUpdateEvent
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
import org.apache.logging.log4j.LogManager
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.Properties
import java.util.function.BiFunction

val logger = LogManager.getLogger("bot")

@ImplicitReflectionSerializer
inline fun <reified T : Any> producerRecord(
    topic: String,
    id: Snowflake,
    value: T
): ProducerRecord<String, GenericRecord> =
    ProducerRecord(topic, id.asString(), Avro.default.toRecord(T::class.serializer(), value))

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
    listen<DiscordEvent>(producer) { event ->
        mapper(event).map {
            producerRecord(
                topic,
                it.first,
                it.second
            )
        }
    }
}

@ImplicitReflectionSerializer
inline fun <reified DiscordEvent : Event> EventDispatcher.listen(
    producer: KafkaProducer<String, GenericRecord>,
    noinline emitter: (event: DiscordEvent) -> Flux<ProducerRecord<String, GenericRecord>>
) {
    on(DiscordEvent::class.java)
        .flatMap { emitter(it) }
        .map {
            try {
                producer.send(it)
                logger.info("Emitted ${it.value().schema.name} to ${it.topic()}")
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

    client.eventDispatcher.listen<GuildCreateEvent>(producer) { event ->
        val guild = event.guild

        Flux.merge(
            ServerInfo.of(guild).map { producerRecord("servers", guild.id, it) },
            guild.channels
                .filter { it is GuildMessageChannel }
                .flatMap {
                    Mono.just(it.id).zipWith(ChannelInfo.of(it as GuildMessageChannel))
                }
                .map { producerRecord("channels", it.t1, it.t2) },
            guild.roles
                .flatMap {
                    Mono.just(it.id).zipWith(RoleInfo.of(it))
                }
                .map { producerRecord("roles", it.t1, it.t2) }
        )
    }

    client.eventDispatcher.map<GuildUpdateEvent, ServerInfo>(
        producer,
        "servers"
    ) {
        it.current.id to ServerInfo.of(it.current)
    }

    client.eventDispatcher.map<GuildDeleteEvent, ServerDeletion>(
        producer,
        "servers"
    ) {
        it.guildId to ServerDeletion.of(Instant.now())
    }

    client.eventDispatcher.map<RoleCreateEvent, RoleInfo>(
        producer,
        "roles"
    ) {
        it.role.id to RoleInfo.of(it.role)
    }

    client.eventDispatcher.map<RoleUpdateEvent, RoleInfo>(
        producer,
        "roles"
    ) {
        it.current.id to RoleInfo.of(it.current)
    }

    client.eventDispatcher.map<RoleDeleteEvent, RoleDeletion>(
        producer,
        "roles"
    ) {
        it.roleId to RoleDeletion.of(Instant.now())
    }

    client.eventDispatcher.map<TextChannelCreateEvent, ChannelInfo>(
        producer,
        "channels"
    ) {
        it.channel.id to ChannelInfo.of(it.channel)
    }

    client.eventDispatcher.map<TextChannelUpdateEvent, ChannelInfo>(
        producer,
        "channels"
    ) {
        it.current.id to ChannelInfo.of(it.current)
    }

    client.eventDispatcher.map<TextChannelDeleteEvent, ChannelDeletion>(
        producer,
        "channels"
    ) {
        it.channel.id to ChannelDeletion.of(Instant.now())
    }

    client.eventDispatcher.map<NewsChannelCreateEvent, ChannelInfo>(
        producer,
        "channels"
    ) {
        it.channel.id to ChannelInfo.of(it.channel)
    }

    client.eventDispatcher.map<NewsChannelUpdateEvent, ChannelInfo>(
        producer,
        "channels"
    ) {
        it.current.id to ChannelInfo.of(it.current)
    }

    client.eventDispatcher.map<NewsChannelDeleteEvent, ChannelDeletion>(
        producer,
        "channels"
    ) {
        it.channel.id to ChannelDeletion.of(Instant.now())
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

    client.eventDispatcher.map<MemberUpdateEvent, UserNicknameChange>(
        producer,
        "users"
    ) { event ->
        event.memberId to Mono.zip(
            event.client.getUserById(event.memberId),
            event.guild,
            Mono.justOrEmpty(event.old.filter { it.nickname != event.currentNickname }.map { event.currentNickname })
        ).flatMap {
            UserNicknameChange.of(it.t1, it.t2, it.t3.orElse(null))
        }
    }

    client.login().block()
}
