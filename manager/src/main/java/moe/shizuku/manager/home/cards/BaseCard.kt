package moe.shizuku.manager.home.cards

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.card.MaterialCardView
import moe.shizuku.manager.databinding.HomeItemContainerBinding

abstract class BaseCard
    @JvmOverloads
    constructor(
        context: Context,
    ) : MaterialCardView(context), View.OnClickListener {
        private val binding =
            HomeItemContainerBinding.inflate(
                LayoutInflater.from(context),
                this,
                true,
            )

        protected abstract val cardTitle: String
        protected abstract val cardIcon: Int

        init {
            setOnClickListener(this)
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
