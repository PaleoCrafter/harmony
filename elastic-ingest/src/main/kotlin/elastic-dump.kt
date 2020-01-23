@file:JvmName("ElasticDump")

package com.seventeenthshard.harmony.search

import com.seventeenthshard.harmony.events.UserInfo
import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.Embed
import discord4j.core.`object`.entity.Attachment
import discord4j.core.`object`.entity.GuildMessageChannel
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.domain.guild.GuildCreateEvent
import org.apache.logging.log4j.LogManager
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.component1
import reactor.util.function.component2
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

fun readOldMessages(lastDate: LocalDate, channel: GuildMessageChannel): Flux<Message> =
    channel.getMessagesBefore(Snowflake.of(Instant.now()))
        .filter { it.type == Message.Type.DEFAULT }
        .takeUntil { it.timestamp < lastDate.atTime(0, 0).toInstant(ZoneOffset.UTC) }

fun runDump(elasticClient: RestHighLevelClient, arguments: List<String>) {
    val logger = LogManager.getLogger("Dump")
    val startDate = arguments.firstOrNull()?.let { LocalDate.parse(it) }
        ?: throw IllegalArgumentException("Dump start date must be provided via YYYY-MM-DD argument")
    val client = DiscordClientBuilder(
        requireNotNull(System.getenv("BOT_TOKEN")) { "Bot token must be provided via BOT_TOKEN environment variable" }
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
        .flatMap {
            Mono.zip(
                it.guild,
                Mono.justOrEmpty(Optional.ofNullable(it as? GuildMessageChannel))
            )
        }
        .filter { (_, channel) -> channel.id.asString() !in ignoredChannels }
        .flatMap { (guild, channel) ->
            readOldMessages(startDate, channel)
                .flatMap { msg ->
                    Mono.zip(
                        Mono.just(msg),
                        Mono.justOrEmpty(msg.author).map { UserInfo(it.id.asString(), it.username, it.discriminator, it.isBot) }
                            .switchIfEmpty(msg.webhook.map { UserInfo(it.id.asString(), it.name.orElse("Webhook"), "HOOK", true) })
                    )
                }
                .window(1000)
                .flatMap { group ->
                    group.collectList().map { messages ->
                        messages.forEach { (message, author) ->
                            val (_, state) = DiscordMarkdownRules.parse(message.content.orElse(""))
                            val messageProperties = mutableSetOf<String>()

                            if (state.hasLinks) {
                                messageProperties += "link"
                            }

                            if (message.attachments.isNotEmpty()) {
                                messageProperties += "file"
                            }

                            if (message.embeds.isNotEmpty()) {
                                messageProperties += "embed"
                            }

                            messageProperties += message.attachments.map { it.type }
                            messageProperties += message.embeds.flatMap { it.mediaTypes }

                            elasticClient.index(
                                IndexRequest(INDEX).id(message.id.asString()).source(mapOf(
                                    "tie_breaker_id" to message.id.asString(),
                                    "server" to guild.id.asString(),
                                    "channel" to mapOf("id" to channel.id.asString(), "name" to channel.name),
                                    "author" to mapOf("id" to author.id, "name" to author.username, "discriminator" to author.discriminator),
                                    "content" to message.content.orElse(""),
                                    "has" to messageProperties,
                                    "mentions" to state.mentionedUsers,
                                    "attachments" to message.attachments.isNotEmpty(),
                                    "timestamp" to message.timestamp.toEpochMilli()
                                )),
                                RequestOptions.DEFAULT
                            )

                            Unit
                        }

                        logger.info("Successfully imported ${messages.size} messages into #${channel.name} on '${guild.name}'")
                    }
                }
        }
        .subscribe()

    client.login().block()
}

private val Attachment.type: String
    get() = when (this.filename.substring(this.filename.lastIndexOf('.') + 1).toLowerCase()) {
        "jpg" -> "image"
        "jpeg" -> "image"
        "webp" -> "image"
        "gif" -> "image"
        "png" -> "image"
        "mp4" -> "video"
        "webm" -> "video"
        "mp3" -> "sound"
        "wav" -> "sound"
        "ogg" -> "sound"
        else -> "file"
    }

private val Embed.mediaTypes: Iterable<String>
    get() {
        val types = mutableSetOf<String>()

        if (type === Embed.Type.IMAGE || image.isPresent) {
            types += "image"
        }

        if (type === Embed.Type.VIDEO || video.isPresent) {
            types += "video"
        }

        return types
    }
