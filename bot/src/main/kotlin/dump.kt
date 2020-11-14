package com.seventeenthshard.harmony.bot

import com.seventeenthshard.harmony.bot.handlers.db.buildDbDumper
import com.seventeenthshard.harmony.bot.handlers.elastic.buildElasticDumper
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.event.domain.guild.GuildCreateEvent
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.apache.logging.log4j.LogManager
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.component1
import reactor.util.function.component2
import java.time.*
import java.util.*
import kotlin.system.exitProcess

fun readOldMessages(startDate: Instant?, endDate: Instant, channel: GuildMessageChannel): Flux<Message> =
    channel.getMessagesBefore(Snowflake.of(endDate))
        .filter { it.type == Message.Type.DEFAULT }
        .takeUntil { startDate !== null && it.timestamp < startDate }

fun runDump(client: GatewayDiscordClient, arguments: List<String>) {
    val options = Options()
    options.addOption("s", "start", true, "Start date for dump")
    options.addOption("e", "end", true, "End date for dump")

    val cli = DefaultParser().parse(options, arguments.toTypedArray())

    val channels = cli.argList
    if (channels.isEmpty()) {
        throw IllegalArgumentException("You must provide at least one channel ID as positional argument")
    }

    val logger = LogManager.getLogger("Dump")
    val startDate = cli.getOptionValue('e', null)
        ?.let { LocalDate.parse(it).atTime(0, 0).toInstant(ZoneOffset.UTC) }
    val endDate = cli.getOptionValue('e', null)
        ?.let { LocalDate.parse(it).atTime(0, 0).toInstant(ZoneOffset.UTC) }
        ?: Instant.now()

    val dbDumper = buildDbDumper()
    val elasticDumper = buildElasticDumper()

    if (startDate !== null) {
        logger.info("Starting dump from $startDate until ${endDate.atZone(ZoneId.systemDefault())}")
    } else {
        logger.info("Starting dump until ${endDate.atZone(ZoneId.systemDefault())}")
    }

    client.on(GuildCreateEvent::class.java)
        .take(Duration.ofSeconds(30))
        .flatMap {
            it.guild.channels
        }
        .flatMap { Mono.justOrEmpty(Optional.ofNullable(it as? GuildMessageChannel)) }
        .flatMap { Mono.zip(ChannelInfo.of(it), Mono.just(it)) }
        .filter { (channelInfo, _) -> channelInfo.id in channels }
        .flatMap { (channelInfo, channel)  ->
            logger.info("Starting dump for #${channel.name} on '${channelInfo.server.name}'")
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
                .onErrorContinue { t, o ->
                    logger.error("Failed to import message $o", t)
                }
                .window(1000)
                .flatMapSequential { group ->
                    group.collectList().map { messages ->
                        dbDumper(channelInfo, messages)
                        elasticDumper(channelInfo, messages)

                        logger.info("Successfully imported ${messages.size} messages into #${channel.name} on '${channelInfo.server.name}' last was from ${messages.last().t1.timestamp}")
                    }
                }
        }
        .doOnComplete { client.logout().subscribe() }
        .subscribe()

    client.onDisconnect().block()

    logger.info("Finished dumping all messages!")

    exitProcess(0)
}
