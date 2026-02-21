package moe.shizuku.manager.core.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import moe.shizuku.manager.R

fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("text", text))
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) toast(R.string.copied_to_clipboard)
}

fun Context.toast(id: Int, long: Boolean = false) =
    toast(getString(id), long)

fun Context.toast(message: String, long: Boolean = false) {
    Toast.makeText(
        this,
        message,
        if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    ).show()
}