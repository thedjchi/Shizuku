package moe.shizuku.manager.home

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.databinding.HomeAutomationBinding
import moe.shizuku.manager.databinding.HomeAutomationBottomSheetBinding
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.utils.EnvironmentUtils
import rikka.core.util.ClipboardUtils
import rikka.html.text.HtmlCompat
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator

class AutomationViewHolder(
    binding: HomeAutomationBinding,
    root: View,
) : BaseViewHolder<Any?>(root) {
    companion object {
        val CREATOR =
            Creator<Any> { inflater: LayoutInflater, parent: ViewGroup? ->
                val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
                val inner = HomeAutomationBinding.inflate(inflater, outer.root, true)
                AutomationViewHolder(inner, outer.root)
            }
    }

    private data class Field(
        val layout: TextInputLayout,
        val input: TextInputEditText,
        val initText: String
    )

    init {
        binding.button1.setOnClickListener { v ->
            val context = v.context
            val authToken = ShizukuSettings.getAuthToken()

            val sheetBinding = HomeAutomationBottomSheetBinding.inflate(
                LayoutInflater.from(context)
            )

            sheetBinding.apply {
                val action = getIntentAction(buttonGroup.checkedButtonId)
                val fields = listOf(
                    Field(actionLayout, actionEditText, action),
                    Field(packageLayout, packageEditText, context.packageName),
                    Field(targetLayout, targetEditText, "Broadcast Receiver"),
                    Field(extrasLayout, extrasEditText, authToken)
                )

                fields.forEach { (layout, input, initText) ->
                    input.setText(initText)
                    input.setKeyListener(null)

                    layout.setEndIconOnClickListener { v ->
                        val context = v.context
                        if (
                            ClipboardUtils.put(context, input.text) &&
                            Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2
                        ) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_copied_to_clipboard),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }

                buttonGroup.addOnButtonCheckedListener { _, buttonId, isChecked ->
                    if (isChecked) {
                        val action = getIntentAction(buttonId)
                        actionEditText.setText(action)
                    }
                }
                extrasLayout.setStartIconOnClickListener {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.home_automation_regenerate_token)
                        .setMessage(R.string.home_automation_regenerate_token_message)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, { _, _ -> 
                            val authToken = ShizukuSettings.generateAuthToken()
                            extrasEditText.setText(authToken)
                        })
                        .show()
                }
            }

            BottomSheetDialog(context).apply {
                setContentView(sheetBinding.root)
                show()
            }
        }

        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
            !EnvironmentUtils.isTelevision() &&
            !EnvironmentUtils.isRooted()
        ) {
            binding.text2.apply {
                visibility = View.VISIBLE
                text = context.getString(R.string.home_automation_description_device_restriction, "adb tcpip 5555")
                    .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
            }
        }
    }

    private fun getIntentAction(buttonId: Int): String =
        when (buttonId) {
            R.id.buttonStart -> "${BuildConfig.APPLICATION_ID}.START"
            R.id.buttonStop -> "${BuildConfig.APPLICATION_ID}.STOP"
            else -> ""
        }
}
