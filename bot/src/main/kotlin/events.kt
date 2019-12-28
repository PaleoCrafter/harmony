package com.seventeenthshard.harmony.bot

import com.seventeenthshard.harmony.events.ChannelDeletion
import com.seventeenthshard.harmony.events.ChannelInfo
import com.seventeenthshard.harmony.events.MessageDeletion
import com.seventeenthshard.harmony.events.MessageEdit
import com.seventeenthshard.harmony.events.NewMessage
import com.seventeenthshard.harmony.events.RoleDeletion
import com.seventeenthshard.harmony.events.RoleInfo
import com.seventeenthshard.harmony.events.ServerDeletion
import com.seventeenthshard.harmony.events.ServerInfo
import com.seventeenthshard.harmony.events.UserInfo
import com.seventeenthshard.harmony.events.UserNicknameChange
import discord4j.core.`object`.PermissionOverwrite
import discord4j.core.`object`.entity.Channel
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.GuildMessageChannel
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.Role
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.util.Image
import reactor.core.publisher.Mono
import java.time.Instant

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
            ServerInfo.of(it)
        }
        .map {
            RoleInfo(role.id.asString(), it, role.name, role.permissions.rawValue)
        }

fun RoleDeletion.Companion.of(timestamp: Instant) =
    Mono.just(RoleDeletion(timestamp))

fun ChannelInfo.Companion.of(channel: GuildMessageChannel) =
    channel.guild
        .flatMap {
            ServerInfo.of(it)
        }
        .map {
            ChannelInfo(
                channel.id.asString(),
                it,
                channel.name,
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

fun UserInfo.Companion.of(user: User) =
    Mono.just(
        UserInfo(
            user.id.asString(),
            user.username,
            user.discriminator
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

fun NewMessage.Companion.of(message: Message) =
    Mono.zip(message.channel, Mono.justOrEmpty(message.author))
        .filter { it.t1 is GuildMessageChannel }
        .flatMap {
            Mono.zip(
                ChannelInfo.of(it.t1 as GuildMessageChannel),
                UserInfo.of(it.t2)
            )
        }
        .map {
            NewMessage(
                it.t1,
                it.t2,
                message.content.orElse(""),
                message.timestamp
            )
        }

fun MessageEdit.Companion.of(content: String, timestamp: Instant) =
    Mono.just(MessageEdit(content, timestamp))

fun MessageDeletion.Companion.of(timestamp: Instant) =
    Mono.just(MessageDeletion(timestamp))
