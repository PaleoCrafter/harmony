package com.seventeenthshard.harmony.bot.commands

import com.seventeenthshard.harmony.bot.channelMentionRegex
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.Channel
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import org.apache.logging.log4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.component1
import reactor.util.function.component2
import reactor.util.function.component3
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

class AutoPublishCommand(private val logger: Logger, private val client: GatewayDiscordClient) {
    private val channels = ConcurrentHashMap.newKeySet<String>()

    init {
        try {
            channels.addAll(Files.readAllLines(Paths.get("autoPublishChannels.txt")).filter { it.isNotBlank() })
        } catch (exception: IOException) {
            logger.error("Could not read auto-published channels, defaulting to empty")
        }
    }

    fun listen() {
        client.eventDispatcher.on(MessageCreateEvent::class.java)
            .filter { it.message.channelId.asString() in channels }
            .flatMap { event ->
                Mono.zip(
                    event.guild,
                    event.message.channel.flatMap { if (it is GuildMessageChannel) Mono.just(it) else Mono.empty() },
                    Mono.just(event.message)
                )
            }
            .flatMap { (server, channel, message) ->
                message.publish()
                    .map {
                        logger.info("Auto-published a message from '${message.userData.username()}' in #${channel.name} on '${server.name}'")
                    }
            }
            .onErrorContinue { throwable, obj ->
                logger.error("Failed to auto-publish message, event: $obj", throwable)
            }
            .subscribe()
    }

    fun handle(
        guildId: Snowflake,
        channel: GuildMessageChannel,
        words: List<String>
    ): Mono<*> {
        val mentionedChannels = words.mapNotNull { word -> channelMentionRegex.find(word)?.groupValues?.get(1) }
        val (action, channelIds) = when {
            "add" in words -> "add" to mentionedChannels
            "remove" in words -> "remove" to mentionedChannels
            "list" in words -> "list" to channels.toList()
            else -> return Mono.empty<Any>()
        }

        return Flux.fromIterable(channelIds)
            .flatMap { client.getChannelById(Snowflake.of(it)).onErrorResume { Mono.empty() } }
            .filter { it is GuildMessageChannel && it.guildId == guildId }
            .map { it as GuildMessageChannel }
            .collectList()
            .flatMap { channels ->
                when (action) {
                    "add" -> {
                        val (newsChannels, nonNewsChannels) = channels.partition { it.type == Channel.Type.GUILD_NEWS }
                        val newsChannelList = newsChannels.joinToString("\n") { " - ${it.mention}" }
                        val nonNewsChannelList = nonNewsChannels.joinToString("\n") { " - ${it.mention}" }

                        this.channels.addAll(newsChannels.map { it.id.asString() })
                        save()

                        var response = "Successfully enabled auto-publish for the following channels:\n$newsChannelList"

                        if (nonNewsChannels.isNotEmpty()) {
                            response += "\n\nThe following channels were not added as they aren't announcement channels:\n$nonNewsChannelList"
                        }

                        channel.createMessage(response)
                    }
                    "remove" -> {
                        val channelList = channels.joinToString("\n") { " - ${it.mention}" }

                        this.channels.removeAll(channels.map { it.id.asString() })
                        save()
                        channel.createMessage("Successfully disabled auto-publish for the following channels:\n$channelList")
                    }
                    else -> {
                        val channelList = channels.joinToString("\n") {
                            " - ${it.mention}${if (it.type !== Channel.Type.GUILD_NEWS) " (invalid)" else ""}"
                        }

                        channel.createMessage("Currently the following channels are auto-published:\n$channelList")
                    }
                }
            }
    }

    private fun save() {
        Files.write(Paths.get("autoPublishChannels.txt"), channels)
    }
}
