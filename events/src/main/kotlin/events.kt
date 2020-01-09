package com.seventeenthshard.harmony.events

import com.sksamuel.avro4k.serializer.InstantSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.CommonEnumSerializer
import java.awt.Color
import java.time.Instant

fun Color.toHex(): String {
    val format = "%02x"

    return "${format.format(red)}${format.format(green)}${format.format(blue)}"
}

@Serializable
data class ServerInfo(val id: String, val name: String, val iconUrl: String?)

@Serializable
data class ServerDeletion(
    @Serializable(with = InstantSerializer::class) val timestamp: Instant
)

@Serializable
data class RoleInfo(
    val id: String,
    val server: ServerInfo,
    val name: String,
    val color: String,
    val position: Int,
    val permissions: Long
)

@Serializable
data class RoleDeletion(
    @Serializable(with = InstantSerializer::class) val timestamp: Instant
)

@Serializable
data class ChannelInfo(
    val id: String,
    val server: ServerInfo,
    val name: String,
    val category: String = "Text channels",
    val position: Int,
    val type: Type,
    val permissionOverrides: List<PermissionOverride>
) {
    @Serializable(with = Type.Serializer::class)
    enum class Type {
        TEXT, NEWS;

        companion object Serializer : CommonEnumSerializer<Type>(
            serialName = "ChannelType",
            choices = values(),
            choicesNames = arrayOf("TEXT", "NEWS")
        )
    }

    @Serializable
    data class PermissionOverride(val type: Type, val targetId: String, val allowed: Long, val denied: Long) {
        @Serializable(with = Type.Serializer::class)
        enum class Type {
            UNKNOWN, ROLE, USER;

            companion object Serializer : CommonEnumSerializer<Type>(
                serialName = "ChannelPermissionOverrideType",
                choices = values(),
                choicesNames = arrayOf("UNKNOWN", "ROLE", "USER")
            )
        }
    }
}

@Serializable
data class ChannelDeletion(
    @Serializable(with = InstantSerializer::class) val timestamp: Instant
)

@Serializable
data class ChannelRemoval(
    @Serializable(with = InstantSerializer::class) val timestamp: Instant
)

@Serializable
data class UserInfo(
    val id: String,
    val username: String,
    val discriminator: String,
    val isBot: Boolean
)

@Serializable
data class UserNicknameChange(
    val user: UserInfo,
    val server: ServerInfo,
    val nickname: String?,
    @Serializable(with = InstantSerializer::class) val timestamp: Instant
)

@Serializable
data class UserRolesChange(
    val user: UserInfo,
    val server: ServerInfo,
    val roles: List<String>,
    @Serializable(with = InstantSerializer::class) val timestamp: Instant
)

@Serializable
data class NewMessage(
    val channel: ChannelInfo,
    val user: UserInfo,
    val content: String,
    val embeds: List<Embed>,
    val attachments: List<Attachment>,
    @Serializable(with = InstantSerializer::class) val timestamp: Instant
) {
    @Serializable
    data class Attachment(
        val name: String,
        val url: String,
        val proxyUrl: String,
        val width: Int?,
        val height: Int?,
        val spoiler: Boolean
    )
}

@Serializable
data class MessageEdit(
    val content: String,
    @Serializable(with = InstantSerializer::class) val timestamp: Instant
)

@Serializable
data class MessageDeletion(
    @Serializable(with = InstantSerializer::class) val timestamp: Instant
)

@Serializable
data class MessageEmbedUpdate(val embeds: List<Embed>)

@Serializable
data class Embed(
    val type: Type,
    val title: String?,
    val description: String?,
    val url: String?,
    val color: String?,
    @Serializable(with = InstantSerializer::class) val timestamp: Instant?,
    val footer: Footer?,
    val image: Media?,
    val thumbnail: Media?,
    val video: Media?,
    val provider: Provider?,
    val author: Author?,
    val fields: List<Field>
) {
    @Serializable(with = Type.Serializer::class)
    enum class Type {
        UNKNOWN, IMAGE, LINK, RICH, VIDEO;

        companion object Serializer : CommonEnumSerializer<Type>(
            serialName = "EmbedType",
            choices = values(),
            choicesNames = arrayOf("UNKNOWN", "IMAGE", "LINK", "RICH", "VIDEO")
        )
    }

    @Serializable
    data class Footer(val text: String, val iconUrl: String?, val proxyIconUrl: String?)

    @Serializable
    data class Media(val url: String?, val proxyUrl: String?, val width: Int?, val height: Int?)

    @Serializable
    data class Provider(val name: String?, val url: String?)

    @Serializable
    data class Author(val name: String?, val url: String?, val iconUrl: String?, val proxyIconUrl: String?)

    @Serializable
    data class Field(val name: String, val value: String, val inline: Boolean)
}

@Serializable
data class NewReaction(
    val user: UserInfo,
    val type: Type,
    val emoji: String,
    val emojiId: String?,
    val emojiAnimated: Boolean,
    @Serializable(with = InstantSerializer::class) val timestamp: Instant
) {
    @Serializable(with = Type.Serializer::class)
    enum class Type {
        UNICODE, CUSTOM;

        companion object Serializer : CommonEnumSerializer<Type>(
            serialName = "ReactionType",
            choices = values(),
            choicesNames = arrayOf("UNICODE", "CUSTOM")
        )
    }
}

@Serializable
data class ReactionRemoval(
    val user: UserInfo,
    val type: NewReaction.Type,
    val emoji: String,
    val emojiId: String?,
    @Serializable(with = InstantSerializer::class) val timestamp: Instant
)

@Serializable
data class ReactionClear(@Serializable(with = InstantSerializer::class) val timestamp: Instant)
