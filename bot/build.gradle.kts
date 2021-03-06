plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow") version "5.0.0"
}

version = "1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.discord4j:discord4j-core:3.1.3")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.13.0")
    implementation("org.jetbrains.exposed:exposed-core:0.19.3")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.19.3")
    implementation("org.jetbrains.exposed:exposed-java-time:0.19.3")
    implementation("org.postgresql:postgresql:42.2.5")
    implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.5.1")
    implementation("commons-cli:commons-cli:1.4")
}

application {
    mainClassName = "com.seventeenthshard.harmony.bot.HarmonyBot"
}

tasks.withType<Jar> {
    archiveFileName.set("harmony-bot.jar")
    manifest {
        attributes(
            mapOf(
                "Main-Class" to application.mainClassName
            )
        )
    }
}
