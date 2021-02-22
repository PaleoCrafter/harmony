@file:JvmName("DBDump")

package com.seventeenthshard.harmony.bot.handlers.db

import com.seventeenthshard.harmony.bot.ChannelInfo
import com.seventeenthshard.harmony.bot.UserInfo
import com.seventeenthshard.harmony.bot.toHex
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.reaction.ReactionEmoji
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import reactor.util.function.Tuple3
import reactor.util.function.component1
import reactor.util.function.component2
import reactor.util.function.component3
import java.sql.Connection
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun buildDbDumperImpl(): (
    channel: ChannelInfo,
    messages: List<Tuple3<Message, UserInfo, List<Pair<ReactionEmoji, Snowflake>>>>
) -> Unit =
    { channel, messages ->
        transaction(Connection.TRANSACTION_READ_COMMITTED, 2) {
            Servers.replace {
                it[id] = channel.server.id
                it[name] = channel.server.name
                it[iconUrl] = channel.server.iconUrl
                it[active] = true
            }

            Channels.replace {
                it[id] = channel.id
                it[server] = channel.server.id
                it[name] = channel.name
                it[category] = channel.category
                it[categoryPosition] = channel.categoryPosition
                it[position] = channel.position
                it[type] = channel.type
                it[dumpedAt] = LocalDateTime.now()
            }

            PermissionOverrides.deleteWhere { PermissionOverrides.channel eq channel.id }

            PermissionOverrides.batchInsert(channel.permissionOverrides) {
                this[PermissionOverrides.channel] = channel.id
                this[PermissionOverrides.type] = it.type
                this[PermissionOverrides.target] = it.targetId
                this[PermissionOverrides.allowed] = it.allowed
                this[PermissionOverrides.denied] = it.denied
            }

            val messageIds = messages.map { (msg) -> msg.id.asString() }
            val existing = Messages.select { Messages.id inList messageIds }
                .map { it[Messages.id] }

            Users.batchInsert(messages.mapNotNull { it.t2 }, ignore = true) {
                this[Users.id] = it.id
                this[Users.name] = it.username
                this[Users.discriminator] = it.discriminator
                this[Users.bot] = it.isBot
            }

            Messages.batchInsert(messages, ignore = true) { (msg, author) ->
                val creationTimestamp = LocalDateTime.ofInstant(msg.timestamp, ZoneId.of("UTC"))

                this[Messages.id] = msg.id.asString()
                this[Messages.server] = channel.server.id
                this[Messages.channel] = msg.channelId.asString()
                this[Messages.user] = author.id
                this[Messages.webhookName] = author.webhookName
                val ref = msg.messageReference.orElse(null)
                this[Messages.referencedServer] = ref?.guildId?.orElse(null)?.asString()
                this[Messages.referencedChannel] = ref?.channelId?.asString()
                this[Messages.referencedMessage] = ref?.messageId?.orElse(null)?.asString()
                this[Messages.createdAt] = creationTimestamp
            }

            val (existingMessages, newMessages) = messages.partition { it.t1.id.asString() in existing }
            existingMessages.forEach { (msg) ->
                val lastVersion = MessageVersions.select { MessageVersions.message eq msg.id.asString() }
                    .orderBy(MessageVersions.timestamp, SortOrder.DESC).limit(1).firstOrNull()

                val requiresEdit = if (lastVersion === null) {
                    val creationTimestamp = LocalDateTime.ofInstant(msg.timestamp, ZoneId.of("UTC"))
                    MessageVersions.insert {
                        it[message] = msg.id.asString()
                        it[content] = msg.content
                        it[timestamp] = creationTimestamp
                    }

                    msg.editedTimestamp.isPresent
                } else lastVersion[MessageVersions.content] != msg.content

                if (requiresEdit && msg.editedTimestamp.isPresent) {
                    val editTimestamp = LocalDateTime.ofInstant(msg.editedTimestamp.get(), ZoneId.of("UTC"))
                    MessageVersions.insert {
                        it[message] = msg.id.asString()
                        it[content] = msg.content
                        it[timestamp] = editTimestamp
                    }
                }
            }

            newMessages.forEach { (msg) ->
                val creationTimestamp = LocalDateTime.ofInstant(msg.timestamp, ZoneId.of("UTC"))
                MessageVersions.replace {
                    it[message] = msg.id.asString()
                    it[content] = msg.content
                    it[timestamp] = creationTimestamp
                }
                val editTimestamp = msg.editedTimestamp.orElse(null)?.let {
                    LocalDateTime.ofInstant(it, ZoneId.of("UTC"))
                }

                if (editTimestamp != null) {
                    MessageVersions.replace {
                        it[message] = msg.id.asString()
                        it[content] = msg.content
                        it[timestamp] = editTimestamp
                    }
                }

                MessageAttachments.batchInsert(msg.attachments) {
                    this[MessageAttachments.message] = msg.id.asString()
                    this[MessageAttachments.name] = it.filename
                    this[MessageAttachments.url] = it.url
                    this[MessageAttachments.proxyUrl] = it.proxyUrl
                    this[MessageAttachments.width] = if (it.width.isPresent) it.width.asInt else null
                    this[MessageAttachments.height] = if (it.height.isPresent) it.height.asInt else null
                    this[MessageAttachments.spoiler] = it.isSpoiler
                }
            }

            MessageEmbeds.deleteWhere { MessageEmbeds.message inList messageIds }

            messages.flatMap { (msg) -> msg.embeds.map { msg.id.asString() to it } }.forEach { (msg, embed) ->
                val embedId = MessageEmbeds.insertAndGetId {
                    it[message] = msg
                    it[type] = embed.type?.name?.let { t -> MessageEmbeds.Type.valueOf(t) } ?: MessageEmbeds.Type.UNKNOWN
                    it[title] = embed.title.orElse(null)
                    it[description] = embed.description.orElse(null)
                    it[url] = embed.url.orElse(null)
                    it[color] = embed.color.orElse(null)?.toHex()
                    it[timestamp] = embed.timestamp.orElse(null)?.let {
                            ts -> LocalDateTime.ofInstant(ts, ZoneId.of("UTC"))
                    }

                    it[footerText] = embed.footer.orElse(null)?.text
                    it[footerIconUrl] = embed.footer.orElse(null)?.iconUrl
                    it[footerIconProxyUrl] = embed.footer.orElse(null)?.proxyIconUrl

                    it[imageUrl] = embed.image.orElse(null)?.url
                    it[imageProxyUrl] = embed.image.orElse(null)?.proxyUrl
                    it[imageWidth] = embed.image.orElse(null)?.width
                    it[imageHeight] = embed.image.orElse(null)?.height

                    it[thumbnailUrl] = embed.thumbnail.orElse(null)?.url
                    it[thumbnailProxyUrl] = embed.thumbnail.orElse(null)?.proxyUrl
                    it[thumbnailWidth] = embed.thumbnail.orElse(null)?.width
                    it[thumbnailHeight] = embed.thumbnail.orElse(null)?.height

                    it[videoUrl] = embed.video.orElse(null)?.url
                    it[videoProxyUrl] = embed.video.orElse(null)?.url
                    it[videoWidth] = embed.video.orElse(null)?.width
                    it[videoHeight] = embed.video.orElse(null)?.height

                    it[providerName] = embed.provider.orElse(null)?.name
                    it[providerUrl] = embed.provider.orElse(null)?.url?.orElse(null)

                    it[authorName] = embed.author.orElse(null)?.name
                    it[authorUrl] = embed.author.orElse(null)?.url
                    it[authorIconUrl] = embed.author.orElse(null)?.iconUrl
                    it[authorIconProxyUrl] = embed.author.orElse(null)?.proxyIconUrl
                }

                MessageEmbedFields.batchInsert(embed.fields.withIndex()) { (index, field) ->
                    this[MessageEmbedFields.embed] = embedId
                    this[MessageEmbedFields.position] = index
                    this[MessageEmbedFields.name] = field.name
                    this[MessageEmbedFields.value] = field.value
                    this[MessageEmbedFields.inline] = field.isInline
                }
            }

            MessageReactions.batchInsert(
                messages.flatMap { (msg, _, reactions) -> reactions.map { msg.id to it } },
                ignore = true
            ) { (messageId, reaction) ->
                val (emoji, user) = reaction
                this[MessageReactions.message] = messageId.asString()
                this[MessageReactions.user] = user.asString()

                emoji.asUnicodeEmoji().ifPresent {
                    this[MessageReactions.emoji] = it.raw
                    this[MessageReactions.type] = MessageReactions.Type.UNICODE
                    this[MessageReactions.emojiId] = "0"
                    this[MessageReactions.emojiAnimated] = false
                }

                emoji.asCustomEmoji().ifPresent {
                    this[MessageReactions.emoji] = it.name
                    this[MessageReactions.type] = MessageReactions.Type.CUSTOM
                    this[MessageReactions.emojiId] = it.id.asString()
                    this[MessageReactions.emojiAnimated] = it.isAnimated
                }

                this[MessageReactions.createdAt] = LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC"))
            }
        }
    }
