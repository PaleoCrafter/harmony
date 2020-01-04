@file:JvmName("DBDump")

package com.seventeenthshard.harmony.dbimport

import com.seventeenthshard.harmony.events.Embed
import com.seventeenthshard.harmony.events.toHex
import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.entity.GuildMessageChannel
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.domain.guild.GuildCreateEvent
import org.apache.logging.log4j.LogManager
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
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
                val messageIds = messages.map { msg -> msg.id.asString() }
                val existing = Messages.select { Messages.id inList messageIds }
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

                    MessageAttachments.batchInsert(msg.attachments) {
                        this[MessageAttachments.message] = msg.id.asString()
                        this[MessageAttachments.name] = it.filename
                        this[MessageAttachments.url] = it.url
                        this[MessageAttachments.proxyUrl] = it.proxyUrl
                        this[MessageAttachments.width] = if (it.width.isPresent) it.width.asInt else null
                        this[MessageAttachments.height] = if (it.height.isPresent) it.height.asInt else null
                        this[MessageAttachments.spoiler] = it.isSpoiler
                    }
                }

                MessageEmbeds.deleteWhere { MessageEmbeds.message inList messageIds }

                messages.flatMap { msg -> msg.embeds.map { msg.id.asString() to it } }.forEach { (msg, embed) ->
                    val embedId = MessageEmbeds.insertAndGetId {
                        it[message] = msg
                        it[type] = Embed.Type.valueOf(embed.type.name)
                        it[title] = embed.title.orElse(null)
                        it[description] = embed.description.orElse(null)
                        it[url] = embed.url.orElse(null)
                        it[color] = embed.color.orElse(null)?.toHex()
                        it[timestamp] = embed.timestamp.orElse(null)?.let { ts -> LocalDateTime.ofInstant(ts, ZoneId.of("UTC")) }

                        it[footerText] = embed.footer.orElse(null)?.text
                        it[footerIconUrl] = embed.footer.orElse(null)?.iconUrl
                        it[footerIconProxyUrl] = embed.footer.orElse(null)?.proxyIconUrl

                        it[imageUrl] = embed.image.orElse(null)?.url
                        it[imageProxyUrl] = embed.image.orElse(null)?.proxyUrl
                        it[imageWidth] = embed.image.orElse(null)?.width
                        it[imageHeight] = embed.image.orElse(null)?.height

                        it[thumbnailUrl] = embed.thumbnail.orElse(null)?.url
                        it[thumbnailProxyUrl] = embed.thumbnail.orElse(null)?.proxyUrl
                        it[thumbnailWidth] = embed.thumbnail.orElse(null)?.width
                        it[thumbnailHeight] = embed.thumbnail.orElse(null)?.height

                        it[videoUrl] = embed.video.orElse(null)?.url
                        it[videoProxyUrl] = embed.video.orElse(null)?.proxyUrl
                        it[videoWidth] = embed.video.orElse(null)?.width
                        it[videoHeight] = embed.video.orElse(null)?.height

                        it[providerName] = embed.provider.orElse(null)?.name
                        it[providerUrl] = embed.provider.orElse(null)?.url

                        it[authorName] = embed.author.orElse(null)?.name
                        it[authorUrl] = embed.author.orElse(null)?.url
                        it[authorIconUrl] = embed.author.orElse(null)?.iconUrl
                        it[authorIconProxyUrl] = embed.author.orElse(null)?.proxyIconUrl
                    }

                    MessageEmbedFields.batchInsert(embed.fields.withIndex()) { (index, field) ->
                        this[MessageEmbedFields.embed] = embedId
                        this[MessageEmbedFields.position] = index
                        this[MessageEmbedFields.name] = field.name
                        this[MessageEmbedFields.value] = field.value
                        this[MessageEmbedFields.inline] = field.isInline
                    }
                }
            }

            logger.info("Successfully imported ${messages.size} messages into #${channel.name} on '${guild.name}'")
        }
        .subscribe()

    client.login().block()
}
