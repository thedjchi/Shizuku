package moe.shizuku.manager.home

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.app.AppBarActivity
import moe.shizuku.manager.app.SnackbarHelper
import moe.shizuku.manager.databinding.AboutDialogBinding
import moe.shizuku.manager.databinding.HomeActivityBinding
import moe.shizuku.manager.home.showAccessibilityDialog
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.management.AppsViewModel
import moe.shizuku.manager.settings.SettingsActivity
import moe.shizuku.manager.utils.AppIconCache
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.SettingsHelper
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.core.content.asActivity
import rikka.core.ktx.unsafeLazy
import rikka.lifecycle.Status
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import rikka.shizuku.Shizuku

abstract class HomeActivity : AppBarActivity() {

    private val homeModel: HomeViewModel by viewModels()
    private val appsModel: AppsViewModel by viewModels()
    private val adapter by unsafeLazy { HomeAdapter(homeModel, appsModel, lifecycleScope) }

    private val stateListener: (ShizukuStateMachine.State) -> Unit = {
        if (ShizukuStateMachine.isRunning()) {
            checkServerStatus()
            appsModel.load()
        } else if (ShizukuStateMachine.isDead()) {
            checkServerStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = HomeActivityBinding.inflate(layoutInflater, rootView, true)

        homeModel.serviceStatus.observe(this) {
            if (it.status == Status.SUCCESS) {
                val status = homeModel.serviceStatus.value?.data ?: return@observe
                adapter.updateData()
                ShizukuSettings.setLastLaunchMode(if (status.uid == 0) ShizukuSettings.LaunchMethod.ROOT else ShizukuSettings.LaunchMethod.ADB)
            }
        }

        homeModel.shouldShowBatteryOptimizationSnackbar.observe(this) {
            if (it) {
                SnackbarHelper.show(
                    this,
                    binding.root,
                    msg = getString(R.string.snackbar_battery_optimization_home),
                    duration = Snackbar.LENGTH_INDEFINITE,
                    actionText = getString(R.string.snackbar_action_fix),
                    action = { SettingsHelper.requestIgnoreBatteryOptimizations(this, null) }
                )
            }
        }
        homeModel.checkBatteryOptimization()

        appsModel.grantedCount.observe(this) {
            if (it.status == Status.SUCCESS) {
                adapter.updateData()
            }
        }

        val recyclerView = binding.list
        recyclerView.adapter = adapter
        recyclerView.fixEdgeEffect()

        val cardSpacing = resources.getDimension(R.dimen.card_spacing)
        val marginHorizontal = resources.getDimension(R.dimen.margin_horizontal)
        val marginVertical = resources.getDimension(R.dimen.margin_vertical)

        val itemSpacing = cardSpacing / 2f
        val edgeSpacingH = marginHorizontal
        val edgeSpacingV = marginVertical - itemSpacing

        recyclerView.addItemSpacing(top = itemSpacing, bottom = itemSpacing)
        recyclerView.addEdgeSpacing(top = edgeSpacingV, bottom = edgeSpacingV, left = edgeSpacingH, right = edgeSpacingH)

        ShizukuStateMachine.addListener(stateListener)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            val showDialog = it.getBooleanExtra(HomeActivity.EXTRA_SHOW_PAIRING_DIALOG, false)
            if (showDialog) showAccessibilityDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        checkServerStatus()
        appsModel.load()
    }

    override fun onPause() {
        super.onPause()
        SnackbarHelper.dismiss()
    }

    private fun checkServerStatus() {
        homeModel.reload()
    }

    override fun onDestroy() {
        ShizukuStateMachine.removeListener(stateListener)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                val binding = AboutDialogBinding.inflate(LayoutInflater.from(this), null, false)
                binding.sourceCode.movementMethod = LinkMovementMethod.getInstance()
                binding.sourceCode.text = getString(
                    R.string.about_view_source_code,
                    "<b><a href=\"https://github.com/thedjchi/Shizuku\">GitHub</a></b>"
                ).toHtml()
                binding.icon.setImageBitmap(
                    AppIconCache.getOrLoadBitmap(
                        this,
                        applicationInfo,
                        Process.myUid() / 100000,
                        resources.getDimensionPixelOffset(R.dimen.default_app_icon_size)
                    )
                )
                binding.versionName.text = packageManager.getPackageInfo(packageName, 0).versionName

                val dialog = MaterialAlertDialogBuilder(this)
                    .setView(binding.root)
                    .create()

                binding.btnClose.setOnClickListener {
                    dialog.dismiss()
                }
                
                dialog.show()
                true
            }
            R.id.action_stop -> {
                if (ShizukuStateMachine.isRunning()) {
                    MaterialAlertDialogBuilder(this)
                        .setMessage(R.string.dialog_stop_message)
                        .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                            ShizukuStateMachine.set(ShizukuStateMachine.State.STOPPING)
                            runCatching { Shizuku.exit() }
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val EXTRA_SHOW_PAIRING_DIALOG = "show_pairing_dialog"
    }

}
