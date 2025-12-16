package moe.shizuku.manager.home

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.R
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import moe.shizuku.manager.databinding.HomeAutomationBinding
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.receiver.ManualStartReceiver
import moe.shizuku.manager.receiver.ManualStopReceiver
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

            val startLabel = context.getString(R.string.action_start)
            val stopLabel = context.getString(R.string.action_stop)
            val actionLabel = context.getString(R.string.home_automation_dialog_label_action)
            val packageLabel = context.getString(R.string.home_automation_dialog_label_package)
            val classLabel = context.getString(R.string.home_automation_dialog_label_class)
            val targetLabel = context.getString(R.string.home_automation_dialog_label_target)

            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.home_automation_button_view_intents)
                .setMessage(HtmlCompat.fromHtml("""
                    <h4>$startLabel</h4>
                    <b>$actionLabel:</b> ${BuildConfig.APPLICATION_ID}.START<br>
                    <b>$packageLabel:</b> ${context.packageName}<br>
                    <b>$classLabel:</b> ${ManualStartReceiver::class.java.name}<br>
                    <b>$targetLabel:</b> Broadcast Receiver<br><br>

                    <h4>$stopLabel</h4>
                    <b>$actionLabel:</b> ${BuildConfig.APPLICATION_ID}.STOP<br>
                    <b>$packageLabel:</b> ${context.packageName}<br>
                    <b>$classLabel:</b> ${ManualStopReceiver::class.java.name}<br>
                    <b>$targetLabel:</b> Broadcast Receiver<br>
                """.trimIndent()))
                .setPositiveButton(android.R.string.ok, null)
                .show()
                .getWindow()!!
                .getDecorView()
                .findViewById<TextView>(android.R.id.message)
                .setTextIsSelectable(true)
        }
        binding.text1.movementMethod = LinkMovementMethod.getInstance()
        binding.text1.text = context.getString(R.string.home_automation_description, "adb tcpip 5555")
            .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
    }
}
