plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":events"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.apache.kafka:kafka-clients:2.4.0")
    implementation("io.confluent:kafka-avro-serializer:5.3.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.13.0")
    implementation("org.jetbrains.exposed:exposed-core:0.19.3")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.19.3")
    implementation("org.jetbrains.exposed:exposed-java-time:0.19.3")
    implementation("org.postgresql:postgresql:42.2.5")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.serialization.ImplicitReflectionSerializer"
}
