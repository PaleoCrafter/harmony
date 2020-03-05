@file:JvmName("ElasticDump")

package com.seventeenthshard.harmony.bot.handlers.elastic

import com.seventeenthshard.harmony.bot.UserInfo
import discord4j.core.`object`.Embed
import discord4j.core.`object`.entity.Attachment
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.GuildMessageChannel
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.`object`.util.Snowflake
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import reactor.util.function.Tuple3
import reactor.util.function.component1
import reactor.util.function.component2

fun buildElasticDumperImpl(elasticClient: RestHighLevelClient): (
    guild: Guild,
    channel: GuildMessageChannel,
    messages: List<Tuple3<Message, UserInfo, List<Pair<ReactionEmoji, Snowflake>>>>
) -> Unit =
    { guild, channel, messages ->
        messages.forEach { (message, author) ->
            val (_, state) = DiscordMarkdownRules.parse(
                message.content.orElse("")
            )
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

            elasticClient.index(
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
                        "content" to message.content.orElse(""),
                        "has" to messageProperties,
                        "mentions" to state.mentionedUsers,
                        "attachments" to message.attachments.isNotEmpty(),
                        "timestamp" to message.timestamp.toEpochMilli()
                    )
                ),
                RequestOptions.DEFAULT
            )
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
