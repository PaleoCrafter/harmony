@file:JvmName("HarmonyBot")

package com.seventeenthshard.harmony.bot

import com.seventeenthshard.harmony.bot.commands.AutoPublishCommand
import com.seventeenthshard.harmony.bot.commands.IgnoredChannelsCommand
import com.seventeenthshard.harmony.bot.handlers.db.buildDbHandler
import com.seventeenthshard.harmony.bot.handlers.elastic.buildElasticHandler
import discord4j.core.DiscordClientBuilder
import org.apache.logging.log4j.LogManager
import kotlin.system.exitProcess

val channelMentionRegex = Regex("^<#(\\d+)>$")

fun main(args: Array<String>) {
    val logger = LogManager.getLogger("bot")

    val client = DiscordClientBuilder.create(
        requireNotNull(System.getenv("BOT_TOKEN")) { "Bot token must be provided via BOT_TOKEN environment variable" }
    ).build()

    val gatewayClient = requireNotNull(client.login().block()) { "Received null Gateway client unexpectedly" }

    val ignoredChannels = IgnoredChannelsCommand(logger, gatewayClient)
    val autoPublish = AutoPublishCommand(logger, gatewayClient)

    when (val action = args.firstOrNull() ?: "import") {
        "import" -> {
            val emitter = EventEmitter(gatewayClient.eventDispatcher, listOf(buildDbHandler(), buildElasticHandler()))

            runBot(gatewayClient, emitter, ignoredChannels, autoPublish)
        }
        "dump" -> runDump(gatewayClient, ignoredChannels, args.drop(1))
        else -> {
            System.err.println("Unknown action '$action', available options are 'import' and 'dump'")
            exitProcess(1)
        }
    }
}
