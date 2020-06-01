@file:JvmName("Bot")

package com.seventeenthshard.harmony.bot

import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.GuildMessageChannel
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.util.Permission
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.domain.channel.CategoryUpdateEvent
import discord4j.core.event.domain.channel.NewsChannelCreateEvent
import discord4j.core.event.domain.channel.NewsChannelDeleteEvent
import discord4j.core.event.domain.channel.NewsChannelUpdateEvent
import discord4j.core.event.domain.channel.TextChannelCreateEvent
import discord4j.core.event.domain.channel.TextChannelDeleteEvent
import discord4j.core.event.domain.channel.TextChannelUpdateEvent
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.guild.GuildDeleteEvent
import discord4j.core.event.domain.guild.GuildUpdateEvent
import discord4j.core.event.domain.guild.MemberJoinEvent
import discord4j.core.event.domain.guild.MemberUpdateEvent
import discord4j.core.event.domain.message.MessageBulkDeleteEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.MessageDeleteEvent
import discord4j.core.event.domain.message.MessageEvent
import discord4j.core.event.domain.message.MessageUpdateEvent
import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.event.domain.message.ReactionRemoveAllEvent
import discord4j.core.event.domain.message.ReactionRemoveEvent
import discord4j.core.event.domain.role.RoleCreateEvent
import discord4j.core.event.domain.role.RoleDeleteEvent
import discord4j.core.event.domain.role.RoleUpdateEvent
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuple4
import reactor.util.function.component1
import reactor.util.function.component2
import reactor.util.function.component3
import reactor.util.function.component4
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

fun runBot(client: DiscordClient, emitter: EventEmitter, ignoredChannels: ConcurrentHashMap.KeySetView<String, Boolean>) {
    fun saveIgnoredChannels() {
        Files.write(Paths.get("ignoredChannels.txt"), ignoredChannels)
    }

    emitter.listen<GuildCreateEvent, Any> { event ->
        val guild = event.guild

        Flux.merge(
            ServerInfo.of(guild).map { guild.id to it },
            guild.channels
                .filter { it is GuildMessageChannel && !ignoredChannels.contains(it.id.asString()) }
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
        event.current.id to ChannelInfo.of(event.current).filter { !ignoredChannels.contains(event.current.id.asString()) }
    }

    emitter.map<TextChannelDeleteEvent, ChannelDeletion> { event ->
        event.channel.id to ChannelDeletion.of(Instant.now()).filter { !ignoredChannels.contains(event.channel.id.asString()) }
    }

    emitter.map<NewsChannelCreateEvent, ChannelInfo> {
        it.channel.id to ChannelInfo.of(it.channel)
    }

    emitter.map<NewsChannelUpdateEvent, ChannelInfo> { event ->
        event.current.id to ChannelInfo.of(event.current).filter { !ignoredChannels.contains(event.current.id.asString()) }
    }

    emitter.map<NewsChannelDeleteEvent, ChannelDeletion> { event ->
        event.channel.id to ChannelDeletion.of(Instant.now()).filter { !ignoredChannels.contains(event.channel.id.asString()) }
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
        message.filter { it.type == Message.Type.DEFAULT && !ignoredChannels.contains(it.channelId.asString()) }
            .flux()
            .flatMap { msg ->
                Flux.fromIterable(producers.map { msg.id to { it(msg) } })
            }

    fun validateMessage(
        message: Message,
        vararg producers: (Message) -> Publisher<*>
    ): Flux<Pair<Snowflake, () -> Publisher<*>>> =
        validateMessage(Mono.just(message), *producers)

    client.eventDispatcher.on(MessageEvent::class.java)
        .map { it.also { emitter.logger.info("Message event received: ${it.javaClass}") } }
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
                    if (!ignoredChannels.contains(event.channelId.asString()))
                        Mono.just(
                            event.messageId to {
                                MessageDeletion.of(Instant.now())
                            }
                        )
                    else
                        Mono.empty()
                is MessageBulkDeleteEvent ->
                    if (!ignoredChannels.contains(event.channelId.asString()))
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
        .groupBy { it.first }
        .flatMap { group ->
            group.flatMapSequential { (_, builder) -> builder() }
                .flatMapSequential { event ->
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

    val channelMentionRegex = Regex("^<#(\\d+)>$")
    client.eventDispatcher.on(MessageCreateEvent::class.java)
        .flatMap { event ->
            Mono.zip(
                event.message.channel.flatMap { if (it is GuildMessageChannel) Mono.just(it) else Mono.empty() },
                Mono.justOrEmpty(event.guildId),
                Mono.just(event.message),
                event.member.map { it.basePermissions }.orElse(Mono.empty())
            )
        }
        .filter { it.t4.contains(Permission.MANAGE_CHANNELS) }
        .filterWhen { event -> event.t3.userMentions.any { u -> u.isBot && u.id == client.selfId.orElse(null) } }
        .map { (it.t1 to it.t2) to it.t3.content.orElse("").split(" ") }
        .filter { (_, words) -> "ignored-channels" in words }
        .flatMap { (params, words) ->
            val (channel, guildId) = params
            val mentionedChannels = words.mapNotNull { word -> channelMentionRegex.find(word)?.groupValues?.get(1) }
            val (action, channelIds) = when {
                "add" in words -> "add" to mentionedChannels
                "remove" in words -> "remove" to mentionedChannels
                "list" in words -> "list" to ignoredChannels.toList()
                else -> return@flatMap Mono.empty<Tuple4<GuildMessageChannel, String, Boolean, List<GuildMessageChannel>>>()
            }
            Mono.zip(
                Mono.just(channel),
                Mono.just(action),
                Mono.just("--clear" in words),
                Flux.fromIterable(channelIds)
                    .flatMap { client.getChannelById(Snowflake.of(it)).onErrorResume { Mono.empty() } }
                    .filter { it is GuildMessageChannel && it.guildId == guildId }
                    .map { it as GuildMessageChannel }
                    .collectList()
            )
        }
        .flatMap { (channel, action, clear, channels) ->
            val channelList = channels.joinToString("\n") { " - ${it.mention}" }
            when (action) {
                "add" -> {
                    ignoredChannels.addAll(channels.map { it.id.asString() })
                    val sideEffect =
                        if (clear)
                            Flux.fromIterable(channels)
                                .flatMap { Mono.zip(Mono.just(it.id), ChannelRemoval.of(Instant.now())) }
                                .map { emitter.emit(it.t1, it.t2) }
                                .collectList()
                        else
                            Mono.just(Unit)
                    sideEffect
                        .flatMap {
                            saveIgnoredChannels()
                            channel.createMessage("Successfully started ignoring the following channels:\n$channelList")
                        }
                        .onErrorResume {
                            channel.createMessage(
                                """
                                Failed to ignore the following channels:
                                $channelList
                                
                                Error Message:
                                ```
                                ${it.message}
                                ```
                                """.trimIndent()
                            )
                        }
                }
                "remove" -> {
                    ignoredChannels.removeAll(channels.map { it.id.asString() })
                    Flux.fromIterable(channels)
                        .flatMap { ChannelInfo.of(it) }
                        .map { emitter.emit(Snowflake.of(it.id), it) }
                        .collectList()
                        .flatMap {
                            saveIgnoredChannels()
                            channel.createMessage("Successfully stopped ignoring the following channels:\n$channelList")
                        }
                        .onErrorResume {
                            channel.createMessage(
                                """
                                Failed to stop ignoring the following channels:
                                $channelList
                                
                                Error Message:
                                ```
                                ${it.message}
                                ```
                                """.trimIndent()
                            )
                        }
                }
                else -> {
                    channel.createMessage("Currently the following channels are ignored:\n$channelList")
                }
            }
        }
        .onErrorContinue { t, e ->
            emitter.logger.error("Failed to handle command, event: $e", t)
        }
        .subscribe()

    client.login().block()
}
