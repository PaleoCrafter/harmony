package com.seventeenthshard.harmony.bot

import com.seventeenthshard.harmony.events.ChannelDeletion
import com.seventeenthshard.harmony.events.ChannelInfo
import com.seventeenthshard.harmony.events.ChannelRemoval
import com.seventeenthshard.harmony.events.Embed
import com.seventeenthshard.harmony.events.MessageDeletion
import com.seventeenthshard.harmony.events.MessageEdit
import com.seventeenthshard.harmony.events.MessageEmbedUpdate
import com.seventeenthshard.harmony.events.NewMessage
import com.seventeenthshard.harmony.events.NewReaction
import com.seventeenthshard.harmony.events.ReactionClear
import com.seventeenthshard.harmony.events.ReactionRemoval
import com.seventeenthshard.harmony.events.RoleDeletion
import com.seventeenthshard.harmony.events.RoleInfo
import com.seventeenthshard.harmony.events.ServerDeletion
import com.seventeenthshard.harmony.events.ServerInfo
import com.seventeenthshard.harmony.events.UserInfo
import com.seventeenthshard.harmony.events.UserNicknameChange
import com.seventeenthshard.harmony.events.UserRolesChange
import com.seventeenthshard.harmony.events.toHex
import discord4j.core.`object`.PermissionOverwrite
import discord4j.core.`object`.entity.Channel
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.GuildMessageChannel
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.Role
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.`object`.util.Image
import discord4j.core.`object`.util.Snowflake
import reactor.core.publisher.Mono
import reactor.util.function.component1
import reactor.util.function.component2
import java.time.Instant
import java.util.Optional
import discord4j.core.`object`.Embed as DiscordEmbed

fun ServerInfo.Companion.of(guild: Guild) =
    Mono.just(
        ServerInfo(
            guild.id.asString(),
            guild.name,
            guild.getIconUrl(Image.Format.PNG).orElse(null)
        )
    )

fun ServerDeletion.Companion.of(timestamp: Instant) =
    Mono.just(ServerDeletion(timestamp))

fun RoleInfo.Companion.of(role: Role) =
    role.guild
        .flatMap {
            Mono.zip(ServerInfo.of(it), role.position)
        }
        .map {
            role.position
            RoleInfo(role.id.asString(), it.t1, role.name, role.color.toHex(), it.t2, role.permissions.rawValue)
        }

fun RoleDeletion.Companion.of(timestamp: Instant) =
    Mono.just(RoleDeletion(timestamp))

fun ChannelInfo.Companion.of(channel: GuildMessageChannel) =
    channel.guild
        .flatMap {
            Mono.zip(ServerInfo.of(it), channel.category)
        }
        .map {
            ChannelInfo(
                channel.id.asString(),
                it.t1,
                channel.name,
                it.t2.name,
                channel.rawPosition,
                if (channel.type == Channel.Type.GUILD_NEWS)
                    ChannelInfo.Type.NEWS
                else
                    ChannelInfo.Type.TEXT,
                channel.permissionOverwrites.map { override ->
                    ChannelInfo.PermissionOverride(
                        when (override.type) {
                            PermissionOverwrite.Type.UNKNOWN -> ChannelInfo.PermissionOverride.Type.UNKNOWN
                            PermissionOverwrite.Type.ROLE -> ChannelInfo.PermissionOverride.Type.ROLE
                            PermissionOverwrite.Type.MEMBER -> ChannelInfo.PermissionOverride.Type.USER
                            null -> ChannelInfo.PermissionOverride.Type.UNKNOWN
                        },
                        override.targetId.asString(),
                        override.allowed.rawValue,
                        override.denied.rawValue
                    )
                }
            )
        }

fun ChannelDeletion.Companion.of(timestamp: Instant) =
    Mono.just(ChannelDeletion(timestamp))

fun ChannelRemoval.Companion.of(timestamp: Instant) =
    Mono.just(ChannelRemoval(timestamp))

fun UserInfo.Companion.of(user: User) =
    Mono.just(
        UserInfo(
            user.id.asString(),
            user.username,
            user.discriminator,
            user.isBot
        )
    )

fun UserNicknameChange.Companion.of(user: User, guild: Guild, nickname: String?) =
    Mono.zip(UserInfo.of(user), ServerInfo.of(guild))
        .map {
            UserNicknameChange(
                it.t1,
                it.t2,
                nickname,
                Instant.now()
            )
        }

fun UserRolesChange.Companion.of(user: User, guild: Guild, roles: List<Snowflake>) =
    Mono.zip(UserInfo.of(user), ServerInfo.of(guild))
        .map {
            UserRolesChange(
                it.t1,
                it.t2,
                roles.map { role -> role.asString() },
                Instant.now()
            )
        }

fun NewMessage.Companion.of(message: Message) =
    Mono.zip(
        message.channel,
        Mono.justOrEmpty(message.author).flatMap { UserInfo.of(it) }
            .switchIfEmpty(message.webhook.map { UserInfo(it.id.asString(), it.name.orElse("Webhook"), "HOOK", true) })
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
                message.content.orElse(""),
                message.embeds.map(Embed.Companion::of),
                message.attachments.map {
                    NewMessage.Attachment(
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

fun MessageEdit.Companion.of(content: String, timestamp: Instant) =
    Mono.just(MessageEdit(content, timestamp))

fun MessageDeletion.Companion.of(timestamp: Instant) =
    Mono.just(MessageDeletion(timestamp))

fun MessageEmbedUpdate.Companion.of(embeds: Iterable<DiscordEmbed>) =
    Mono.just(MessageEmbedUpdate(embeds.map(Embed.Companion::of)))

fun Embed.Companion.of(embed: DiscordEmbed) =
    Embed(
        Embed.Type.valueOf(embed.type.name),
        embed.title.orElse(null),
        embed.description.orElse(null),
        embed.url.orElse(null),
        embed.color.orElse(null)?.toHex(),
        embed.timestamp.orElse(null),
        embed.footer.orElse(null)?.let {
            Embed.Footer(it.text, it.iconUrl, it.proxyIconUrl)
        },
        embed.image.orElse(null)?.let {
            Embed.Media(it.url, it.proxyUrl, it.width, it.height)
        },
        embed.thumbnail.orElse(null)?.let {
            Embed.Media(it.url, it.proxyUrl, it.width, it.height)
        },
        embed.video.orElse(null)?.let {
            Embed.Media(it.url, it.proxyUrl, it.width, it.height)
        },
        embed.provider.orElse(null)?.let {
            Embed.Provider(it.name, it.url)
        },
        embed.author.orElse(null)?.let {
            Embed.Author(it.name, it.url, it.iconUrl, it.proxyIconUrl)
        },
        embed.fields.map {
            Embed.Field(it.name, it.value, it.isInline)
        }
    )

fun NewReaction.Companion.of(user: User, emoji: ReactionEmoji) =
    UserInfo.of(user)
        .flatMap { u ->
            Mono.justOrEmpty(
                Optional.ofNullable(
                    emoji.asCustomEmoji().orElse(null)?.let {
                        NewReaction(u, NewReaction.Type.CUSTOM, it.name, it.id.asString(), it.isAnimated, Instant.now())
                    } ?: emoji.asUnicodeEmoji().orElse(null)?.let {
                        NewReaction(u, NewReaction.Type.UNICODE, it.raw, null, false, Instant.now())
                    }
                )
            )
        }

fun ReactionRemoval.Companion.of(user: User, emoji: ReactionEmoji) =
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

fun ReactionClear.Companion.of(timestamp: Instant) =
    Mono.just(ReactionClear(timestamp))
