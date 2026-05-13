package com.openbss.agent

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.openbss.models.CommandMessage
import com.openbss.models.CommandType
import com.openbss.models.MacroReport
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

val json = Json { prettyPrint = true }
val PC_ID = System.getenv("PC_ID") ?: System.getenv("COMPUTERNAME") ?: "unknown-pc"
val MQTT_HOST = System.getenv("MQTT_HOST") ?: "192.168.1.100"
val MQTT_PORT = System.getenv("MQTT_PORT")?.toIntOrNull() ?: 1883
val AHK_SCRIPT_PATH = System.getenv("AHK_SCRIPT_PATH") ?: "C:\\scripts\\macro.ahk"

fun main() {
    println("openBSS Agent starting for PC: $PC_ID")
    println("Connecting to MQTT at $MQTT_HOST:$MQTT_PORT")

    val client: Mqtt5AsyncClient = Mqtt5Client.builder()
        .serverHost(MQTT_HOST)
        .serverPort(MQTT_PORT)
        .identifier("agent-$PC_ID")
        .buildAsync()

    client.connect().get(5, TimeUnit.SECONDS)
    println("Connected to MQTT broker")

    client.subscribeWith()
        .topicFilter("openbss/commands/$PC_ID")
        .callback(Consumer<Mqtt5Publish> { publish ->
            val payload = publish.payloadAsBytes.decodeToString()
            val msg = json.decodeFromString<CommandMessage>(payload)
            handleCommand(msg)
        })
        .send().get(5, TimeUnit.SECONDS)

    Thread {
        while (true) {
            Thread.sleep(60_000)
            sendHeartbeat(client)
        }
    }.start()

    println("Agent is running. Waiting for commands...")
    while (true) {
        Thread.sleep(1000)
    }
}

fun handleCommand(msg: CommandMessage) {
    println("Received command: ${msg.type}")
    when (msg.type) {
        CommandType.WAKE -> println("Wake command received (no-op, agent already awake)")
        CommandType.RESTART_MACRO -> restartMacro()
        CommandType.RESTART_RDP -> restartRdp()
        CommandType.SHUTDOWN -> shutdown()
        CommandType.PING -> sendPing()
    }
}

fun restartMacro() {
    val ahkPath = "C:\\Program Files\\AutoHotkey\\v2\\AutoHotkey.exe"
    val script = File(AHK_SCRIPT_PATH)
    if (script.exists()) {
        Runtime.getRuntime().exec(arrayOf(ahkPath, AHK_SCRIPT_PATH, "restart"))
        println("Restart macro AutoHotKey script executed")
    } else {
        println("AutoHotKey script not found at $AHK_SCRIPT_PATH")
    }
    publishReport("macro_restart_triggered")
}

fun restartRdp() {
    val ahkPath = "C:\\Program Files\\AutoHotkey\\v2\\AutoHotkey.exe"
    val script = File(AHK_SCRIPT_PATH)
    if (script.exists()) {
        Runtime.getRuntime().exec(arrayOf(ahkPath, AHK_SCRIPT_PATH, "restart_rdp"))
        println("Restart RDP AutoHotKey script executed")
    } else {
        println("AutoHotKey script not found at $AHK_SCRIPT_PATH")
    }
    publishReport("rdp_restart_triggered")
}

fun shutdown() {
    Runtime.getRuntime().exec("shutdown /s /t 0")
    println("Shutdown command issued")
}

fun sendPing() {
    publishReport("alive")
}

fun sendHeartbeat(client: Mqtt5AsyncClient) {
    publishReport("heartbeat", client)
}

fun publishReport(status: String, clientOverride: Mqtt5AsyncClient? = null) {
    val report = MacroReport(
        pcId = PC_ID,
        macroName = System.getenv("MACRO_NAME") ?: "NatroMacro",
        status = status,
        timestamp = System.currentTimeMillis()
    )
    val payload = json.encodeToString(report)
    val client = clientOverride ?: Mqtt5Client.builder()
        .serverHost(MQTT_HOST)
        .serverPort(MQTT_PORT)
        .identifier("agent-$PC_ID")
        .buildAsync()

    try {
        if (clientOverride == null) client.connect().get(5, TimeUnit.SECONDS)
        client.publishWith()
            .topic("openbss/reports/$PC_ID")
            .payload(payload.encodeToByteArray())
            .send()
        println("Published report: $status")
    } catch (e: Exception) {
        println("Failed to publish report: ${e.message}")
    }
}
