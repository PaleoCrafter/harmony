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
    val category: String,
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
data class UserInfo(
    val id: String,
    val username: String,
    val discriminator: String
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
