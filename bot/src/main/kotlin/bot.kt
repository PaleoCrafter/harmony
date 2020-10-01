@file:JvmName("Bot")

package com.seventeenthshard.harmony.bot

import com.seventeenthshard.harmony.bot.commands.AutoPublishCommand
import com.seventeenthshard.harmony.bot.commands.IgnoredChannelsCommand
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.event.domain.channel.*
import discord4j.core.event.domain.guild.*
import discord4j.core.event.domain.message.*
import discord4j.core.event.domain.role.RoleCreateEvent
import discord4j.core.event.domain.role.RoleDeleteEvent
import discord4j.core.event.domain.role.RoleUpdateEvent
import discord4j.rest.util.Permission
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.component1
import reactor.util.function.component2
import reactor.util.function.component3
import java.time.Duration
import java.time.Instant

fun runBot(
    client: GatewayDiscordClient,
    emitter: EventEmitter,
    ignoredChannels: IgnoredChannelsCommand,
    autoPublish: AutoPublishCommand
) {
    emitter.listen<GuildCreateEvent, Any> { event ->
        val guild = event.guild

        Flux.merge(
            ServerInfo.of(guild).map { guild.id to it },
            guild.channels
                .filter { it is GuildMessageChannel && it.id !in ignoredChannels }
                .flatMap {
                    Mono.just(it.id).zipWith(ChannelInfo.of(it as GuildMessageChannel))
                }
                .map { it.t1 to it.t2 },
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

    emitter.map<TextChannelCreateEvent, ChannelInfo> { event ->
        event.channel.id to ChannelInfo.of(event.channel)
    }

    emitter.map<TextChannelUpdateEvent, ChannelInfo> { event ->
        event.current.id to ChannelInfo.of(event.current)
            .filter { event.current.id !in ignoredChannels }
    }

    emitter.map<TextChannelDeleteEvent, ChannelDeletion> { event ->
        event.channel.id to ChannelDeletion.of(Instant.now())
            .filter { event.channel.id !in ignoredChannels }
    }

    emitter.map<NewsChannelCreateEvent, ChannelInfo> {
        it.channel.id to ChannelInfo.of(it.channel)
    }

    emitter.map<NewsChannelUpdateEvent, ChannelInfo> { event ->
        event.current.id to ChannelInfo.of(event.current)
            .filter { event.current.id !in ignoredChannels }
    }

    emitter.map<NewsChannelDeleteEvent, ChannelDeletion> { event ->
        event.channel.id to ChannelDeletion.of(Instant.now())
            .filter { event.channel.id !in ignoredChannels }
    }

    emitter.listen<CategoryUpdateEvent, ChannelInfo> { event ->
        event.current.channels
            .filter { it is GuildMessageChannel }
            .flatMap { Mono.just(it.id).zipWith(ChannelInfo.of(it as GuildMessageChannel)) }
            .map { it.t1 to it.t2 }
    }

    fun validateMessage(
        message: Mono<Message>,
        vararg producers: (Message) -> Publisher<*>
    ): Flux<Pair<Snowflake, () -> Publisher<*>>> =
        message.filter { it.type == Message.Type.DEFAULT && it.channelId !in ignoredChannels }
            .flux()
            .flatMap { msg ->
                emitter.logger.info("Message event received: $msg")
                Flux.fromIterable(producers.map { msg.id to { it(msg) } })
            }

    fun validateMessage(
        message: Message,
        vararg producers: (Message) -> Publisher<*>
    ): Flux<Pair<Snowflake, () -> Publisher<*>>> =
        validateMessage(Mono.just(message), *producers)

    client.eventDispatcher.on(MessageEvent::class.java)
        .flatMap<Pair<Snowflake, () -> Publisher<*>>> { event ->
            when (event) {
                is MessageCreateEvent ->
                    validateMessage(
                        event.message,
                        {
                            NewMessage.of(event.message)
                        }
                    )
                is MessageUpdateEvent ->
                    validateMessage(
                        event.message,
                        { msg ->
                            Mono.justOrEmpty(event.currentContent)
                                .flatMap {
                                    MessageEdit.of(it, msg.editedTimestamp.orElse(Instant.now()))
                                }
                        },
                        {
                            Mono.justOrEmpty(event.currentEmbeds)
                                .flatMap { MessageEmbedUpdate.of(it) }
                        }
                    )
                is MessageDeleteEvent ->
                    if (event.channelId !in ignoredChannels)
                        Mono.just(
                            event.messageId to {
                                MessageDeletion.of(Instant.now())
                            }
                        )
                    else
                        Mono.empty()
                is MessageBulkDeleteEvent ->
                    if (event.channelId !in ignoredChannels)
                        Flux.fromIterable(event.messageIds.map { msg ->
                            msg to {
                                MessageDeletion.of(Instant.now())
                            }
                        })
                    else
                        Flux.empty()
                is ReactionAddEvent ->
                    validateMessage(
                        event.message,
                        {
                            event.user
                                .flatMap { NewReaction.of(it, event.emoji) }
                        }
                    )
                is ReactionRemoveEvent ->
                    validateMessage(
                        event.message,
                        {
                            event.user
                                .flatMap { ReactionRemoval.of(it, event.emoji) }
                        }
                    )
                is ReactionRemoveAllEvent ->
                    validateMessage(
                        event.message,
                        {
                            ReactionClear.of(Instant.now())
                        }
                    )
                else -> Flux.empty()
            }
        }
        .groupBy { it.first.also { id -> emitter.logger.info("Grouped message event for $id") } }
        .flatMap { group ->
            emitter.logger.info("Handling message group ${group.key()}")
            group.take(Duration.ofMillis(1000)).concatMap { (_, builder) -> builder() }
                .concatMap { event ->
                    Mono.create<Unit> {
                        it.success(emitter.emit(group.key()!!, event))
                    }
                }
        }
        .onErrorContinue { t, e ->
            emitter.logger.error("Failed to handle message event, event: $e", t)
        }
        .subscribe()

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
                "ignored-channels" in words -> ignoredChannels.handle(guildId, channel, words, emitter)
                "auto-publish" in words -> autoPublish.handle(guildId, channel, words)
                else -> channel.createMessage("Could not determine option")
            }
        }
        .onErrorContinue { t, e ->
            emitter.logger.error("Failed to handle command, event: $e", t)
        }
        .subscribe()

    autoPublish.listen()

    client.onDisconnect().block()

    emitter.logger.info("Disconnected from Gateway!")
}
