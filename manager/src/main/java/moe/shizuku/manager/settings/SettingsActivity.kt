package moe.shizuku.manager.settings

import android.content.res.Resources
import android.os.Bundle
import androidx.fragment.app.Fragment
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppBarFragmentActivity

class SettingsActivity : AppBarFragmentActivity() {

    override fun createFragment(): Fragment = SettingsFragment()

}
