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
    implementation("com.discord4j:discord4j-core:3.0.11")
    implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.5.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.serialization.ImplicitReflectionSerializer"
}

application {
    mainClassName = "com.seventeenthshard.harmony.search.ElasticIngest"
}

tasks.withType<Jar> {
    archiveFileName.set("harmony-elastic-ingest.jar")
    manifest {
        attributes(
            mapOf(
                "Main-Class" to application.mainClassName
            )
        )
    }
}
