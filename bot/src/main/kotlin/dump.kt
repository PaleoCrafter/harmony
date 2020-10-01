package com.seventeenthshard.harmony.bot

import com.seventeenthshard.harmony.bot.commands.IgnoredChannelsCommand
import com.seventeenthshard.harmony.bot.handlers.db.buildDbDumper
import com.seventeenthshard.harmony.bot.handlers.elastic.buildElasticDumper
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.event.domain.guild.GuildCreateEvent
import org.apache.logging.log4j.LogManager
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.component1
import reactor.util.function.component2
import java.time.*
import java.util.*
import kotlin.system.exitProcess

fun readOldMessages(startDate: LocalDate, endDate: Instant, channel: GuildMessageChannel): Flux<Message> =
    channel.getMessagesBefore(Snowflake.of(endDate))
        .filter { it.type == Message.Type.DEFAULT }
        .takeUntil { it.timestamp < startDate.atTime(0, 0).toInstant(ZoneOffset.UTC) }

fun runDump(
    client: GatewayDiscordClient,
    ignoredChannels: IgnoredChannelsCommand,
    arguments: List<String>
) {
    val logger = LogManager.getLogger("Dump")
    val startDate = arguments.getOrNull(0)?.let { LocalDate.parse(it) }
        ?: throw IllegalArgumentException("Dump start date must be provided via YYYY-MM-DD argument")
    val endDate = arguments.getOrNull(1)?.let { LocalDate.parse(it).atTime(0, 0).toInstant(ZoneOffset.UTC) }
        ?: Instant.now()

    val dbDumper = buildDbDumper()
    val elasticDumper = buildElasticDumper()

    logger.info("Starting dump from $startDate until ${endDate.atZone(ZoneId.systemDefault())}")

    client.on(GuildCreateEvent::class.java)
        .take(Duration.ofSeconds(30))
        .flatMap {
            it.guild.channels
        }
        .flatMap {
            Mono.zip(
                it.guild,
                Mono.justOrEmpty(Optional.ofNullable(it as? GuildMessageChannel))
            )
        }
        .filter { (_, channel) -> channel.id !in ignoredChannels }
        .filter { (_, channel) -> arguments.size <= 2 || channel.id.asString() in arguments.drop(2) }
        .flatMapSequential { (guild, channel) ->
            logger.info("Starting dump for #${channel.name} on '${guild.name}'")
            readOldMessages(startDate, endDate, channel)
                .flatMap { msg ->
                    Mono.zip(
                        Mono.just(msg),
                        UserInfo.from(msg),
                        Flux.fromIterable(msg.reactions)
                            .flatMap { reaction -> msg.getReactors(reaction.emoji).map { reaction.emoji to it.id } }
                            .collectList()
                    )
                }
                .onErrorContinue { e, t ->
                    logger.error("Failed to import message $e", t)
                }
                .window(1000)
                .flatMapSequential { group ->
                    group.collectList().map { messages ->
                        dbDumper(guild, channel, messages)
                        elasticDumper(guild, channel, messages)

                        logger.info("Successfully imported ${messages.size} messages into #${channel.name} on '${guild.name}' last was from ${messages.last().t1.timestamp}")
                    }
                }
        }
        .doOnComplete { client.logout().subscribe() }
        .subscribe()

    client.onDisconnect().block()

    logger.info("Finished dumping all messages!")

    exitProcess(0)
}
