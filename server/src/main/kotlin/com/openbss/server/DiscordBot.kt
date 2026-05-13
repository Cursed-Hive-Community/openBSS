package com.openbss.server

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class DiscordBot(private val token: String) : ListenerAdapter() {
    private lateinit var jda: JDA

    fun start() {
        jda = JDABuilder.createDefault(token)
            .setActivity(Activity.watching("macro reports"))
            .addEventListeners(this)
            .build()
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        val content = event.message.contentRaw
        val reportChannelIds = System.getenv("DISCORD_REPORT_CHANNELS")?.split(",") ?: emptyList()
        if (event.channel.id in reportChannelIds) {
            val report = com.openbss.models.MacroReport(
                pcId = event.author.id,
                macroName = "discord_macro",
                status = content,
                timestamp = System.currentTimeMillis()
            )
            macroReports.add(report)
            val pc = pcStore.getOrPut(report.pcId) {
                PcInfo(
                    id = report.pcId,
                    name = event.author.effectiveName,
                    ip = "",
                    mac = ""
                )
            }
            pcStore[report.pcId] = pc.copy(
                lastSeen = report.timestamp,
                lastMacroReport = content,
                macroRunning = !content.lowercase().contains("error") && !content.lowercase().contains("stopped")
            )
        }
    }
}
