@file:JvmName("ElasticImport")

package com.seventeenthshard.harmony.bot.handlers.elastic

import com.seventeenthshard.harmony.bot.*
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import java.io.IOException

fun buildElasticHandlerImpl(elasticClient: RestHighLevelClient) =
    EventHandler("elastic-ingest") {
        listen<NewMessage> { id, event ->
            val (_, state) = DiscordMarkdownRules.parse(
                event.content
            )
            val messageProperties = mutableSetOf<String>()

            if (state.hasLinks) {
                messageProperties += "link"
            }

            if (event.attachments.isNotEmpty()) {
                messageProperties += "file"
            }

            if (event.embeds.isNotEmpty()) {
                messageProperties += "embed"
            }

            messageProperties += event.attachments.map { it.type }
            messageProperties += event.embeds.flatMap { it.mediaTypes }

            elasticClient.index(
                IndexRequest(INDEX).id(id).source(
                    mapOf(
                        "tie_breaker_id" to id,
                        "server" to event.channel.server.id,
                        "channel" to mapOf("id" to event.channel.id, "name" to event.channel.name),
                        "author" to mapOf(
                            "id" to event.user.id,
                            "name" to event.user.username,
                            "discriminator" to event.user.discriminator
                        ),
                        "content" to event.content,
                        "has" to messageProperties,
                        "mentions" to state.mentionedUsers,
                        "attachments" to event.attachments.isNotEmpty(),
                        "timestamp" to event.timestamp.toEpochMilli()
                    )
                ),
                RequestOptions.DEFAULT
            )
        }

        listen<MessageEdit> { id, event ->
            val existing = try {
                elasticClient.get(GetRequest(INDEX, id), RequestOptions.DEFAULT)
            } catch (exception: IOException) {
                logger.warn("Skipping message edit due to missing existing document")
                return@listen
            }
            val (_, state) = DiscordMarkdownRules.parse(
                event.content
            )
            val messageProperties = existing.getField("has")?.values.orEmpty().mapTo(mutableSetOf()) { it.toString() }
            messageProperties -= "link"

            if (state.hasLinks) {
                messageProperties += "link"
            }

            elasticClient.update(
                UpdateRequest(INDEX, id).doc(
                    mapOf(
                        "content" to event.content,
                        "has" to messageProperties
                    )
                ),
                RequestOptions.DEFAULT
            )
        }

        listen<MessageEmbedUpdate> { id, event ->
            val existing = try {
                elasticClient.get(GetRequest(INDEX, id), RequestOptions.DEFAULT)
            } catch (exception: IOException) {
                logger.warn("Skipping message embed update due to missing existing document")
                return@listen
            }
            val messageProperties = existing.getField("has")?.values.orEmpty()
                .filter { existing.getField("attachments").getValue<Boolean>() || it != "link" }
                .mapTo(mutableSetOf()) { it.toString() }

            messageProperties -= "embed"

            if (event.embeds.isNotEmpty()) {
                messageProperties += "embed"
            }
            messageProperties += event.embeds.flatMap { it.mediaTypes }

            elasticClient.update(
                UpdateRequest(INDEX, id).doc(
                    mapOf(
                        "has" to messageProperties
                    )
                ),
                RequestOptions.DEFAULT
            )
        }

        listen<MessageDeletion> { id, event ->
            elasticClient.update(
                UpdateRequest(INDEX, id).doc(
                    mapOf(
                        "deletedAt" to event.timestamp.toEpochMilli()
                    )
                ),
                RequestOptions.DEFAULT
            )
        }
    }

private val NewMessage.Attachment.type: String
    get() = when (this.name.substring(this.name.lastIndexOf('.') + 1).toLowerCase()) {
        "jpg" -> "image"
        "jpeg" -> "image"
        "webp" -> "image"
        "gif" -> "image"
        "png" -> "image"
        "mp4" -> "video"
        "webm" -> "video"
        "mp3" -> "sound"
        "wav" -> "sound"
        "ogg" -> "sound"
        else -> "file"
    }

private val Embed.mediaTypes: Iterable<String>
    get() {
        val types = mutableSetOf<String>()

        if (type === Embed.Type.IMAGE || image != null) {
            types += "image"
        }

        if (type === Embed.Type.VIDEO || video != null) {
            types += "video"
        }

        return types
    }
