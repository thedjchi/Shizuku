package moe.shizuku.manager.home.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import moe.shizuku.manager.databinding.HomeItemContainerBinding

abstract class BaseCard
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null
    ) : FrameLayout(context, attrs), View.OnClickListener {
        private val binding =
            HomeItemContainerBinding.inflate(
                LayoutInflater.from(context),
                this,
                true,
            )

        protected abstract val cardTitle: String
        protected abstract val cardIcon: Int

        init {
            binding.root.setOnClickListener(this)
            setTitle(cardTitle)
            setIcon(cardIcon)
        }

        protected fun setTitle(text: String) {
            binding.title.text = text
        }

        protected fun setSummary(text: String) {
            if (text.isEmpty()) {
                binding.summary.visibility = View.GONE
            } else {
                binding.summary.visibility = View.VISIBLE
                binding.summary.text = text
            }
        }

        protected fun setIcon(resId: Int) {
            binding.icon.setImageResource(resId)
        }

        override fun onClick(v: View) {}
    }
