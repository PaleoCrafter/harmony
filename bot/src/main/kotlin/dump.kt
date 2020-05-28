package com.seventeenthshard.harmony.bot

import com.seventeenthshard.harmony.bot.handlers.db.buildDbDumper
import com.seventeenthshard.harmony.bot.handlers.elastic.buildElasticDumper
import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.entity.GuildMessageChannel
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.domain.guild.GuildCreateEvent
import org.apache.logging.log4j.LogManager
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.component1
import reactor.util.function.component2
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun readOldMessages(lastDate: LocalDate, channel: GuildMessageChannel): Flux<Message> =
    channel.getMessagesBefore(Snowflake.of(Instant.now()))
        .filter { it.type == Message.Type.DEFAULT }
        .takeUntil { it.timestamp < lastDate.atTime(0, 0).toInstant(ZoneOffset.UTC) }

fun runDump(ignoredChannels: ConcurrentHashMap.KeySetView<String, Boolean>, arguments: List<String>) {
    val logger = LogManager.getLogger("Dump")
    val startDate = arguments.firstOrNull()?.let { LocalDate.parse(it) }
        ?: throw IllegalArgumentException("Dump start date must be provided via YYYY-MM-DD argument")
    val client = DiscordClientBuilder.create(
        requireNotNull(System.getenv("BOT_TOKEN")) { "Bot token must be provided via BOT_TOKEN environment variable" }
    ).build()

    val dbDumper = buildDbDumper()
    val elasticDumper = buildElasticDumper()

    logger.info("Starting dump up until $startDate")

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
                            .switchIfEmpty(msg.webhook.map { UserInfo(it.id.asString(), it.name.orElse("Webhook"), "HOOK", true) }),
                        Flux.fromIterable(msg.reactions)
                            .flatMap { reaction -> msg.getReactors(reaction.emoji).map { reaction.emoji to it.id } }
                            .collectList()
                    )
                }
                .onErrorContinue { e, t ->
                    logger.error("Failed to import message $e", t)
                }
                .window(1000)
                .flatMap { group ->
                    group.collectList().map { messages ->
                        dbDumper(guild, channel, messages)
                        elasticDumper(guild, channel, messages)

                        logger.info("Successfully imported ${messages.size} messages into #${channel.name} on '${guild.name}'")
                    }
                }
        }
        .subscribe()

    client.login().block()
}
