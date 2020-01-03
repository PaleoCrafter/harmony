plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow") version "5.0.0"
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
    implementation("com.discord4j:discord4j-core:3.0.11")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.serialization.ImplicitReflectionSerializer"
}

application {
    mainClassName = "com.seventeenthshard.harmony.dbimport.DBTools"
}

tasks.withType<Jar> {
    archiveFileName.set("harmony-db-import.jar")
    manifest {
        attributes(
            mapOf(
                "Main-Class" to application.mainClassName
            )
        )
    }
}
