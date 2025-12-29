package moe.shizuku.manager.ktx

import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import moe.shizuku.manager.utils.CustomTabsHelper
import rikka.html.text.HtmlCompat

fun CharSequence.toHtml(flags: Int = 0): Spanned {
    return HtmlCompat.fromHtml(this.toString(), flags)
}

fun CharSequence.toHtml(tagHandler: HtmlCompat.TagHandler): Spanned {
    return HtmlCompat.fromHtml(this.toString(), null, tagHandler)
}

fun CharSequence.toHtml(flags: Int, tagHandler: HtmlCompat.TagHandler): Spanned {
    return HtmlCompat.fromHtml(this.toString(), flags, null, tagHandler)
}

fun String.asLink(url: String): CharSequence {
    return SpannableString(this).apply {
        setSpan(object : ClickableSpan() {
            override fun onClick(v: View) {
                CustomTabsHelper.launchUrlOrCopy(v.context, url)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = ds.linkColor
            }
        }, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

fun TextView.applyTemplateArgs(vararg args: CharSequence) {
    val template = text as CharSequence
    text = TextUtils.expandTemplate(template, *args)
    movementMethod = LinkMovementMethod.getInstance()
}