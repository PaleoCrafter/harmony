@file:JvmName("ElasticIngress")

package com.seventeenthshard.harmony.search

import com.seventeenthshard.harmony.events.EventHandler
import com.seventeenthshard.harmony.events.NewMessage
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.subject.RecordNameStrategy
import org.apache.http.HttpHost
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.CloseIndexRequest
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.client.indices.PutMappingRequest
import org.elasticsearch.common.xcontent.XContentType
import org.intellij.lang.annotations.Language
import java.util.concurrent.TimeUnit

const val INDEX = "messages"

@Language("JSON")
const val MESSAGES_MAPPING = """{
    "properties": {
        "server": { "type": "keyword" },
        "channel": {
            "properties": {
                "id": { "type": "keyword" },
                "name": { "type": "keyword" }
            }
        },
        "author": {
            "properties": {
                "id": { "type": "keyword" },
                "name": { "type": "keyword" },
                "discriminator": { "type": "keyword" }
            }
        },
        "content": {
            "type": "text",
            "fields": {
                "cs": {
                    "type": "text",
                    "analyzer": "stemmed_strip_html_analyzer",
                    "term_vector": "with_positions_offsets",
                    "fields": {
                        "lowercase": {
                            "type": "text",
                            "analyzer": "stemmed_case_insensitive_analyzer",
                            "term_vector": "with_positions_offsets",
                            "fields": {
                                "exact": {
                                    "type": "text",
                                    "analyzer": "case_insensitive_analyzer",
                                    "term_vector": "with_positions_offsets"
                                }
                            }
                        },
                        "exact": {
                            "type": "text",
                            "analyzer": "strip_html_analyzer",
                            "term_vector": "with_positions_offsets"
                        }
                    }
                }
            }
        },
        "has": {
            "properties": {
                "link": { "type": "boolean" },
                "embed": { "type": "boolean" },
                "file": { "type": "boolean" },
                "video": { "type": "boolean" },
                "image": { "type": "boolean" },
                "sound": { "type": "boolean" }
            }
        },
        "timestamp": { "type": "date", "format": "basic_date_time||epoch_millis" }
    }
}"""

@Language("JSON")
const val INDEX_SETTINGS = """{
    "max_inner_result_window": 1000,
    "analysis": {
        "analyzer": {
            "strip_html_analyzer": {
                "tokenizer": "classic",
                "filter": ["remove_accents"],
                "char_filter": ["html_stripper", "normalize_quotes", "normalize_apostrophes"]
            },
            "case_insensitive_analyzer": {
                "tokenizer": "classic",
                "filter": ["lowercase", "remove_accents"],
                "char_filter": ["html_stripper", "normalize_quotes", "normalize_apostrophes"]
            },
            "stemmed_strip_html_analyzer": {
                "tokenizer": "classic",
                "filter": ["remove_accents", "possessive_stemmer", "kstem"],
                "char_filter": ["html_stripper", "normalize_quotes", "normalize_apostrophes"]
            },
            "stemmed_case_insensitive_analyzer": {
                "tokenizer": "classic",
                "filter": ["lowercase", "remove_accents", "possessive_stemmer", "kstem"],
                "char_filter": ["html_stripper", "normalize_quotes", "normalize_apostrophes"]
            },
            "signature_analyzer": {
                "tokenizer": "standard",
                "filter": [
                    "possessive_stemmer",
                    "suffix_cleanup",
                    "en_US",
                    "english_stops",
                    "dictionary_stops",
                    "revert_hyphens",
                    "remove_duplicates"
                ],
                "char_filter": [
                    "html_stripper",
                    "normalize_apostrophes",
                    "normalize_quotes",
                    "convert_hyphen_start",
                    "convert_hyphens",
                    "remove_numbers",
                    "remove_all_caps"
                ]
            }
        },
        "char_filter": {
            "html_stripper": {
                "type": "html_strip"
            },
            "convert_hyphen_start": {
                "type": "pattern_replace",
                "pattern": "(\\p{Upper}[^\\s-]+)-(?=[^-\\s]+)",
                "replacement": "$1_"
            },
            "convert_hyphens": {
                "type": "pattern_replace",
                "pattern": "(?<=_)([^\\s-_]+)-(?=[^-\\s]+)",
                "replacement": "$1_"
            },
            "remove_numbers": {
                "type": "pattern_replace",
                "pattern": "[0-9]+",
                "replacement": ""
            },
            "remove_all_caps": {
                "type": "pattern_replace",
                "pattern": "\\p{Upper}{2,}",
                "replacement": ""
            },
            "normalize_apostrophes": {
                "type": "mapping",
                "mappings": [
                    "\u0091=>\u0027",
                    "\u0092=>\u0027",
                    "\u2018=>\u0027",
                    "\u2019=>\u0027",
                    "\u201A=>\u0027",
                    "\u2039=>\u0027",
                    "\u203A=>\u0027",
                    "\uFF07=>\u0027"
                ]
            },
            "normalize_quotes": {
                "type": "mapping",
                "mappings": [
                    "\u00AB=>\u0022",
                    "\u00BB=>\u0022",
                    "\u201C=>\u0022",
                    "\u201D=>\u0022",
                    "\u201E=>\u0022"
                ]
            }
        },
        "filter": {
            "suffix_cleanup": {
                "type": "length",
                "min": 2
            },
            "possessive_stemmer": {
                "type": "stemmer",
                "name": "possessive_english"
            },
            "en_US" : {
                "type" : "hunspell",
                "locale" : "en_US",
                "dedup" : true
            },
            "english_stops": {
                "type": "stop",
                "stopwords":  "_english_"
            },
            "dictionary_stops": {
                "type": "stop",
                "stopwords_path": "hunspell/en_US/stopwords.txt",
                "ignore_case": true
            },
            "revert_hyphens": {
                "type": "pattern_replace",
                "pattern": "_",
                "replacement": "-"
            },
            "remove_accents": {
                "type": "asciifolding"
            }
        }
    }
}"""

fun main() {
    val elasticHosts = requireNotNull(System.getenv("ELASTIC_HOST")) {
        "ELASTIC_HOST env variable must be set!"
    }.split(",").map { HttpHost.create(it) }.toTypedArray()
    val elasticClient = RestHighLevelClient(
        RestClient.builder(*elasticHosts)
            .setHttpClientConfigCallback {
                it.setKeepAliveStrategy { _, _ -> TimeUnit.MINUTES.toMillis(10) }
            }
    )
    elasticClient.ensureIndex(INDEX, MESSAGES_MAPPING)

    val events = EventHandler {
        listen<NewMessage> { id, event ->
            elasticClient.index(
                IndexRequest(INDEX).,
                RequestOptions.DEFAULT
            )
        }
    }

    events.consume(
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to requireNotNull(System.getenv("BROKER_URLS")) {
                "BROKER_URLS env variable must be set!"
            },
            ConsumerConfig.GROUP_ID_CONFIG to "elastic-ingress",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "true",
            ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG to "1000",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
            KafkaAvroDeserializerConfig.VALUE_SUBJECT_NAME_STRATEGY to RecordNameStrategy::class.java,
            KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to requireNotNull(System.getenv("SCHEMA_REGISTRY_URL")) {
                "SCHEMA_REGISTRY_URL env variable must be set!"
            }
        ),
        "messages"
    )
}

private fun RestHighLevelClient.ensureIndex(index: String, mapping: String) {
    if (indices().exists(
            GetIndexRequest(index),
            RequestOptions.DEFAULT
        )
    ) {
        indices().create(
            CreateIndexRequest(index),
            RequestOptions.DEFAULT
        )

        indices().close(
            CloseIndexRequest(index),
            RequestOptions.DEFAULT
        )

        indices().putSettings(
            UpdateSettingsRequest(index).settings(INDEX_SETTINGS, XContentType.JSON),
            RequestOptions.DEFAULT
        )

        indices().putMapping(
            PutMappingRequest(index).source(mapping, XContentType.JSON),
            RequestOptions.DEFAULT
        )

        indices().open(
            OpenIndexRequest(index),
            RequestOptions.DEFAULT
        )
    }
}
