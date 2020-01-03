@file:JvmName("DBDump")

package com.seventeenthshard.harmony.dbimport

import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.entity.GuildMessageChannel
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.domain.guild.GuildCreateEvent
import org.apache.logging.log4j.LogManager
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.replace
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.util.function.component1
import reactor.util.function.component2
import reactor.util.function.component3
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

fun readOldMessages(lastDate: LocalDate, channel: GuildMessageChannel): Flux<Message> {
    return generateSequence(LocalDate.now()) {
        it.minusDays(1).takeIf { d -> d >= lastDate }
    }.toList().fold(
        Mono.justOrEmpty(channel.lastMessageId).zipWith(
            Mono.just(channel.lastMessage.toFlux().onErrorResume {
                channel.getMessagesBefore(Snowflake.of(Instant.now())).next()
            })
        )
    ) { acc, date ->
        acc.flatMap { state ->
            val ref = date.atTime(0, 0).toInstant(ZoneOffset.UTC)
            val (lastId, messages) = state
            val newMessages = channel.getMessagesBefore(lastId).takeWhile { it.timestamp >= ref }.collectList()

            newMessages.flatMap {
                Mono.just(it.lastOrNull()?.id ?: lastId).zipWith(Mono.just(messages.mergeWith(Flux.fromIterable(it))))
            }
        }
    }.toFlux()
        .flatMap { it.t2 }
        .distinct { it.id }
        .filter { it.type == Message.Type.DEFAULT }
}

fun runDump(arguments: List<String>) {
    val logger = LogManager.getLogger("Dump")
    val startDate = arguments.firstOrNull()?.let { LocalDate.parse(it) }
        ?: throw IllegalArgumentException("Dump start date must be provided via YYYY-MM-DD argument")
    val client = DiscordClientBuilder(
        System.getenv("BOT_TOKEN")
            ?: throw IllegalArgumentException("Bot token must be provided via BOT_TOKEN environment variable")
    ).build()

    logger.info("Starting dump up until $startDate")

    val ignoredChannels = ConcurrentHashMap.newKeySet<String>()
    try {
        ignoredChannels.addAll(Files.readAllLines(Paths.get("ignoredChannels.txt")).filter { it.isNotBlank() })
    } catch (exception: IOException) {
        logger.error("Could not read ignored channels, defaulting to empty")
    }

    client.eventDispatcher.on(GuildCreateEvent::class.java)
        .flatMap {
            it.guild.channels
        }
        .flatMap { Mono.justOrEmpty(Optional.ofNullable(it as? GuildMessageChannel)) }
        .filter { it.id.asString() !in ignoredChannels }
        .flatMap { channel ->
            Mono.zip(readOldMessages(startDate, channel).collectList(), Mono.just(channel), channel.guild)
        }
        .map { (messages, channel, guild) ->
            transaction {
                val existing = Messages.select { Messages.id inList messages.map { msg -> msg.id.asString() } }
                    .map { it[Messages.id] }

                Users.batchInsert(messages.mapNotNull { it.author.orElse(null) }, ignore = true) {
                    this[Users.id] = it.id.asString()
                    this[Users.name] = it.username
                    this[Users.discriminator] = it.discriminator
                }

                Messages.batchInsert(messages.filter { it.author.isPresent }, ignore = true) {
                    val creationTimestamp = LocalDateTime.ofInstant(it.timestamp, ZoneId.of("UTC"))

                    this[Messages.id] = it.id.asString()
                    this[Messages.server] = guild.id.asString()
                    this[Messages.channel] = it.channelId.asString()
                    this[Messages.user] = it.author.map { u -> u.id.asString() }.orElse(null) ?: return@batchInsert
                    this[Messages.createdAt] = creationTimestamp
                }

                val (existingMessages, newMessages) = messages.partition { it.id.asString() in existing }
                existingMessages.forEach { msg ->
                    val lastVersion = MessageVersions.select { MessageVersions.message eq msg.id.asString() }
                        .orderBy(MessageVersions.timestamp, SortOrder.DESC).limit(1).firstOrNull()

                    val requiresEdit = if (lastVersion === null) {
                        val creationTimestamp = LocalDateTime.ofInstant(msg.timestamp, ZoneId.of("UTC"))
                        MessageVersions.insert {
                            it[message] = msg.id.asString()
                            it[content] = msg.content.orElse("")
                            it[timestamp] = creationTimestamp
                        }

                        msg.editedTimestamp.isPresent
                    } else lastVersion[MessageVersions.content] != msg.content.orElse("")

                    if (requiresEdit && msg.editedTimestamp.isPresent) {
                        val editTimestamp = LocalDateTime.ofInstant(msg.editedTimestamp.get(), ZoneId.of("UTC"))
                        MessageVersions.insert {
                            it[message] = msg.id.asString()
                            it[content] = msg.content.orElse("")
                            it[timestamp] = editTimestamp
                        }
                    }
                }

                newMessages.forEach { msg ->
                    val creationTimestamp = LocalDateTime.ofInstant(msg.timestamp, ZoneId.of("UTC"))
                    MessageVersions.replace {
                        it[message] = msg.id.asString()
                        it[content] = msg.content.orElse("")
                        it[timestamp] = creationTimestamp
                    }
                    val editTimestamp = msg.editedTimestamp.orElse(null)?.let {
                        LocalDateTime.ofInstant(it, ZoneId.of("UTC"))
                    }

                    if (editTimestamp != null) {
                        MessageVersions.replace {
                            it[message] = msg.id.asString()
                            it[content] = msg.content.orElse("")
                            it[timestamp] = editTimestamp
                        }
                    }
                }
            }

            logger.info("Successfully imported ${messages.size} messages into #${channel.name} on '${guild.name}'")
        }
        .subscribe()

    client.login().block()
}
