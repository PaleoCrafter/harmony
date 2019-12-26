plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

version = "1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.13.0")
    api("org.apache.avro:avro:1.9.1")
    api("com.sksamuel.avro4k:avro4k-core:0.20.0")
}
