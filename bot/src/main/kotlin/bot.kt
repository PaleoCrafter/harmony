@file:JvmName("Bot")

package com.seventeenthshard.harmony.bot

import com.seventeenthshard.harmony.bot.commands.AutoPublishCommand
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.`object`.presence.Activity
import discord4j.core.`object`.presence.Presence
import discord4j.core.event.domain.guild.*
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.role.RoleCreateEvent
import discord4j.core.event.domain.role.RoleDeleteEvent
import discord4j.core.event.domain.role.RoleUpdateEvent
import discord4j.rest.util.Permission
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import kotlin.concurrent.timer

fun runBot(
    client: GatewayDiscordClient,
    emitter: EventEmitter,
    autoPublish: AutoPublishCommand
) {
    val publicUrl = System.getenv("PUBLIC_URL")
    val presenceTimer = publicUrl?.let {
        timer("Update Discord presence", daemon = true, period = Duration.ofMinutes(1).toMillis()) {
            emitter.logger.info("Updating presence in Discord")
            client.updatePresence(Presence.online(Activity.watching(publicUrl))).block()
        }
    }

    emitter.listen<GuildCreateEvent, Any> { event ->
        val guild = event.guild

        Flux.merge(
            ServerInfo.of(guild).map { guild.id to it },
            guild.roles
                .flatMap {
                    Mono.just(it.id).zipWith(RoleInfo.of(it))
                }
                .map { it.t1 to it.t2 },
            guild.members
                .flatMap {
                    Mono.zip(event.client.getUserById(it.id), Mono.just(it.nickname), Mono.just(it.roleIds))
                }
                .flatMap {
                    Mono.zip(
                        Mono.just(it.t1.id),
                        UserInfo.of(it.t1),
                        UserNicknameChange.of(it.t1, guild, it.t2.orElse(null)),
                        UserRolesChange.of(it.t1, guild, it.t3.toList())
                    )
                }
                .flatMap {
                    Flux.just(
                        it.t1 to it.t2,
                        it.t1 to it.t3,
                        it.t1 to it.t4
                    )
                }
        )
    }

    emitter.map<GuildUpdateEvent, ServerInfo> {
        it.current.id to ServerInfo.of(it.current)
    }

    emitter.map<GuildDeleteEvent, ServerDeletion> {
        it.guildId to ServerDeletion.of(Instant.now())
    }

    emitter.map<RoleCreateEvent, RoleInfo> {
        it.role.id to RoleInfo.of(it.role)
    }

    emitter.map<RoleUpdateEvent, RoleInfo> {
        it.current.id to RoleInfo.of(it.current)
    }

    emitter.map<RoleDeleteEvent, RoleDeletion> {
        it.roleId to RoleDeletion.of(Instant.now())
    }

    emitter.map<MemberJoinEvent, UserInfo> { event ->
        event.member.id to event.client.getUserById(event.member.id)
            .flatMap {
                UserInfo.of(it)
            }
    }
    emitter.map<MemberUpdateEvent, UserNicknameChange> { event ->
        event.memberId to Mono.zip(
            event.client.getUserById(event.memberId),
            event.guild,
            Mono.justOrEmpty(event.old.filter { it.nickname != event.currentNickname }.map { event.currentNickname })
        ).flatMap {
            UserNicknameChange.of(it.t1, it.t2, it.t3.orElse(null))
        }
    }
    emitter.map<MemberUpdateEvent, UserRolesChange> { event ->
        event.memberId to Mono.zip(
            event.client.getUserById(event.memberId),
            event.guild
        ).flatMap {
            UserRolesChange.of(it.t1, it.t2, event.currentRoles.toList())
        }
    }

    client.eventDispatcher.on(MessageCreateEvent::class.java)
        .flatMap { event ->
            Mono.zip(
                Mono.justOrEmpty(event.guildId),
                event.message.channel.flatMap { if (it is GuildMessageChannel) Mono.just(it) else Mono.empty() },
                Mono.just(event.message),
                event.member.map { it.basePermissions }.orElse(Mono.empty())
            )
        }
        .filter { it.t4.contains(Permission.MANAGE_CHANNELS) }
        .filterWhen { event -> event.t3.userMentions.any { u -> u.isBot && u.id == client.selfId } }
        .map { Triple(it.t1, it.t2, it.t3.content.split(" ")) }
        .flatMap { (guildId, channel, words) ->
            when {
                "auto-publish" in words -> autoPublish.handle(guildId, channel, words)
                else -> channel.createMessage("Could not determine option")
            }
        }
        .onErrorContinue { t, e ->
            emitter.logger.error("Failed to handle command, event: $e", t)
        }
        .subscribe()

    autoPublish.listen()

    client.onDisconnect().map { presenceTimer?.cancel() }.block()

    emitter.logger.info("Disconnected from Gateway!")
}
