package com.openbss.models

import kotlinx.serialization.Serializable

@Serializable
data class PcStatus(
    val id: String,
    val name: String,
    val ip: String,
    val mac: String,
    val online: Boolean,
    val cpu: Double? = null,
    val memory: Double? = null,
    val temperature: Double? = null,
    val macroRunning: Boolean = false,
    val lastSeen: Long? = null,
    val lastMacroReport: String? = null
)

@Serializable
data class CommandMessage(
    val type: CommandType,
    val payload: String = ""
)

@Serializable
enum class CommandType {
    WAKE,
    RESTART_MACRO,
    RESTART_RDP,
    SHUTDOWN,
    PING
}

@Serializable
data class MacroReport(
    val pcId: String,
    val macroName: String,
    val status: String,
    val timestamp: Long
)
