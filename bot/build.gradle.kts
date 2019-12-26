plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":events"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.13.0")
    implementation("com.discord4j:discord4j-core:3.0.11")
    implementation("org.apache.kafka:kafka-clients:2.4.0")
    implementation("org.apache.avro:avro:1.9.1")
    implementation("com.sksamuel.avro4k:avro4k-core:0.20.0")
    implementation("io.confluent:kafka-avro-serializer:5.3.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.13.0")
}
