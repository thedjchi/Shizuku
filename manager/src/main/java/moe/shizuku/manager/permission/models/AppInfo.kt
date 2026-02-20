package moe.shizuku.manager.permission.models

data class AppInfo(
    val appName: String,
    val packageName: String,
    val isGranted: Boolean = false,
    val requiresRoot: Boolean = false,
)