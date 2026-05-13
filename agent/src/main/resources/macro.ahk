; openBSS AutoHotKey Recovery Script
; Place this file on each Windows PC and set AHK_SCRIPT_PATH in the agent config.
; Adjust paths and window titles to match your macro and RDP setup.

#Requires AutoHotkey v2.0
#SingleInstance Force

arg := A_Args[1]

if (arg == "restart") {
    restartMacro()
} else if (arg == "restart_rdp") {
    restartRdp()
} else {
    MsgBox("Unknown argument: " . arg)
}

restartMacro() {
    ; Example: Restart NatroMacro
    ; Close existing macro
    ProcessClose("NatroMacro.exe")
    Sleep(2000)
    ; Start macro
    Run("C:\Users\YourUser\NatroMacro\NatroMacro.exe")
    Sleep(5000)
    ; Example: click start button inside macro
    ; WinActivate("NatroMacro")
    ; Click(100, 100)
}

restartRdp() {
    ; Example: Restart Remote Desktop session
    Run("C:\Windows\System32\mstsc.exe /v:your-remote-pc")
    Sleep(3000)
}
