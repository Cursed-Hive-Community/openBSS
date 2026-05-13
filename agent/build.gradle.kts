plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

repositories {
    mavenCentral()
}

val mqttVersion = "1.3.14"

dependencies {
    implementation("com.hivemq:hivemq-mqtt-client:$mqttVersion")
    implementation("io.projectreactor:reactor-core:3.6.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

application {
    mainClass.set("com.openbss.agent.MainKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_23
    targetCompatibility = JavaVersion.VERSION_23
}

kotlin {
    jvmToolchain(23)
}
