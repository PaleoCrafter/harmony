package com.seventeenthshard.harmony.dbimport

import com.seventeenthshard.harmony.events.MessageDeletion
import com.seventeenthshard.harmony.events.MessageEdit
import com.seventeenthshard.harmony.events.NewMessage
import com.seventeenthshard.harmony.events.ServerInfo
import com.sksamuel.avro4k.Avro
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.subject.RecordNameStrategy
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.serializer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.util.*

class EventHandler(init: EventHandler.() -> Unit) {
    private val handlers = mutableMapOf<String, Handler<*>>()

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
                    (handlers[it.value().schema.fullName] as? Handler<Any>)?.let { handler ->
                        handler.run(it.key(), Avro.default.fromRecord(handler.deserializer, record))
                    }
                }
            }
        }
    }

    private data class Handler<T>(val deserializer: DeserializationStrategy<T>, val run: (id: String, event: T) -> Unit)
}

fun main() {
    Database.connect("jdbc:postgresql://localhost/harmony", "org.postgresql.Driver", "harmony_imports", "imp0rt5")

    transaction {
        SchemaUtils.createMissingTablesAndColumns(Servers, Channels, Users, MessageVersions)
    }

    val events = EventHandler {
        listen<ServerInfo> { id, event ->
            println("$id: $event")
        }
        listen<NewMessage> { id, event ->
            println("$id: $event")
        }
        listen<MessageEdit> { id, event ->
            println("$id: $event")
        }
        listen<MessageDeletion> { id, event ->
            println("$id: $event")
        }
    }

    events.consume(
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
            ConsumerConfig.GROUP_ID_CONFIG to "db-import",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "true",
            ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG to "1000",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
            KafkaAvroDeserializerConfig.VALUE_SUBJECT_NAME_STRATEGY to RecordNameStrategy::class.java,
            KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://localhost:8081"
        ),
        "servers", "messages"
    )
}
