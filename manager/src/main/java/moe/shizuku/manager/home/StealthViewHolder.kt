package moe.shizuku.manager.home

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.R
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import moe.shizuku.manager.databinding.HomeStealthBinding
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.stealth.StealthModeHelper
import rikka.html.text.HtmlCompat
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator

class StealthViewHolder(binding: HomeStealthBinding, root: View) : BaseViewHolder<Any?>(root) {

    companion object {
        val CREATOR = Creator<Any> { inflater: LayoutInflater, parent: ViewGroup? ->
            val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
            val inner = HomeStealthBinding.inflate(inflater, outer.root, true)
            StealthViewHolder(inner, outer.root)
        }
    }

    init {
        root.setOnClickListener { v: View ->
            StealthModeHelper.listAllPatches(v.context)
            // v.context.startActivity(Intent(v.context, StealthTutorialActivity::class.java))
        }
    }
}
