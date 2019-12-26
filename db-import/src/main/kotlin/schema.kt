package com.seventeenthshard.harmony.dbimport

import com.seventeenthshard.harmony.dbimport.MessageVersions.index
import com.seventeenthshard.harmony.dbimport.MessageVersions.primaryKey
import org.jetbrains.exposed.sql.*
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

object Servers: Table() {
    val id = snowflake("id").primaryKey()
    val name = varchar("name", 255)
    val iconUrl = varchar("iconUrl", 255).nullable()
    val active = bool("active").default(true)
}

object Channels: Table() {
    val id = snowflake("id").primaryKey()
    val server = snowflake("server").index()
    val name = varchar("name", 255)
}

object Users: Table() {
    val id = snowflake("id").primaryKey()
    val name = varchar("name", 255)
    val discriminator = varchar("discriminator", 4)
}

object Messages : Table() {
    val id = snowflake("id").primaryKey()
    val server = snowflake("server").index()
    val channel = snowflake("channel").index()
    val user = snowflake("user")
    val deletedAt = datetime("deletedAt").nullable()
}

object MessageVersions: Table() {
    val message = snowflake("message").primaryKey(0)
    val timestamp = datetime("timestamp").primaryKey(1)
    val content = text("content")
}
