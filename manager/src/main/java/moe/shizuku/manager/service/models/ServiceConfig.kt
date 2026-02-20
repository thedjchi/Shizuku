package moe.shizuku.manager.service.models

import moe.shizuku.manager.core.models.preferences.StartMode

data class ServiceConfig(
    val startMode: StartMode,
    val startOnBoot: Boolean,
    val tcpMode: TcpMode,
    val autoDebuggingOff: Boolean
)

data class TcpMode(
    val enabled: Boolean,
    val port: Int
)