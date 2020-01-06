@file:JvmName("DBImport")

package com.seventeenthshard.harmony.dbimport

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
import com.sksamuel.avro4k.Avro
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.subject.RecordNameStrategy
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.serializer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.logging.log4j.LogManager
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.replace
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Properties

class EventHandler(init: EventHandler.() -> Unit) {
    private val handlers = mutableMapOf<String, Handler<*>>()
    private val logger = LogManager.getLogger("EventHandler")

    init {
        this.init()
    }

    fun <T> addHandler(
        schemaName: String,
        deserializer: DeserializationStrategy<T>,
        handler: (id: String, event: T) -> Unit
    ) {
        handlers[schemaName] = Handler(deserializer, handler)
    }

    inline fun <reified T : Any> listen(noinline handler: (id: String, event: T) -> Unit) {
        addHandler(T::class.java.name, T::class.serializer(), handler)
    }

    fun consume(settings: Map<String, Any>, vararg topics: String) {
        val props = Properties()
        props.putAll(settings)

        KafkaConsumer<String, GenericRecord>(props).use { consumer ->
            consumer.subscribe(topics.toList())

            while (true) {
                consumer.poll(Duration.ofMillis(100)).forEach {
                    val record = it.value()
                    try {
                        (handlers[it.value().schema.fullName] as? Handler<Any>)?.let { handler ->
                            val event = Avro.default.fromRecord(handler.deserializer, record)
                            handler.run(it.key(), event)
                            logger.info("Handled ${event.javaClass}")
                        } ?: logger.warn("Skipping event $it")
                    } catch (e: MissingFieldException) {
                        logger.warn("Skipping event due to missing fields")
                    }
                }
            }
        }
    }

    private data class Handler<T>(val deserializer: DeserializationStrategy<T>, val run: (id: String, event: T) -> Unit)
}

private fun insertEmbeds(messageId: String, embeds: List<Embed>) {
    MessageEmbeds.deleteWhere { MessageEmbeds.message eq messageId }

    embeds.forEach { embed ->
        val embedId = MessageEmbeds.insertAndGetId {
            it[message] = messageId
            it[type] = embed.type
            it[title] = embed.title
            it[description] = embed.description
            it[url] = embed.url
            it[color] = embed.color
            it[timestamp] = embed.timestamp?.let { ts -> LocalDateTime.ofInstant(ts, ZoneId.of("UTC")) }

            it[footerText] = embed.footer?.text
            it[footerIconUrl] = embed.footer?.iconUrl
            it[footerIconProxyUrl] = embed.footer?.proxyIconUrl

            it[imageUrl] = embed.image?.url
            it[imageProxyUrl] = embed.image?.proxyUrl
            it[imageWidth] = embed.image?.width
            it[imageHeight] = embed.image?.height

            it[thumbnailUrl] = embed.thumbnail?.url
            it[thumbnailProxyUrl] = embed.thumbnail?.proxyUrl
            it[thumbnailWidth] = embed.thumbnail?.width
            it[thumbnailHeight] = embed.thumbnail?.height

            it[videoUrl] = embed.video?.url
            it[videoProxyUrl] = embed.video?.proxyUrl
            it[videoWidth] = embed.video?.width
            it[videoHeight] = embed.video?.height

            it[providerName] = embed.provider?.name
            it[providerUrl] = embed.provider?.url

            it[authorName] = embed.author?.name
            it[authorUrl] = embed.author?.url
            it[authorIconUrl] = embed.author?.iconUrl
            it[authorIconProxyUrl] = embed.author?.proxyIconUrl
        }

        MessageEmbedFields.batchInsert(embed.fields.withIndex()) { (index, field) ->
            this[MessageEmbedFields.embed] = embedId
            this[MessageEmbedFields.position] = index
            this[MessageEmbedFields.name] = field.name
            this[MessageEmbedFields.value] = field.value
            this[MessageEmbedFields.inline] = field.inline
        }
    }
}

fun runImport() {
    val events = EventHandler {
        listen<ServerInfo> { serverId, event ->
            transaction {
                Servers.replace {
                    it[id] = serverId
                    it[name] = event.name
                    it[iconUrl] = event.iconUrl
                    it[active] = true
                }
            }
        }
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

        listen<ChannelInfo> { channelId, event ->
            transaction {
                Channels.replace {
                    it[id] = channelId
                    it[server] = event.server.id
                    it[name] = event.name
                    it[category] = event.category
                    it[position] = event.position
                    it[type] = event.type
                }

                PermissionOverrides.deleteWhere { PermissionOverrides.channel eq channelId }

                PermissionOverrides.batchInsert(event.permissionOverrides) {
                    this[PermissionOverrides.channel] = event.id
                    this[PermissionOverrides.type] = it.type
                    this[PermissionOverrides.target] = it.targetId
                    this[PermissionOverrides.allowed] = it.allowed
                    this[PermissionOverrides.denied] = it.denied
                }
            }
        }
        listen<ChannelDeletion> { channelId, event ->
            transaction {
                Channels.update({ Channels.id eq channelId }) {
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

        listen<NewMessage> { messageId, event ->
            transaction {
                Users.replace {
                    it[id] = event.user.id
                    it[name] = event.user.username
                    it[discriminator] = event.user.discriminator
                    it[bot] = event.user.isBot
                }

                val creationTimestamp = LocalDateTime.ofInstant(event.timestamp, ZoneId.of("UTC"))

                Messages.replace {
                    it[id] = messageId
                    it[server] = event.channel.server.id
                    it[channel] = event.channel.id
                    it[user] = event.user.id
                    it[createdAt] = creationTimestamp
                }

                MessageVersions.replace {
                    it[message] = messageId
                    it[timestamp] = creationTimestamp
                    it[content] = event.content
                }

                MessageAttachments.deleteWhere { MessageAttachments.message eq messageId }
                MessageAttachments.batchInsert(event.attachments) {
                    this[MessageAttachments.message] = messageId
                    this[MessageAttachments.name] = it.name
                    this[MessageAttachments.url] = it.url
                    this[MessageAttachments.proxyUrl] = it.proxyUrl
                    this[MessageAttachments.width] = it.width
                    this[MessageAttachments.height] = it.height
                    this[MessageAttachments.spoiler] = it.spoiler
                }

                insertEmbeds(messageId, event.embeds)
            }
        }
        listen<MessageEdit> { messageId, event ->
            transaction {
                MessageVersions.replace {
                    it[message] = messageId
                    it[timestamp] = LocalDateTime.ofInstant(event.timestamp, ZoneId.of("UTC"))
                    it[content] = event.content
                }
            }
        }
        listen<MessageEmbedUpdate> { messageId, event ->
            transaction {
                insertEmbeds(messageId, event.embeds)
            }
        }
        listen<MessageDeletion> { messageId, event ->
            transaction {
                Messages.update({ Messages.id eq messageId }) {
                    it[deletedAt] = LocalDateTime.ofInstant(event.timestamp, ZoneId.of("UTC"))
                }
            }
        }
        listen<NewReaction> { messageId, event ->
            transaction {
                Users.replace {
                    it[id] = event.user.id
                    it[name] = event.user.username
                    it[discriminator] = event.user.discriminator
                    it[bot] = event.user.isBot
                }

                MessageReactions.replace {
                    it[message] = messageId
                    it[user] = event.user.id
                    it[type] = event.type
                    it[emoji] = event.emoji
                    it[emojiId] = event.emojiId ?: "0"
                    it[emojiAnimated] = event.emojiAnimated
                    it[createdAt] = LocalDateTime.ofInstant(event.timestamp, ZoneId.of("UTC"))
                }
            }
        }
        listen<ReactionRemoval> { messageId, event ->
            transaction {
                MessageReactions.update({
                    (MessageReactions.message eq messageId) and
                        (MessageReactions.user eq event.user.id) and
                        (MessageReactions.type eq event.type) and
                        (MessageReactions.emoji eq event.emoji) and
                        (MessageReactions.emojiId eq (event.emojiId ?: "0"))
                }) {
                    it[deletedAt] = LocalDateTime.ofInstant(event.timestamp, ZoneId.of("UTC"))
                }
            }
        }
        listen<ReactionClear> { messageId, event ->
            transaction {
                MessageReactions.update({ (MessageReactions.message eq messageId) and MessageReactions.deletedAt.isNull() }) {
                    it[deletedAt] = LocalDateTime.ofInstant(event.timestamp, ZoneId.of("UTC"))
                }
            }
        }

        listen<ChannelRemoval> { channelId, _ ->
            transaction {
                PermissionOverrides.deleteWhere {
                    PermissionOverrides.channel eq channelId
                }

                val versionsStatement = connection.prepareStatement(
                    "DELETE FROM messageversions USING messages WHERE messageversions.message = messages.id AND messages.channel = ?",
                    false
                )
                versionsStatement.fillParameters(listOf(Messages.channel.columnType to channelId))
                versionsStatement.executeUpdate()

                val embedsStatement = connection.prepareStatement(
                    "DELETE FROM messageembeds USING messages WHERE messageembeds.message = messages.id AND messages.channel = ?",
                    false
                )
                embedsStatement.fillParameters(listOf(Messages.channel.columnType to channelId))
                embedsStatement.executeUpdate()

                val attachmentsStatement = connection.prepareStatement(
                    "DELETE FROM messageattachments USING messages WHERE messageattachments.message = messages.id AND messages.channel = ?",
                    false
                )
                attachmentsStatement.fillParameters(listOf(Messages.channel.columnType to channelId))
                attachmentsStatement.executeUpdate()

                val reactionsStatement = connection.prepareStatement(
                    "DELETE FROM messagereactions USING messages WHERE messagereactions.message = messages.id AND messages.channel = ?",
                    false
                )
                reactionsStatement.fillParameters(listOf(Messages.channel.columnType to channelId))
                reactionsStatement.executeUpdate()

                Messages.deleteWhere {
                    Messages.channel eq channelId
                }

                Channels.deleteWhere {
                    Channels.id eq channelId
                }
            }
        }
    }

    events.consume(
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to (System.getenv("BROKER_URLS")
                ?: throw IllegalArgumentException("BROKER_URLS env variable must be set!")),
            ConsumerConfig.GROUP_ID_CONFIG to "db-import",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "true",
            ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG to "1000",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
            KafkaAvroDeserializerConfig.VALUE_SUBJECT_NAME_STRATEGY to RecordNameStrategy::class.java,
            KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to (System.getenv("SCHEMA_REGISTRY_URL")
                ?: throw IllegalArgumentException("SCHEMA_REGISTRY_URL env variable must be set!"))
        ),
        "servers", "messages", "channels", "users", "roles"
    )
}
