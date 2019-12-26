package com.seventeenthshard.harmony.events

import com.sksamuel.avro4k.serializer.InstantSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.CommonEnumSerializer
import java.time.Instant

@Serializable
data class ServerInfo(val id: String, val name: String, val iconUrl: String?)

@Serializable
data class ServerDeletion(
    @Serializable(with = InstantSerializer::class) val timestamp: Instant
)

@Serializable
data class ChannelInfo(val id: String, val server: ServerInfo, val name: String, val type: Type) {
    @Serializable(with = Type.Serializer::class)
    enum class Type {
        TEXT, NEWS;

        companion object Serializer : CommonEnumSerializer<Type>(
            serialName = "ChannelType",
            choices = values(),
            choicesNames = arrayOf("TEXT", "NEWS")
        )
    }
}

@Serializable
data class ChannelDeletion(
    val user: UserInfo,
    @Serializable(with = InstantSerializer::class) val timestamp: Instant
)

@Serializable
data class UserInfo(
    val id: String,
    val username: String,
    val discriminator: String
)

@Serializable
data class UserNicknameChange(
    val user: UserInfo,
    val server: ServerInfo,
    val nickname: String,
    @Serializable(with = InstantSerializer::class) val timestamp: Instant
)

@Serializable
data class NewMessage(
    val channel: ChannelInfo,
    val user: UserInfo,
    val content: String?,
    @Serializable(with = InstantSerializer::class) val timestamp: Instant
)

@Serializable
data class MessageEdit(
    val content: String,
    @Serializable(with = InstantSerializer::class) val timestamp: Instant
)

@Serializable
data class MessageDeletion(
    @Serializable(with = InstantSerializer::class) val timestamp: Instant
)
