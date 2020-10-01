package com.seventeenthshard.harmony.bot.commands

import com.seventeenthshard.harmony.bot.ChannelInfo
import com.seventeenthshard.harmony.bot.ChannelRemoval
import com.seventeenthshard.harmony.bot.EventEmitter
import com.seventeenthshard.harmony.bot.channelMentionRegex
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import org.apache.logging.log4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class IgnoredChannelsCommand(logger: Logger, private val client: GatewayDiscordClient) {
    private val channels = ConcurrentHashMap.newKeySet<String>()

    init {
        try {
            channels.addAll(Files.readAllLines(Paths.get("ignoredChannels.txt")).filter { it.isNotBlank() })
        } catch (exception: IOException) {
            logger.error("Could not read ignored channels, defaulting to empty")
        }
    }

    fun handle(
        guildId: Snowflake,
        channel: GuildMessageChannel,
        words: List<String>,
        emitter: EventEmitter
    ): Mono<Message> {
        val mentionedChannels = words.mapNotNull { word -> channelMentionRegex.find(word)?.groupValues?.get(1) }
        val (action, channelIds) = when {
            "add" in words -> "add" to mentionedChannels
            "remove" in words -> "remove" to mentionedChannels
            "list" in words -> "list" to channels.toList()
            else -> return Mono.empty()
        }
        val clear = "--clear" in words

        return Flux.fromIterable(channelIds)
            .flatMap { client.getChannelById(Snowflake.of(it)).onErrorResume { Mono.empty() } }
            .filter { it is GuildMessageChannel && it.guildId == guildId }
            .map { it as GuildMessageChannel }
            .collectList()
            .flatMap { channels ->
                val channelList = channels.joinToString("\n") { " - ${it.mention}" }
                when (action) {
                    "add" -> {
                        this.channels.addAll(channels.map { it.id.asString() })
                        val sideEffect =
                            if (clear)
                                Flux.fromIterable(channels)
                                    .flatMap { Mono.zip(Mono.just(it.id), ChannelRemoval.of(Instant.now())) }
                                    .map { emitter.emit(it.t1, it.t2) }
                                    .collectList()
                            else
                                Mono.just(Unit)
                        sideEffect
                            .flatMap {
                                save()
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
                        this.channels.removeAll(channels.map { it.id.asString() })
                        Flux.fromIterable(channels)
                            .flatMap { ChannelInfo.of(it) }
                            .map { emitter.emit(Snowflake.of(it.id), it) }
                            .collectList()
                            .flatMap {
                                save()
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
    }

    private fun save() {
        Files.write(Paths.get("ignoredChannels.txt"), channels)
    }

    operator fun contains(channelId: Snowflake) = channelId.asString() in channels
}
