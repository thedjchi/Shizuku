package moe.shizuku.manager.adb

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File
import java.io.PrintStream
import java.util.concurrent.TimeUnit

class AdbTcpipClient(private val context: Context) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: AdbTcpipClient? = null
        fun getInstance(context: Context): AdbTcpipClient = instance ?: synchronized(this) {
            instance ?: AdbTcpipClient(context).also { instance = it }
        }
    }

    private val adbPath = "${context.applicationInfo.nativeLibraryDir}/libadbtcpip.so"

    private val _running = MutableLiveData(false)
    val running: LiveData<Boolean> = _running

    fun getDevices(): List<String> {
        val devicesProcess = adb(listOf("devices"))
        devicesProcess.waitFor()

        val linesRaw = BufferedReader(devicesProcess.inputStream.reader()).readLines()

        val deviceLines = linesRaw.filterNot { it ->
            it.contains("List of devices attached")
        }

        var deviceNames = deviceLines.map { it ->
            it.split("\t").first()
        }

        deviceNames = deviceNames.filterNot { it ->
            it.isEmpty()
        }

        return deviceNames
    }

    fun start(port: Int): String {
        if (_running.value == true)
            return "Local adb is already running"

        val output = StringBuilder()

        var retries = 3
        while (retries >= 0) {
            val connection = adb(listOf("connect", "localhost:$port"))
            connection.waitFor()
            val connectionStatus = connection.inputStream.bufferedReader().readText()
            if ("connected to localhost" in connectionStatus) {
                output.append(connectionStatus).append('\n')
                connection.destroyForcibly()
                break
            } else {
                if (retries == 0) {
                    throw RuntimeException("Failed to connect to localhost:$port after $retries attempts: $connectionStatus")
                } 
                retries--
                Thread.sleep(1000)
            }
        }

        val deviceList = getDevices()
        for (device in deviceList) {
            output.append(device).append('\n')
        }

        var argList = listOf("tcpip", "5555")

        if (deviceList.size > 1) {
            val localDevices = deviceList.filter { it ->
                it.contains("localhost")
            }

            if (localDevices.isNotEmpty()) {
                val serialId = localDevices.first()
                argList = listOf("-s", serialId, "tcpip", "5555")
            } else {
                val nonEmulators = deviceList.filterNot { it ->
                    it.contains("emulator")
                }

                if (nonEmulators.isNotEmpty()) {
                    val serialId = nonEmulators.first()
                    argList = listOf("-s", serialId, "tcpip", "5555")
                } else {
                    val serialId = deviceList.first()
                    argList = listOf("-s", serialId, "tcpip", "5555")
                }
            }
        }

        val tcpip = adb(argList)
        tcpip.waitFor()
        val tcpMsg = tcpip.inputStream.bufferedReader().readText()
        output.append(tcpMsg).append('\n')

        val kill = adb(listOf("kill-server"))
        kill.waitFor()
        output.append(kill.inputStream.bufferedReader().readText()).append('\n')
        kill.destroyForcibly()

        if (tcpMsg.contains("restarting in TCP mode port: 5555")) {
            _running.postValue(true)
        } else {
            throw RuntimeException("Failed to start ADB in TCP mode: $tcpMsg")
        }

        return output.toString()
    }

    fun pair(port: Int, pairingCode: String): Boolean {
        val pairShell = adb(listOf("pair", "localhost:$port"))

        PrintStream(pairShell.outputStream).apply {
            println(pairingCode)
            flush()
        }

        pairShell.waitFor(10, TimeUnit.SECONDS)
        pairShell.destroyForcibly().waitFor()

        val killShell = adb(listOf("kill-server"))
        killShell.waitFor(3, TimeUnit.SECONDS)
        killShell.destroyForcibly()

        return pairShell.exitValue() == 0
    }

    private fun adb(command: List<String>): Process {
        val commandList = command.toMutableList().also {
            it.add(0, adbPath)
        }
        return shell(commandList)
    }

    private fun shell(command: List<String>): Process {
        val processBuilder = ProcessBuilder(command)
            .directory(context.filesDir)
            .apply {
                redirectErrorStream(true)
                environment().apply {
                    put("HOME", context.filesDir.path)
                    put("TMPDIR", context.cacheDir.path)
                }
            }

        return processBuilder.start()!!
    }
}