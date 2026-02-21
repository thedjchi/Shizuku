package moe.shizuku.manager.home.cards

import android.content.Context
import android.util.AttributeSet
import android.view.View
import moe.shizuku.manager.R
import moe.shizuku.manager.intents.ui.IntentsBottomSheet

class IntentsCard
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : BaseCard(context, attrs) {
        override val cardTitle: String
            get() = context.getString(R.string.intents)
        override val cardIcon: Int
            get() = R.drawable.ic_integration_instructions_24

        override fun onClick(v: View) {
            IntentsBottomSheet(context).show()
        }
    }
