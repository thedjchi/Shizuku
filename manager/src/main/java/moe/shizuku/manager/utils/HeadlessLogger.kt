package moe.shizuku.manager.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HeadlessLogger {

    private const val TAG = "ShizukuHeadless"
    private const val LOG_FILE = "headless.log"
    private const val MAX_SIZE = 256 * 1024

    private var logDir: File? = null
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    enum class Level { INFO, WARN, ERROR }

    fun init(context: Context) {
        if (logDir != null) return
        logDir = context.getExternalFilesDir(null) ?: context.filesDir
        logDir?.mkdirs()
        logFile = logDir?.let { File(it, LOG_FILE) }
    }

    fun i(component: String, message: String) = log(Level.INFO, component, message)
    fun w(component: String, message: String) = log(Level.WARN, component, message)
    fun e(component: String, message: String, throwable: Throwable? = null) {
        val msg = if (throwable != null) {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            "$message\n${sw.toString()}"
        } else message
        log(Level.ERROR, component, msg)
    }

    @Synchronized
    private fun log(level: Level, component: String, message: String) {
        val ts = dateFormat.format(Date())
        val line = "$ts ${level.name.padEnd(5)} $component: $message"

        Log.println(level.toLogPriority(), TAG, "$component: $message")

        val file = logFile ?: return
        try {
            if (file.length() > MAX_SIZE) {
                file.renameTo(File(file.parent, LOG_FILE + ".1"))
            }
            FileWriter(file, true).use { it.appendLine(line) }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot write log file: ${e.message}")
        }
    }

    fun getLogPath(): String? = logFile?.absolutePath

    private fun Level.toLogPriority(): Int = when (this) {
        Level.INFO -> Log.INFO
        Level.WARN -> Log.WARN
        Level.ERROR -> Log.ERROR
    }
}
