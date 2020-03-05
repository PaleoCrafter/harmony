@file:JvmName("HarmonyBot")

package com.seventeenthshard.harmony.bot

import com.seventeenthshard.harmony.bot.handlers.elastic.buildElasticHandler
import discord4j.core.DiscordClientBuilder
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val logger = LogManager.getLogger("bot")

    val ignoredChannels = ConcurrentHashMap.newKeySet<String>()
    try {
        ignoredChannels.addAll(Files.readAllLines(Paths.get("ignoredChannels.txt")).filter { it.isNotBlank() })
    } catch (exception: IOException) {
        logger.error("Could not read ignored channels, defaulting to empty")
    }
    val client = DiscordClientBuilder(
        requireNotNull(System.getenv("BOT_TOKEN")) { "Bot token must be provided via BOT_TOKEN environment variable" }
    ).build()

    val action = args.firstOrNull() ?: "import"
    when (action) {
        "import" -> {
            val emitter = EventEmitter(client.eventDispatcher, listOf(buildElasticHandler()))

            runBot(client, emitter, ignoredChannels)
        }
        "dump" -> runDump(ignoredChannels, args.drop(1))
        else -> {
            System.err.println("Unknown action '$action', available options are 'import' and 'dump'")
            exitProcess(1)
        }
    }
}
