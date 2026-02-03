package moe.shizuku.manager.home

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import moe.shizuku.manager.databinding.HomeAutomationBinding
import moe.shizuku.manager.databinding.HomeAutomationBottomSheetBinding
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.utils.EnvironmentUtils
import rikka.html.text.HtmlCompat
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator

class AutomationViewHolder(binding: HomeAutomationBinding, root: View) : BaseViewHolder<Any?>(root) {

    companion object {
        val CREATOR = Creator<Any> { inflater: LayoutInflater, parent: ViewGroup? ->
            val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
            val inner = HomeAutomationBinding.inflate(inflater, outer.root, true)
            AutomationViewHolder(inner, outer.root)
        }
    }

    init {
        binding.button1.setOnClickListener { v: View ->
            val context = v.context

            val authToken = ShizukuSettings.getAuthToken()

            val sheetBinding = HomeAutomationBottomSheetBinding.inflate(
                LayoutInflater.from(context)
            )

            sheetBinding.apply {
                action.apply {
                    updateActionText(buttonGroup.checkedButtonId)
                    setKeyListener(null)
                }
                packageName.apply {
                    setKeyListener(null)
                    setText(context.packageName)
                }
                target.apply {
                    setKeyListener(null)
                    setText("Broadcast Receiver")
                }
                extras.apply {
                    setKeyListener(null)
                    setText(authToken)
                }
                buttonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
                    if (isChecked) action.updateActionText(checkedId)
                }
            }
            
            val sheet = BottomSheetDialog(context)
            sheet.setContentView(sheetBinding.root)
            sheet.show()
        }
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
            !EnvironmentUtils.isTelevision() &&
            !EnvironmentUtils.isRooted()
        ) {
            binding.text2.visibility = View.VISIBLE
            binding.text2.text = context.getString(R.string.home_automation_description_2, "adb tcpip 5555")
            .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
        }
    }

    private fun TextInputEditText.updateActionText(buttonId: Int) {
        when (buttonId) {
            R.id.buttonStart -> setText("${BuildConfig.APPLICATION_ID}.START")
            R.id.buttonStop -> setText("${BuildConfig.APPLICATION_ID}.STOP")
        }
    }
}
