plugins {
    kotlin("multiplatform") version "2.1.0" apply false
    kotlin("jvm") version "2.1.0" apply false
    id("org.jetbrains.compose") version "1.7.3" apply false
    kotlin("plugin.serialization") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
}

allprojects {
    group = "com.cursedhive"
    version = "1.0-SNAPSHOT"

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23
        }
    }

    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}
