plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

version = "1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api("org.apache.logging.log4j:log4j-api:2.13.0")
    api("org.apache.kafka:kafka-clients:2.4.0")
    api("io.confluent:kafka-avro-serializer:5.3.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.13.0")
    api("org.apache.avro:avro:1.9.1")
    api("com.sksamuel.avro4k:avro4k-core:0.20.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.serialization.ImplicitReflectionSerializer"
}
