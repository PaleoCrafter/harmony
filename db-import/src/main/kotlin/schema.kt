package com.seventeenthshard.harmony.dbimport

import com.seventeenthshard.harmony.events.ChannelInfo
import com.seventeenthshard.harmony.events.Embed
import com.seventeenthshard.harmony.events.NewReaction
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.vendors.currentDialect

class SnowflakeColumnType : ColumnType() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.longType()

    override fun notNullValueToDB(value: Any): Any {
        return when (value) {
            is String -> value.toULong().toLong()
            else -> value
        }
    }

    override fun valueFromDB(value: Any): Any {
        return when (value) {
            is Long -> value.toULong().toString()
            else -> value
        }
    }
}

fun Table.snowflake(name: String): Column<String> = registerColumn(name, SnowflakeColumnType())

object Servers : Table() {
    val id = snowflake("id").primaryKey()
    val name = varchar("name", 255)
    val iconUrl = varchar("iconUrl", 255).nullable()
    val active = bool("active").default(true)
}

object Channels : Table() {
    val id = snowflake("id").primaryKey()
    val server = snowflake("server").index()
    val name = varchar("name", 255)
    val category = varchar("category", 255).default("Text Channels")
    val position = integer("position").default(0)
    val type = enumerationByName("type", 16, ChannelInfo.Type::class).default(ChannelInfo.Type.TEXT)
    val deletedAt = datetime("deletedAt").nullable()
}

object Users : Table() {
    val id = snowflake("id").primaryKey()
    val name = varchar("name", 255)
    val discriminator = varchar("discriminator", 4)
    val bot = bool("bot").default(false)
}

object UserNicknames : Table() {
    val server = snowflake("server").primaryKey(0)
    val user = snowflake("user").primaryKey(1)
    val timestamp = datetime("timestamp").primaryKey(2)
    val nickname = varchar("name", 255).nullable()
}

object Roles : Table() {
    val id = snowflake("id").primaryKey()
    val server = snowflake("server").index()
    val name = varchar("name", 64)
    val permissions = long("permissions")
    val color = varchar("color", 6).default("FFFFFF")
    val position = integer("position").default(0)
    val deletedAt = datetime("deletedAt").nullable()
}

object UserRoles : Table() {
    val server = snowflake("server").primaryKey(0)
    val user = snowflake("user").primaryKey(1)
    val role = snowflake("role").primaryKey(2)
}

object PermissionOverrides : Table() {
    val channel = snowflake("channel").primaryKey(0)
    val type = enumerationByName("type", 16, ChannelInfo.PermissionOverride.Type::class).primaryKey(1)
    val target = snowflake("target").primaryKey(2)
    val allowed = long("allowed")
    val denied = long("denied")
}

object Messages : Table() {
    val id = snowflake("id").primaryKey()
    val server = snowflake("server").index()
    val channel = snowflake("channel").index()
    val user = snowflake("user")
    val createdAt = datetime("createdAt").nullable()
    val deletedAt = datetime("deletedAt").nullable()
}

object MessageVersions : Table() {
    val message = snowflake("message").primaryKey(0)
    val timestamp = datetime("timestamp").primaryKey(1)
    val content = text("content")
}

object MessageEmbeds : IntIdTable() {
    val message = snowflake("message").index()
    val type = enumerationByName("type", 16, Embed.Type::class)
    val title = varchar("title", 256).nullable()
    val description = varchar("description", 2048).nullable()
    val url = varchar("url", 2048).nullable()
    val color = varchar("color", 6).nullable()
    val timestamp = datetime("timestamp").nullable()

    val footerText = varchar("footerText", 2048).nullable()
    val footerIconUrl = varchar("footerIconUrl", 2048).nullable()
    val footerIconProxyUrl = varchar("footerIconProxyUrl", 2048).nullable()

    val imageUrl = varchar("imageUrl", 2048).nullable()
    val imageProxyUrl = varchar("imageProxyUrl", 2048).nullable()
    val imageWidth = integer("imageWidth").nullable()
    val imageHeight = integer("imageHeight").nullable()

    val thumbnailUrl = varchar("thumbnailUrl", 2048).nullable()
    val thumbnailProxyUrl = varchar("thumbnailProxyUrl", 2048).nullable()
    val thumbnailWidth = integer("thumbnailWidth").nullable()
    val thumbnailHeight = integer("thumbnailHeight").nullable()

    val videoUrl = varchar("videoUrl", 2048).nullable()
    val videoProxyUrl = varchar("videoProxyUrl", 2048).nullable()
    val videoWidth = integer("videoWidth").nullable()
    val videoHeight = integer("videoHeight").nullable()

    val providerName = varchar("providerName", 2048).nullable()
    val providerUrl = varchar("providerUrl", 2048).nullable()

    val authorName = varchar("authorName", 256).nullable()
    val authorUrl = varchar("authorUrl", 2048).nullable()
    val authorIconUrl = varchar("authorIconUrl", 2048).nullable()
    val authorIconProxyUrl = varchar("authorIconProxyUrl", 2048).nullable()
}

object MessageEmbedFields : Table() {
    val embed = reference("embed", MessageEmbeds, onDelete = ReferenceOption.CASCADE).primaryKey(1)
    val position = integer("position").primaryKey(2)
    val name = varchar("name", 256)
    val value = varchar("value", 1024)
    val inline = bool("inline")
}

object MessageAttachments : Table() {
    val message = snowflake("message").index()
    val name = varchar("name", 256)
    val url = varchar("url", 2048)
    val proxyUrl = varchar("proxyUrl", 2048)
    val width = integer("width").nullable()
    val height = integer("height").nullable()
    val spoiler = bool("spoiler").default(false)
}

object MessageReactions : Table() {
    val message = snowflake("message").primaryKey(0)
    val user = snowflake("user").primaryKey(1)
    val type = enumerationByName("type", 16, NewReaction.Type::class).primaryKey(2)
    val emoji = varchar("emoji", 32).primaryKey(3)
    val emojiId = snowflake("emojiId").primaryKey(4).default("0")
    val emojiAnimated = bool("emojiAnimated")
    val createdAt = datetime("createdAt")
    val deletedAt = datetime("deletedAt").nullable()
}
