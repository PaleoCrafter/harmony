plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

version = "1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.13.0")
    implementation("org.apache.avro:avro:1.9.1")
    implementation("com.sksamuel.avro4k:avro4k-core:0.20.0")
}
