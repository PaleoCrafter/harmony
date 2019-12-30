package com.seventeenthshard.harmony.dbimport

import com.seventeenthshard.harmony.events.ChannelInfo
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.vendors.currentDialect

object SnowflakeColumnType : ColumnType() {
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

fun Table.snowflake(name: String): Column<String> = registerColumn(name, SnowflakeColumnType)

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
    val type = enumerationByName("type", 16, ChannelInfo.Type::class)
    val deletedAt = datetime("deletedAt").nullable()
}

object Users : Table() {
    val id = snowflake("id").primaryKey()
    val name = varchar("name", 255)
    val discriminator = varchar("discriminator", 4)
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
