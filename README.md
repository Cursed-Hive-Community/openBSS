# OpenBSS

> Open-source monitoring & auto-recovery system for macro PCs — built around a Raspberry Pi

> ⚠️ **This project is currently under development.** Features and documentation are a work in progress. Feel free to open an issue or contribute!


OpenBSS is a self-hosted monitoring suite designed for managing one or more macro PCs. It automatically detects crashes (both system-level and macro-level), recovers them without human intervention, and gives you full visibility through a Grafana dashboard and a cross-platform app — all orchestrated by a single Raspberry Pi on your network.

---

## Features

- **Auto-recovery** — Detects PC crashes via Prometheus + Node Exporter. Automatically powers the machine back on with Wake On LAN, then triggers the AutoHotKey script to restart RDPs and macros.
- **Macro health monitoring** — A Discord bot on the Raspberry Pi reads macro reports in real time and flags when something goes wrong.
- **System dashboard** — CPU, RAM, and temperature monitoring across all PCs via Grafana, all visible from one place.
- **Cross-platform control app** — Built with Kotlin Compose Multiplatform: Android, iOS, Desktop, and Web. Monitor and control every PC from anywhere.
- **Lightweight agent** — A small app running on each Windows PC that listens for commands from the Pi via MQTT and executes recovery scripts locally.
- **Google OAuth2** — Secure, simple authentication on the web and mobile apps.
- **Works with any macro** — Compatible with NatroMacro, RevolutionMacro, and others.

---

## Architecture

```
┌─────────────────────────────────────────────┐
│              Windows PC (x N)               │
│                                             │
│  - Macro (NatroMacro / RevolutionMacro)     │
│      → sends reports to Discord             │
│  - Node Exporter                            │
│      → pushes metrics to Prometheus         │
│  - MacroWatch Agent                         │
│      → listens for commands via MQTT        │
│      → runs AutoHotKey recovery script      │
└────────────────┬────────────────────────────┘
                 │  Metrics (HTTP)
                 │  Commands (MQTT)
                 ▼
┌─────────────────────────────────────────────┐
│         Raspberry Pi (NixOS)                │
│                                             │
│  - Mosquitto (MQTT broker)                  │
│  - Prometheus (scrapes Node Exporters)      │
│  - Grafana (system dashboard)               │
│  - Discord Bot (monitors macro reports)     │
│  - Recovery Logic                           │
│      → crash detected → WOL → MQTT cmd      │
│  - MacroWatch Server (API + Web App)        │
└────────────────┬────────────────────────────┘
                 │  HTTPS / WebSocket
                 ▼
┌─────────────────────────────────────────────┐
│     openBSS App                             │
│                                             │
│  - Web (responsive)                         │
│  - Android / iOS / Desktop (KMP)            │
│  - Google OAuth2                            │
│  - Dashboard: PC status, macro state,       │
│    manual restart, Wake On LAN              │
└─────────────────────────────────────────────┘
```

---

## Stack

| Composant | Technologie |
|---|---|
| Raspberry Pi OS | NixOS |
| MQTT Broker | to be determined |
| Dashboard | Prometheus + Grafana + Node Exporter |
| Discord Bot | to be determined |
| Agent Windows | Kotlin |
| App cross-platform | Kotlin Compose Multiplatform |
| Auth | Google OAuth2 & Github 0Auth2 |
| Wake On LAN | to be determined |

---

## Getting Started

*Work in progress...*

---

## License

This project is under **GPL-3.0** license. See the [LICENSE](LICENSE) for details.
