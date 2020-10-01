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

data class ChannelDeletion(val timestamp: Instant) {
    companion object {
        fun of(timestamp: Instant) =
            Mono.just(ChannelDeletion(timestamp))
    }
}

data class ChannelRemoval(val timestamp: Instant) {
    companion object {
        fun of(timestamp: Instant) =
            Mono.just(ChannelRemoval(timestamp))
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

data class NewMessage(
    val channel: ChannelInfo,
    val user: UserInfo,
    val content: String,
    val embeds: List<Embed>,
    val attachments: List<Attachment>,
    val timestamp: Instant
) {
    companion object {
        fun of(message: Message) =
            Mono.zip(
                message.channel,
                UserInfo.from(message)
            ).filter { it.t1 is GuildMessageChannel }
                .flatMap {
                    Mono.zip(
                        ChannelInfo.of(it.t1 as GuildMessageChannel),
                        Mono.just(it.t2)
                    )
                }
                .map { (channel, user) ->
                    NewMessage(
                        channel,
                        user,
                        message.content,
                        message.embeds.map(Embed.Companion::of),
                        message.attachments.map {
                            Attachment(
                                it.filename,
                                it.url,
                                it.proxyUrl,
                                if (it.width.isPresent) it.width.asInt else null,
                                if (it.height.isPresent) it.height.asInt else null,
                                it.isSpoiler
                            )
                        },
                        message.timestamp
                    )
                }
    }

    data class Attachment(
        val name: String,
        val url: String,
        val proxyUrl: String,
        val width: Int?,
        val height: Int?,
        val spoiler: Boolean
    )
}

data class MessageEdit(
    val content: String,
    val timestamp: Instant
) {
    companion object {
        fun of(content: String, timestamp: Instant) =
            Mono.just(MessageEdit(content, timestamp))
    }
}

data class MessageDeletion(
    val timestamp: Instant
) {
    companion object {
        fun of(timestamp: Instant) =
            Mono.just(MessageDeletion(timestamp))
    }
}

data class MessageEmbedUpdate(val embeds: List<Embed>) {
    companion object {
        fun of(embeds: Iterable<DiscordEmbed>) =
            Mono.just(MessageEmbedUpdate(embeds.map(Embed.Companion::of)))
    }
}

data class Embed(
    val type: Type,
    val title: String?,
    val description: String?,
    val url: String?,
    val color: String?,
    val timestamp: Instant?,
    val footer: Footer?,
    val image: Media?,
    val thumbnail: Media?,
    val video: Media?,
    val provider: Provider?,
    val author: Author?,
    val fields: List<Field>
) {
    companion object {
        fun of(embed: DiscordEmbed) =
            Embed(
                Type.valueOf(embed.type.name),
                embed.title.orElse(null),
                embed.description.orElse(null),
                embed.url.orElse(null),
                embed.color.orElse(null)?.toHex(),
                embed.timestamp.orElse(null),
                embed.footer.orElse(null)?.let {
                    Footer(it.text, it.iconUrl, it.proxyIconUrl)
                },
                embed.image.orElse(null)?.let {
                    Media(it.url, it.proxyUrl, it.width, it.height)
                },
                embed.thumbnail.orElse(null)?.let {
                    Media(it.url, it.proxyUrl, it.width, it.height)
                },
                embed.video.orElse(null)?.let {
                    Media(it.url, it.url, it.width, it.height)
                },
                embed.provider.orElse(null)?.let {
                    Provider(it.name, it.url.orElse(null))
                },
                embed.author.orElse(null)?.let {
                    Author(
                        it.name,
                        it.url,
                        it.iconUrl,
                        it.proxyIconUrl
                    )
                },
                embed.fields.map {
                    Field(it.name, it.value, it.isInline)
                }
            )
    }

    enum class Type {
        UNKNOWN, IMAGE, LINK, RICH, VIDEO
    }

    data class Footer(val text: String, val iconUrl: String?, val proxyIconUrl: String?)

    data class Media(val url: String?, val proxyUrl: String?, val width: Int?, val height: Int?)

    data class Provider(val name: String?, val url: String?)

    data class Author(val name: String?, val url: String?, val iconUrl: String?, val proxyIconUrl: String?)

    data class Field(val name: String, val value: String, val inline: Boolean)
}

data class NewReaction(
    val user: UserInfo,
    val type: Type,
    val emoji: String,
    val emojiId: String?,
    val emojiAnimated: Boolean,
    val timestamp: Instant
) {
    companion object {
        fun of(user: User, emoji: ReactionEmoji) =
            UserInfo.of(user)
                .flatMap { u ->
                    Mono.justOrEmpty(
                        Optional.ofNullable(
                            emoji.asCustomEmoji().orElse(null)?.let {
                                NewReaction(u, Type.CUSTOM, it.name, it.id.asString(), it.isAnimated, Instant.now())
                            } ?: emoji.asUnicodeEmoji().orElse(null)?.let {
                                NewReaction(u, Type.UNICODE, it.raw, null, false, Instant.now())
                            }
                        )
                    )
                }
    }

    enum class Type {
        UNICODE, CUSTOM
    }
}

data class ReactionRemoval(
    val user: UserInfo,
    val type: NewReaction.Type,
    val emoji: String,
    val emojiId: String?,
    val timestamp: Instant
) {
    companion object {
        fun of(user: User, emoji: ReactionEmoji) =
            UserInfo.of(user)
                .flatMap { u ->
                    Mono.justOrEmpty(
                        Optional.ofNullable(
                            emoji.asCustomEmoji().orElse(null)?.let {
                                ReactionRemoval(u, NewReaction.Type.CUSTOM, it.name, it.id.asString(), Instant.now())
                            } ?: emoji.asUnicodeEmoji().orElse(null)?.let {
                                ReactionRemoval(u, NewReaction.Type.UNICODE, it.raw, null, Instant.now())
                            }
                        )
                    )
                }
    }
}

data class ReactionClear(val timestamp: Instant) {
    companion object {
        fun of(timestamp: Instant) =
            Mono.just(ReactionClear(timestamp))
    }
}
