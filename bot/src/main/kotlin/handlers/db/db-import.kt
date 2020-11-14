@file:JvmName("DBImport")

package com.seventeenthshard.harmony.bot.handlers.db

import com.seventeenthshard.harmony.bot.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.ZoneId

fun buildDbHandlerImpl() =
    EventHandler("db-ingest") {
        listen<ServerDeletion> { serverId, _ ->
            transaction {
                Servers.update({ Servers.id eq serverId }) {
                    it[active] = false
                }
            }
        }

        listen<RoleInfo> { roleId, event ->
            transaction {
                Roles.replace {
                    it[id] = roleId
                    it[server] = event.server.id
                    it[name] = event.name
                    it[color] = event.color
                    it[position] = event.position
                    it[permissions] = event.permissions
                }
            }
        }
        listen<RoleDeletion> { roleId, event ->
            transaction {
                Roles.update({ Roles.id eq roleId }) {
                    it[deletedAt] = LocalDateTime.ofInstant(event.timestamp, ZoneId.of("UTC"))
                }
            }
        }

        listen<UserInfo> { _, event ->
            transaction {
                Users.replace {
                    it[id] = event.id
                    it[name] = event.username
                    it[discriminator] = event.discriminator
                    it[bot] = event.isBot
                }
            }
        }
        listen<UserNicknameChange> { userId, event ->
            transaction {
                val currentNickname = UserNicknames
                    .select {
                        (UserNicknames.server eq event.server.id) and (UserNicknames.user eq userId)
                    }
                    .orderBy(UserNicknames.timestamp to SortOrder.DESC)
                    .limit(1)
                    .firstOrNull()
                    ?.get(UserNicknames.nickname)

                if (currentNickname != event.nickname) {
                    UserNicknames.replace {
                        it[server] = event.server.id
                        it[user] = userId
                        it[timestamp] = LocalDateTime.ofInstant(event.timestamp, ZoneId.of("UTC"))
                        it[nickname] = event.nickname
                    }
                }
            }
        }
        listen<UserRolesChange> { userId, event ->
            transaction {
                UserRoles.deleteWhere { (UserRoles.server eq event.server.id) and (UserRoles.user eq userId) }

                UserRoles.batchInsert(event.roles) {
                    this[UserRoles.server] = event.server.id
                    this[UserRoles.user] = userId
                    this[UserRoles.role] = it
                }
            }
        }
    }
