import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.50" apply false
    kotlin("plugin.serialization") version "1.3.50" apply false
}

version = "1.0-SNAPSHOT"

allprojects {
    group = "com.seventeenthshard"
}

subprojects {
    repositories {
        mavenCentral()
        jcenter()

        maven {
            name = "Confluent"
            url = uri("https://packages.confluent.io/maven/")
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
    }
}
