package com.seventeenthshard.harmony.bot


import discord4j.common.util.Snowflake
import discord4j.core.`object`.PermissionOverwrite
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.Role
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.Channel
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.rest.util.Color
import discord4j.rest.util.Image
import reactor.core.publisher.Mono
import reactor.util.function.component1
import reactor.util.function.component2
import java.time.Instant
import java.util.*
import discord4j.core.`object`.Embed as DiscordEmbed

fun Color.toHex(): String {
    val format = "%02x"

    return "${format.format(red)}${format.format(green)}${format.format(blue)}"
}

data class ServerInfo(val id: String, val name: String, val iconUrl: String?) {
    companion object {
        fun of(guild: Guild) =
            Mono.just(
                ServerInfo(
                    guild.id.asString(),
                    guild.name,
                    guild.getIconUrl(Image.Format.PNG).orElse(null)
                )
            )
    }
}

data class ServerDeletion(val timestamp: Instant) {
    companion object {
        fun of(timestamp: Instant) =
            Mono.just(ServerDeletion(timestamp))
    }
}

data class RoleInfo(
    val id: String,
    val server: ServerInfo,
    val name: String,
    val color: String,
    val position: Int,
    val permissions: Long
) {
    companion object {
        fun of(role: Role) =
            role.guild
                .flatMap {
                    Mono.zip(ServerInfo.of(it), role.position)
                }
                .map {
                    role.position
                    RoleInfo(role.id.asString(), it.t1, role.name, role.color.toHex(), it.t2, role.permissions.rawValue)
                }
    }
}

data class RoleDeletion(val timestamp: Instant) {
    companion object {
        fun of(timestamp: Instant) =
            Mono.just(RoleDeletion(timestamp))
    }
}

data class ChannelInfo(
    val id: String,
    val server: ServerInfo,
    val name: String,
    val category: String?,
    val categoryPosition: Int,
    val position: Int,
    val type: Type,
    val permissionOverrides: List<PermissionOverride>
) {
    companion object {
        fun of(channel: GuildMessageChannel) =
            channel.guild
                .flatMap {
                    Mono.zip(
                        ServerInfo.of(it),
                        channel.category.map { c -> Optional.of(c) }.defaultIfEmpty(Optional.empty())
                    )
                }
                .map {
                    val category = it.t2.orElse(null)

                    ChannelInfo(
                        channel.id.asString(),
                        it.t1,
                        channel.name,
                        category?.name,
                        category?.rawPosition ?: -1,
                        channel.rawPosition,
                        if (channel.type == Channel.Type.GUILD_NEWS)
                            Type.NEWS
                        else
                            Type.TEXT,
                        channel.permissionOverwrites.map { override ->
                            PermissionOverride(
                                when (override.type) {
                                    PermissionOverwrite.Type.UNKNOWN -> PermissionOverride.Type.UNKNOWN
                                    PermissionOverwrite.Type.ROLE -> PermissionOverride.Type.ROLE
                                    PermissionOverwrite.Type.MEMBER -> PermissionOverride.Type.USER
                                    null -> PermissionOverride.Type.UNKNOWN
                                },
                                override.targetId.asString(),
                                override.allowed.rawValue,
                                override.denied.rawValue
                            )
                        }
                    )
                }
    }

    enum class Type {
        TEXT, NEWS
    }

    data class PermissionOverride(val type: Type, val targetId: String, val allowed: Long, val denied: Long) {
        enum class Type {
            UNKNOWN, ROLE, USER
        }
    }
}

data class UserInfo(
    val id: String,
    val username: String,
    val discriminator: String,
    val isBot: Boolean,
    val webhookName: String? = null
) {
    companion object {
        fun of(user: User) =
            Mono.just(
                UserInfo(
                    user.id.asString(),
                    user.username,
                    user.discriminator,
                    user.isBot
                )
            )

        fun from(message: Message) =
            message.webhookId.map { id ->
                Mono.create<UserInfo> { sink ->
                    message.webhook
                        .flatMap { Mono.justOrEmpty(it.name) }
                        .onErrorResume { Mono.empty() }
                        .switchIfEmpty(Mono.just("Webhook #${id.asString()}"))
                        .map {
                            UserInfo(
                                id.asString(),
                                it,
                                "HOOK",
                                true,
                                message.userData.username()
                            )
                        }
                        .subscribe {
                            sink.success(it)
                        }
                }
            }.orElse(Mono.justOrEmpty(message.author).flatMap(::of))
    }
}

data class UserNicknameChange(
    val user: UserInfo,
    val server: ServerInfo,
    val nickname: String?,
    val timestamp: Instant
) {
    companion object {
        fun of(user: User, guild: Guild, nickname: String?) =
            Mono.zip(UserInfo.of(user), ServerInfo.of(guild))
                .map {
                    UserNicknameChange(
                        it.t1,
                        it.t2,
                        nickname,
                        Instant.now()
                    )
                }
    }
}

data class UserRolesChange(
    val user: UserInfo,
    val server: ServerInfo,
    val roles: List<String>,
    val timestamp: Instant
) {
    companion object {
        fun of(user: User, guild: Guild, roles: List<Snowflake>) =
            Mono.zip(UserInfo.of(user), ServerInfo.of(guild))
                .map {
                    UserRolesChange(
                        it.t1,
                        it.t2,
                        roles.map { role -> role.asString() },
                        Instant.now()
                    )
                }
    }
}
