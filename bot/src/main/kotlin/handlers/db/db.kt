@file:JvmName("DBTools")

package com.seventeenthshard.harmony.bot.handlers.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun buildDbHandler() = connect().run { buildDbHandlerImpl() }

fun buildDbDumper() = connect().run { buildDbDumperImpl() }

private fun connect() {
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
}
