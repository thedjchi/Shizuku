package moe.shizuku.manager.starter

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import java.net.SocketException
import javax.net.ssl.SSLProtocolException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.shizuku.manager.AppConstants.EXTRA
import moe.shizuku.manager.R
import moe.shizuku.manager.adb.AdbKeyException
import moe.shizuku.manager.adb.AdbStarter
import moe.shizuku.manager.app.AppBarActivity
import moe.shizuku.manager.utils.ShizukuStateMachine
import moe.shizuku.manager.databinding.StarterActivityBinding
import rikka.lifecycle.Resource
import rikka.lifecycle.Status
import rikka.lifecycle.viewModels

private class NotRootedException: Exception()

class StarterActivity : AppBarActivity() {

    private val viewModel by viewModels {
        ViewModel(
            this,
            intent.getBooleanExtra(EXTRA_IS_ROOT, false),
            intent.getIntExtra(EXTRA_PORT, 0),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_24)

        val binding = StarterActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.output.observe(this) {
            val output = it.data!!.trim()
            if (output.endsWith("Service started, this window will be automatically closed in 3 seconds")) {
                window?.decorView?.postDelayed({
                    if (!isFinishing) finish()
                }, 3000)
            } else if (it.status == Status.ERROR) {
                var message = 0
                when (it.error) {
                    is AdbKeyException -> {
                        message = R.string.adb_error_key_store
                    }
                    is NotRootedException -> {
                        message = R.string.start_with_root_failed
                    }
                    is SocketException -> {
                        message = R.string.cannot_connect_port
                    }
                    is SSLProtocolException -> {
                        message = R.string.adb_pair_required
                    }
                }

                if (message != 0) {
                    MaterialAlertDialogBuilder(this)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
            binding.text1.text = output
        }
    }

    private var hasStarted = false

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !hasStarted) {
            hasStarted = true
            viewModel.start()
        }
    }

    companion object {

        const val EXTRA_IS_ROOT = "$EXTRA.IS_ROOT"
        const val EXTRA_PORT = "$EXTRA.PORT"
    }
}

private class ViewModel(
    private val context: Context,
    private val root: Boolean,
    private val port: Int
) : androidx.lifecycle.ViewModel() {

    private val sb = StringBuilder()
    private val _output = MutableLiveData<Resource<StringBuilder>>()

    val output = _output as LiveData<Resource<StringBuilder>>

    private val handler = CoroutineExceptionHandler { _, throwable ->
        log(error = throwable)
    }

    private var started = false

    fun start() {
        if (started) return
        started = true

        viewModelScope.launch(Dispatchers.IO + handler) {
            try {
                if (root) {
                    startRoot()
                } else AdbStarter.startAdb(context, port, { log(it) })
                Starter.waitForBinder({ log(it) })
            } finally {
                ShizukuStateMachine.update()
            }
        }
    }

    private fun log(line: String? = null, error: Throwable? = null) {
        line?.let { sb.appendLine(it) }
        error?.let { sb.appendLine().appendLine(Log.getStackTraceString(it)) }

        if (error == null)
            _output.postValue(Resource.success(sb))
        else
            _output.postValue(Resource.error(error, sb))
    }

    private fun startRoot() {
        log("Starting with root...\n")

        if (!Shell.getShell().isRoot) {
            // Try again just in case
            Shell.getCachedShell()?.close()

            if (!Shell.getShell().isRoot) {
                Shell.getCachedShell()?.close()
                throw NotRootedException()
            }
        }

        ShizukuStateMachine.set(ShizukuStateMachine.State.STARTING)
        Shell.cmd(Starter.internalCommand).to(object : CallbackList<String?>() {
            override fun onAddElement(s: String?) {
                s?.let { log(it) }
            }
        }).submit {
            if (!it.isSuccess)
                throw Exception("Failed to start with root")
        }
    }
}
