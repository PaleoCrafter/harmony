@file:JvmName("ElasticDump")

package com.seventeenthshard.harmony.bot.handlers.elastic

import com.seventeenthshard.harmony.bot.UserInfo
import discord4j.common.util.Snowflake
import discord4j.core.`object`.Embed
import discord4j.core.`object`.entity.Attachment
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.`object`.reaction.ReactionEmoji
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.elasticsearch.action.bulk.BackoffPolicy
import org.elasticsearch.action.bulk.BulkProcessor
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.unit.TimeValue
import reactor.util.function.Tuple3
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

private object BulkListener : BulkProcessor.Listener {
    private val logger: Logger = LogManager.getLogger("elastic-bulk")

    override fun beforeBulk(executionId: Long, request: BulkRequest) {
        logger.info("About to perform bulk request [${executionId}] with ${request.numberOfActions()} actions")
    }

    override fun afterBulk(executionId: Long, request: BulkRequest, response: BulkResponse) {
        if (response.hasFailures()) {
            logger.warn("Bulk [${executionId}] executed with failures")
        } else {
            logger.info("Bulk [${executionId}] completed in ${response.took.millis} milliseconds")
        }
    }

    override fun afterBulk(executionId: Long, request: BulkRequest, failure: Throwable) {
        logger.error("Failed to execute bulk [${executionId}]", failure)
    }
}

fun buildElasticDumperImpl(elasticClient: RestHighLevelClient): (
    guild: Guild,
    channel: GuildMessageChannel,
    messages: List<Tuple3<Message, UserInfo, List<Pair<ReactionEmoji, Snowflake>>>>
) -> Unit {
    val bulkProcessor = BulkProcessor.builder(
        { request, bulkListener -> elasticClient.bulkAsync(request, RequestOptions.DEFAULT, bulkListener) },
        BulkListener
    ).run {
        setFlushInterval(TimeValue.timeValueSeconds(10L))
        setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueMillis(50L), Int.MAX_VALUE))
    }.build()

    return { guild, channel, messages ->
        messages.forEach { (message, author) ->
            val (_, state) = DiscordMarkdownRules.parse(message.content)
            val messageProperties = mutableSetOf<String>()

            if (state.hasLinks) {
                messageProperties += "link"
            }

            if (message.attachments.isNotEmpty()) {
                messageProperties += "file"
            }

            if (message.embeds.isNotEmpty()) {
                messageProperties += "embed"
            }

            messageProperties += message.attachments.map { it.type }
            messageProperties += message.embeds.flatMap { it.mediaTypes }

            bulkProcessor.add(
                IndexRequest(INDEX).id(message.id.asString()).source(
                    mapOf(
                        "tie_breaker_id" to message.id.asString(),
                        "server" to guild.id.asString(),
                        "channel" to mapOf("id" to channel.id.asString(), "name" to channel.name),
                        "author" to mapOf(
                            "id" to author.id,
                            "name" to author.username,
                            "discriminator" to author.discriminator
                        ),
                        "content" to message.content,
                        "has" to messageProperties,
                        "mentions" to state.mentionedUsers,
                        "attachments" to message.attachments.isNotEmpty(),
                        "timestamp" to message.timestamp.toEpochMilli()
                    )
                )
            )
        }

        bulkProcessor.flush()
    }
}

private val Attachment.type: String
    get() = when (this.filename.substring(this.filename.lastIndexOf('.') + 1).toLowerCase()) {
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

        if (type === Embed.Type.IMAGE || image.isPresent) {
            types += "image"
        }

        if (type === Embed.Type.VIDEO || video.isPresent) {
            types += "video"
        }

        return types
    }
