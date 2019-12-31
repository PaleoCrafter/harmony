@file:JvmName("Bot")

package com.seventeenthshard.harmony.bot

import com.seventeenthshard.harmony.events.ChannelDeletion
import com.seventeenthshard.harmony.events.ChannelInfo
import com.seventeenthshard.harmony.events.ChannelRemoval
import com.seventeenthshard.harmony.events.MessageDeletion
import com.seventeenthshard.harmony.events.MessageEdit
import com.seventeenthshard.harmony.events.NewMessage
import com.seventeenthshard.harmony.events.RoleDeletion
import com.seventeenthshard.harmony.events.RoleInfo
import com.seventeenthshard.harmony.events.ServerDeletion
import com.seventeenthshard.harmony.events.ServerInfo
import com.seventeenthshard.harmony.events.UserInfo
import com.seventeenthshard.harmony.events.UserNicknameChange
import com.seventeenthshard.harmony.events.UserRolesChange
import com.sksamuel.avro4k.Avro
import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.entity.GuildMessageChannel
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.util.Permission
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.EventDispatcher
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.channel.CategoryUpdateEvent
import discord4j.core.event.domain.channel.NewsChannelCreateEvent
import discord4j.core.event.domain.channel.NewsChannelDeleteEvent
import discord4j.core.event.domain.channel.NewsChannelUpdateEvent
import discord4j.core.event.domain.channel.TextChannelCreateEvent
import discord4j.core.event.domain.channel.TextChannelDeleteEvent
import discord4j.core.event.domain.channel.TextChannelUpdateEvent
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.guild.GuildDeleteEvent
import discord4j.core.event.domain.guild.GuildUpdateEvent
import discord4j.core.event.domain.guild.MemberJoinEvent
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
import reactor.util.function.Tuple4
import reactor.util.function.component1
import reactor.util.function.component2
import reactor.util.function.component3
import reactor.util.function.component4
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
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
        .map { producer.emit(it) }
        .subscribe()
}

fun KafkaProducer<String, GenericRecord>.emit(record: ProducerRecord<String, GenericRecord>) {
    try {
        send(record)
        logger.info("Emitted ${record.value().schema.name} to ${record.topic()}")
    } catch (e: SerializationException) {
        e.printStackTrace()
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }
}

@ImplicitReflectionSerializer
fun main() {
    val client = DiscordClientBuilder(
        System.getenv("BOT_TOKEN")
            ?: throw IllegalArgumentException("Bot token must be provided via BOT_TOKEN environment variable")
    ).build()
    val ignoredChannels = ConcurrentHashMap.newKeySet<String>()
    try {
        ignoredChannels.addAll(Files.readAllLines(Paths.get("ignoredChannels.txt")).filter { it.isNotBlank() })
    } catch (exception: IOException) {
        logger.error("Could not read ignored channels, defaulting to empty")
    }

    fun saveIgnoredChannels() {
        Files.write(Paths.get("ignoredChannels.txt"), ignoredChannels)
    }

    val props = Properties()
    props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = System.getenv("BROKER_URLS")
        ?: throw IllegalArgumentException("BROKER_URLS env variable must be set!")
    props[ProducerConfig.ACKS_CONFIG] = "all"
    props[ProducerConfig.RETRIES_CONFIG] = 0
    props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
    props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java
    props[KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY] = RecordNameStrategy::class.java
    props[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = System.getenv("SCHEMA_REGISTRY_URL")
        ?: throw IllegalArgumentException("SCHEMA_REGISTRY_URL env variable must be set!")
    val producer = KafkaProducer<String, GenericRecord>(props)

    client.eventDispatcher.listen<GuildCreateEvent>(producer) { event ->
        val guild = event.guild

        Flux.merge(
            ServerInfo.of(guild).map { producerRecord("servers", guild.id, it) },
            guild.channels
                .filter { it is GuildMessageChannel && !ignoredChannels.contains(it.id.asString()) }
                .flatMap {
                    Mono.just(it.id).zipWith(ChannelInfo.of(it as GuildMessageChannel))
                }
                .map { producerRecord("channels", it.t1, it.t2) },
            guild.roles
                .flatMap {
                    Mono.just(it.id).zipWith(RoleInfo.of(it))
                }
                .map { producerRecord("roles", it.t1, it.t2) },
            guild.members
                .flatMap {
                    Mono.zip(event.client.getUserById(it.id), Mono.just(it.nickname), Mono.just(it.roleIds))
                }
                .flatMap {
                    Mono.zip(
                        Mono.just(it.t1.id),
                        UserInfo.of(it.t1),
                        UserNicknameChange.of(it.t1, guild, it.t2.orElse(null)),
                        UserRolesChange.of(it.t1, guild, it.t3.toList())
                    )
                }
                .flatMap {
                    Flux.just(
                        producerRecord("users", it.t1, it.t2),
                        producerRecord("users", it.t1, it.t3),
                        producerRecord("users", it.t1, it.t4)
                    )
                }
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
    ) { event ->
        event.channel.id to ChannelInfo.of(event.channel)
    }

    client.eventDispatcher.map<TextChannelUpdateEvent, ChannelInfo>(
        producer,
        "channels"
    ) { event ->
        event.current.id to ChannelInfo.of(event.current).filter { !ignoredChannels.contains(event.current.id.asString()) }
    }

    client.eventDispatcher.map<TextChannelDeleteEvent, ChannelDeletion>(
        producer,
        "channels"
    ) { event ->
        event.channel.id to ChannelDeletion.of(Instant.now()).filter { !ignoredChannels.contains(event.channel.id.asString()) }
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
    ) { event ->
        event.current.id to ChannelInfo.of(event.current).filter { !ignoredChannels.contains(event.current.id.asString()) }
    }

    client.eventDispatcher.map<NewsChannelDeleteEvent, ChannelDeletion>(
        producer,
        "channels"
    ) { event ->
        event.channel.id to ChannelDeletion.of(Instant.now()).filter { !ignoredChannels.contains(event.channel.id.asString()) }
    }

    client.eventDispatcher.flatMap<CategoryUpdateEvent, ChannelInfo>(
        producer,
        "channels"
    ) { event ->
        event.current.channels
            .filter { it is GuildMessageChannel }
            .flatMap { Mono.just(it.id).zipWith(ChannelInfo.of(it as GuildMessageChannel)) }
            .map { it.t1 to it.t2 }
    }

    client.eventDispatcher.map<MessageCreateEvent, NewMessage>(
        producer,
        "messages"
    ) { event ->
        event.message.id to NewMessage.of(event.message)
            .filter { event.message.type == Message.Type.DEFAULT && !ignoredChannels.contains(event.message.channelId.asString()) }
    }

    client.eventDispatcher.map<MessageUpdateEvent, MessageEdit>(
        producer,
        "messages"
    ) { event ->
        event.messageId to Mono.justOrEmpty(event.currentContent).zipWith(event.message)
            .filter { it.t2.type == Message.Type.DEFAULT && !ignoredChannels.contains(it.t2.channelId.asString()) }
            .flatMap { tuple ->
                MessageEdit.of(tuple.t1, tuple.t2.editedTimestamp.orElse(Instant.now()))
            }
    }

    client.eventDispatcher.map<MessageDeleteEvent, MessageDeletion>(
        producer,
        "messages"
    ) { event ->
        event.messageId to MessageDeletion.of(Instant.now())
            .filter { event.message.orElse(null)?.type == Message.Type.DEFAULT && !ignoredChannels.contains(event.channelId.asString()) }
    }

    client.eventDispatcher.flatMap<MessageBulkDeleteEvent, MessageDeletion>(
        producer,
        "messages"
    ) { event ->
        Flux.fromIterable(event.messages)
            .filter {
                it.type == Message.Type.DEFAULT && !ignoredChannels.contains(event.channelId.asString())
            }
            .flatMap { message ->
                MessageDeletion.of(Instant.now()).map { message.id to it }
            }
    }

    client.eventDispatcher.map<MemberJoinEvent, UserInfo>(
        producer,
        "users"
    ) { event ->
        event.member.id to event.client.getUserById(event.member.id)
            .flatMap {
                UserInfo.of(it)
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
    client.eventDispatcher.map<MemberUpdateEvent, UserRolesChange>(
        producer,
        "users"
    ) { event ->
        event.memberId to Mono.zip(
            event.client.getUserById(event.memberId),
            event.guild
        ).flatMap {
            UserRolesChange.of(it.t1, it.t2, event.currentRoles.toList())
        }
    }

    val channelMentionRegex = Regex("^<#(\\d+)>$")
    client.eventDispatcher.on(MessageCreateEvent::class.java)
        .flatMap { event ->
            Mono.zip(
                event.message.channel.flatMap { if (it is GuildMessageChannel) Mono.just(it) else Mono.empty() },
                Mono.justOrEmpty(event.guildId),
                Mono.just(event.message),
                event.member.map { it.basePermissions }.orElse(Mono.empty())
            )
        }
        .filter { it.t4.contains(Permission.MANAGE_CHANNELS) }
        .filterWhen { event -> event.t3.userMentions.any { u -> u.isBot && u.id == client.selfId.orElse(null) } }
        .map { (it.t1 to it.t2) to it.t3.content.orElse("").split(" ") }
        .filter { (_, words) -> "ignored-channels" in words }
        .flatMap { (params, words) ->
            val (channel, guildId) = params
            val mentionedChannels = words.mapNotNull { word -> channelMentionRegex.find(word)?.groupValues?.get(1) }
            val (action, channelIds) = when {
                "add" in words -> "add" to mentionedChannels
                "remove" in words -> "remove" to mentionedChannels
                "list" in words -> "list" to ignoredChannels.toList()
                else -> return@flatMap Mono.empty<Tuple4<GuildMessageChannel, String, Boolean, List<GuildMessageChannel>>>()
            }
            Mono.zip(
                Mono.just(channel),
                Mono.just(action),
                Mono.just("--clear" in words),
                Flux.fromIterable(channelIds)
                    .flatMap { client.getChannelById(Snowflake.of(it)) }
                    .filter { it is GuildMessageChannel && it.guildId == guildId }
                    .map { it as GuildMessageChannel }
                    .collectList()
            )
        }
        .flatMap { (channel, action, clear, channels) ->
            val channelList = channels.map { " - ${it.mention}" }.joinToString("\n")
            when (action) {
                "add" -> {
                    ignoredChannels.addAll(channels.map { it.id.asString() })
                    val sideEffect =
                        if (clear)
                            Flux.fromIterable(channels)
                                .flatMap { Mono.zip(Mono.just(it.id), ChannelRemoval.of(Instant.now())) }
                                .map { producer.emit(producerRecord("channels", it.t1, it.t2)) }
                                .collectList()
                        else
                            Mono.just(Unit)
                    sideEffect
                        .flatMap {
                            saveIgnoredChannels()
                            channel.createMessage("Successfully started ignoring the following channels:\n$channelList")
                        }
                        .onErrorResume {
                            channel.createMessage(
                                """
                                Failed to ignore the following channels:
                                $channelList
                                
                                Error Message:
                                ```
                                ${it.message}
                                ```
                                """.trimIndent()
                            )
                        }
                }
                "remove" -> {
                    ignoredChannels.removeAll(channels.map { it.id.asString() })
                    Flux.fromIterable(channels)
                        .flatMap { ChannelInfo.of(it) }
                        .map { producer.emit(producerRecord("channels", Snowflake.of(it.id), it)) }
                        .collectList()
                        .flatMap {
                            saveIgnoredChannels()
                            channel.createMessage("Successfully stopped ignoring the following channels:\n$channelList")
                        }
                        .onErrorResume {
                            channel.createMessage(
                                """
                                Failed to stop ignoring the following channels:
                                $channelList
                                
                                Error Message:
                                ```
                                ${it.message}
                                ```
                                """.trimIndent()
                            )
                        }
                }
                else -> {
                    channel.createMessage("Currently the following channels are ignored:\n$channelList")
                }
            }
        }
        .subscribe()

    client.login().block()
}
