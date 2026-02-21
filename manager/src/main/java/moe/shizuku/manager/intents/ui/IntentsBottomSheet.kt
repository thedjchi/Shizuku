package moe.shizuku.manager.intents.ui

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.core.extensions.*
import moe.shizuku.manager.databinding.IntentsBottomSheetBinding
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.toHtml
import rikka.html.text.HtmlCompat

class IntentsBottomSheet(
    private val context: Context,
) {
    private data class Field(
            val layout: TextInputLayout,
            val input: TextInputEditText,
            val initText: String,
        )

    fun show() {
        val authToken = ShizukuSettings.getAuthToken()

        val sheetBinding =
            IntentsBottomSheetBinding.inflate(
                LayoutInflater.from(context),
            )

        sheetBinding.apply {
            val action = getIntentAction(buttonGroup.checkedButtonId)
            val fields =
                listOf(
                    Field(actionLayout, actionEditText, action),
                    Field(packageLayout, packageEditText, context.packageName),
                    Field(targetLayout, targetEditText, "Broadcast Receiver"),
                    Field(extraLayout, extraEditText, authToken),
                )

            fields.forEach { (layout, input, initText) ->
                input.setText(initText)
                input.setKeyListener(null)

                layout.setEndIconOnClickListener { v ->
                    val token = input.text?.toString().orEmpty()
                    v.context.copyToClipboard(token)
                }
            }

            buttonGroup.addOnButtonCheckedListener { _, buttonId, isChecked ->
                if (isChecked) {
                    val action = getIntentAction(buttonId)
                    actionEditText.setText(action)
                }
            }
            extraLayout.setStartIconOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.intents_token_regenerate)
                    .setMessage(R.string.intents_token_regenerate_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, { _, _ ->
                        val authToken = ShizukuSettings.generateAuthToken()
                        extraEditText.setText(authToken)
                    })
                    .show()
            }
        }

        BottomSheetDialog(context).apply {
            setContentView(sheetBinding.root)
            show()
        }
    }

    // if (
    //     Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
    //     !EnvironmentUtils.isTelevision() &&
    //     !EnvironmentUtils.isRooted()
    // ) {
    //     binding.text2.apply {
    //         visibility = View.VISIBLE
    //         text = context.getString(R.string.intents_device_restriction, "adb tcpip 5555")
    //             .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
    //     }
    // }

    private fun getIntentAction(buttonId: Int): String =
        when (buttonId) {
            R.id.buttonStart -> "${BuildConfig.APPLICATION_ID}.START"
            R.id.buttonStop -> "${BuildConfig.APPLICATION_ID}.STOP"
            else -> ""
        }
}
