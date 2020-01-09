@file:JvmName("DBTools")

package com.seventeenthshard.harmony.dbimport

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val action = args.firstOrNull() ?: "import"

    Database.connect(
        requireNotNull(System.getenv("DB_CONNECTION")) { "DB_CONNECTION env variable must be set!" },
        requireNotNull(System.getenv("DB_DRIVER")) { "DB_DRIVER env variable must be set!" },
        requireNotNull(System.getenv("DB_USER")) { "DB_USER env variable must be set!" },
        requireNotNull(System.getenv("DB_PASSWORD")) { "DB_PASSWORD env variable must be set!" }
    )

    transaction {
        SchemaUtils.createMissingTablesAndColumns(
            Servers,
            Channels,
            Users,
            UserNicknames,
            Roles,
            UserRoles,
            PermissionOverrides,
            Messages,
            MessageVersions,
            MessageEmbeds,
            MessageEmbedFields,
            MessageAttachments,
            MessageReactions
        )
    }

    when (action) {
        "import" -> runImport()
        "dump" -> runDump(args.drop(1))
        else -> {
            System.err.println("Unknown action '$action', available options are 'import' and 'dump'")
            exitProcess(1)
        }
    }
}
