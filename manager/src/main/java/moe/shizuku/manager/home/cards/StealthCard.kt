package moe.shizuku.manager.home.cards

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import moe.shizuku.manager.R
import moe.shizuku.manager.stealth.StealthTutorialActivity

class StealthCard
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null
    ) : BaseCard(context, attrs) {

    override val cardTitle: String
        get() = context.getString(R.string.home_stealth_title)
    override val cardIcon: Int
        get() = R.drawable.ic_visibility_off_outline_24

    override fun onClick(v: View) {
        v.context.startActivity(Intent(v.context, StealthTutorialActivity::class.java))
    }
}
