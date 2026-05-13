package com.openbss.server

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.openbss.models.CommandMessage
import com.openbss.models.CommandType
import com.openbss.models.MacroReport
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.websocket.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

fun main() {
    embeddedServer(Netty, port = 8080) {
        module()
    }.start(wait = true)
}

val json = Json { ignoreUnknownKeys = true }
val pcStore = ConcurrentHashMap<String, PcInfo>()
val macroReports = Collections.synchronizedList(mutableListOf<MacroReport>())
val wsSessions = Collections.synchronizedSet<WebSocketServerSession>(LinkedHashSet())

lateinit var mqttClient: Mqtt5AsyncClient

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(CORS) {
        anyHost()
    }
    install(WebSockets)

    mqttClient = Mqtt5Client.builder()
        .serverHost("localhost")
        .serverPort(1883)
        .buildAsync()

    mqttClient.connect().get(5, TimeUnit.SECONDS)
    mqttClient.subscribeWith().topicFilter("openbss/reports/+").callback(::handleReport).send().get(5, TimeUnit.SECONDS)

    val discordBot = DiscordBot(System.getenv("DISCORD_BOT_TOKEN") ?: "")
    if (System.getenv("DISCORD_BOT_TOKEN") != null) {
        discordBot.start()
    }

    routing {
        get("/api/pcs") {
            call.respond(pcStore.values.toList())
        }
        get("/api/pcs/{id}") {
            val id = call.parameters["id"] ?: return@get call.respondText("Missing id")
            val pc = pcStore[id] ?: return@get call.respondText("Not found", status = io.ktor.http.HttpStatusCode.NotFound)
            call.respond(pc)
        }
        post("/api/pcs/{id}/wake") {
            val id = call.parameters["id"] ?: return@post call.respondText("Missing id")
            val pc = pcStore[id] ?: return@post call.respondText("Not found", status = io.ktor.http.HttpStatusCode.NotFound)
            wakeOnLan(pc.mac, pc.ip)
            call.respond(mapOf("status" to "wake_sent"))
        }
        post("/api/pcs/{id}/restart-macro") {
            val id = call.parameters["id"] ?: return@post call.respondText("Missing id")
            sendCommand(id, CommandType.RESTART_MACRO)
            call.respond(mapOf("status" to "command_sent"))
        }
        post("/api/pcs/{id}/restart-rdp") {
            val id = call.parameters["id"] ?: return@post call.respondText("Missing id")
            sendCommand(id, CommandType.RESTART_RDP)
            call.respond(mapOf("status" to "command_sent"))
        }
        post("/api/pcs/{id}/shutdown") {
            val id = call.parameters["id"] ?: return@post call.respondText("Missing id")
            sendCommand(id, CommandType.SHUTDOWN)
            call.respond(mapOf("status" to "command_sent"))
        }
        webSocket("/api/ws") {
            wsSessions.add(this)
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        if (text == "ping") {
                            send(Frame.Text("pong"))
                        }
                    }
                }
            } finally {
                wsSessions.remove(this)
            }
        }
    }

    Thread {
        while (true) {
            Thread.sleep(30_000)
            pcStore.values.forEach { pc ->
                runBlocking {
                    sendCommand(pc.id, CommandType.PING)
                }
            }
        }
    }.start()
}

fun handleReport(publish: Mqtt5Publish) {
    val payload = publish.payloadAsBytes.decodeToString()
    val topic = publish.topic.toString()
    val pcId = topic.split("/").last()
    val report = json.decodeFromString<MacroReport>(payload)
    macroReports.add(report)
    val pc = pcStore.getOrPut(pcId) { PcInfo(id = pcId, name = "PC-$pcId", ip = "", mac = "") }
    pcStore[pcId] = pc.copy(
        lastSeen = System.currentTimeMillis(),
        lastMacroReport = report.status,
        macroRunning = report.status.lowercase() == "running"
    )
    broadcastUpdate(pcStore[pcId]!!)
}

fun sendCommand(pcId: String, type: CommandType) {
    val msg = CommandMessage(type = type)
    mqttClient.publishWith()
        .topic("openbss/commands/$pcId")
        .payload(json.encodeToString(msg).encodeToByteArray())
        .send()
}

fun wakeOnLan(macAddress: String, ipAddress: String?) {
    val macBytes = macAddress.split("[:-]".toRegex())
        .filter { it.isNotEmpty() }
        .map { it.toInt(16).toByte() }
        .toByteArray()
    val bytes = ByteArray(6 + 16 * macBytes.size)
    repeat(6) { bytes[it] = 0xFF.toByte() }
    repeat(16) { i -> System.arraycopy(macBytes, 0, bytes, 6 + i * macBytes.size, macBytes.size) }

    DatagramSocket().use { socket ->
        val address = InetAddress.getByName(ipAddress ?: "255.255.255.255")
        val packet = DatagramPacket(bytes, bytes.size, address, 9)
        socket.send(packet)
    }
}

fun broadcastUpdate(pc: PcInfo) {
    val msg = json.encodeToString(pc)
    runBlocking {
        wsSessions.forEach { session ->
            try {
                session.send(Frame.Text(msg))
            } catch (_: Exception) {
            }
        }
    }
}

@kotlinx.serialization.Serializable
data class PcInfo(
    val id: String,
    val name: String,
    val ip: String,
    val mac: String,
    val online: Boolean = false,
    val cpu: Double? = null,
    val memory: Double? = null,
    val temperature: Double? = null,
    val macroRunning: Boolean = false,
    val lastSeen: Long? = null,
    val lastMacroReport: String? = null
)
