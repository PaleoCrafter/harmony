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
            name = "Spring Milestone"
            url = uri("https://repo.spring.io/milestone")
        }

        maven {
            name = "Sonatype Snapshots"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
    }
}
