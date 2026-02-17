package moe.shizuku.manager.home

import android.annotation.SuppressLint
import android.app.Application
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.*
import moe.shizuku.manager.databinding.AdbPairDialogBinding
import moe.shizuku.manager.utils.SettingsHelper
import moe.shizuku.manager.utils.SettingsPage
import java.net.ConnectException

@RequiresApi(VERSION_CODES.R)
class AdbPairDialogFragment : DialogFragment() {
    private lateinit var binding: AdbPairDialogBinding

    private val viewModel: ViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        binding = AdbPairDialogBinding.inflate(LayoutInflater.from(context))

        val builder =
            MaterialAlertDialogBuilder(context).apply {
                setTitle(R.string.pairing_searching)
                setView(binding.root)
                setNegativeButton(android.R.string.cancel, null)
                setPositiveButton(android.R.string.ok, null)
                setNeutralButton(R.string.developer_options, null)
            }
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnShowListener { onDialogShow(dialog) }
        return dialog
    }

    private fun onDialogShow(dialog: AlertDialog) {
        binding.text1.text =
            buildString {
                append(getString(R.string.pairing_steps_intro))
                append("\n -")
                append(getString(R.string.pairing_tutorial_1))
                append("\n -")
                append(getString(R.string.pairing_tutorial_2))
                append("\n -")
                append(getString(R.string.pairing_tutorial_3))
            }

        binding.pairingCode.editText!!.doAfterTextChanged {
            binding.pairingCode.error = null
        }

        binding.pairingCode.error = null

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isVisible = false

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            SettingsHelper.launchOrHighlightWirelessDebugging(it.context)
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val context = it.context
            val port =
                try {
                    binding.port.editText!!
                        .text
                        .toString()
                        .toInt()
                } catch (e: Exception) {
                    -1
                }

            val password =
                binding.pairingCode.editText!!
                    .text
                    .toString()

            viewModel.run(port, password)
        }

        viewModel.port.observe(this) {
            if (it == -1) {
                dialog.setTitle(R.string.pairing_searching)
                binding.text1.isVisible = true
                binding.pairingCode.isVisible = false
                binding.port.editText!!.setText(it.toString())
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isVisible = false
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isVisible = true
            } else {
                dialog.setTitle(R.string.pairing_service_found)
                binding.text1.isVisible = false
                binding.pairingCode.isVisible = true
                binding.port.editText!!.setText(it.toString())
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isVisible = true
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isVisible = false
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val context = requireContext()
        val inMultiScreenOrDisplay = (
            requireActivity().isInMultiWindowMode ||
                (
                    requireActivity()
                        .window
                        ?.decorView
                        ?.display
                        ?.displayId ?: -1
                ) > 0
        )

        binding.text1.isVisible = inMultiScreenOrDisplay
        binding.text2.isVisible = !inMultiScreenOrDisplay

        if (inMultiScreenOrDisplay) {
            dialog?.setTitle(R.string.pairing_searching)
        } else {
            dialog?.setTitle(R.string.pair)
        }

        viewModel.result.observe(this) {
            if (it == null) {
                dismissAllowingStateLoss()
            } else {
                when (it) {
                    is ConnectException -> {
                        binding.port.error = context.getString(R.string.start_error_connection)
                    }

                    is AdbInvalidPairingCodeException -> {
                        binding.pairingCode.error = context.getString(R.string.pairing_error_invalid_code)
                    }

                    is AdbKeyException -> {
                        Toast
                            .makeText(context, context.getString(R.string.adb_error_key_store), Toast.LENGTH_LONG)
                            .apply { setGravity(Gravity.CENTER, 0, 0) }
                            .show()
                    }
                }
            }
        }
    }

    fun show(fragmentManager: FragmentManager) {
        if (fragmentManager.isStateSaved) return
        show(fragmentManager, javaClass.simpleName)
    }

    override fun getDialog(): AlertDialog? = super.getDialog() as AlertDialog?
}

@SuppressLint("NewApi")
class ViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContext = getApplication<Application>().applicationContext

    private val _result = MutableLiveData<Throwable?>()
    val result = _result as LiveData<Throwable?>

    private val _port = MutableLiveData<Int>()
    val port = _port as LiveData<Int>

    private val adbMdns: AdbMdns =
        AdbMdns(appContext, AdbMdns.TLS_PAIRING) {
            _port.postValue(it)
        }

    init {
        adbMdns.start()
    }

    fun run(
        port: Int,
        password: String,
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            val host = "127.0.0.1"

            val key =
                try {
                    AdbKey(PreferenceAdbKeyStore(ShizukuSettings.getPreferences()), "shizuku")
                } catch (e: Throwable) {
                    e.printStackTrace()
                    _result.postValue(AdbKeyException(e))
                    return@launch
                }

            AdbPairingClient(host, port, password, key)
                .runCatching {
                    start()
                }.onFailure {
                    _result.postValue(it)
                    it.printStackTrace()
                }.onSuccess {
                    if (it) {
                        _result.postValue(null)
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        adbMdns.stop()
    }
}
