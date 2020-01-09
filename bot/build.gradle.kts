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
    implementation("com.discord4j:discord4j-core:3.0.11")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.13.0")
}

application {
    mainClassName = "com.seventeenthshard.harmony.bot.Bot"
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
