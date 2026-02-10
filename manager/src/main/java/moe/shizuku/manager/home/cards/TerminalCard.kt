package moe.shizuku.manager.home.cards

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import moe.shizuku.manager.R
import moe.shizuku.manager.shell.ShellTutorialActivity

class TerminalCard
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null
    ) : BaseCard(context, attrs) {

    override val cardTitle: String
        get() = context.getString(R.string.home_terminal_title)
    override val cardIcon: Int
        get() = R.drawable.ic_terminal_24

    override fun onClick(v: View) {
        v.context.startActivity(Intent(v.context, ShellTutorialActivity::class.java))
    }
}
