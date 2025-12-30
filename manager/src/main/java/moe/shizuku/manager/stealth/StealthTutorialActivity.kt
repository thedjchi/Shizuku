package moe.shizuku.manager.stealth

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.transition.TransitionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppBarActivity
import moe.shizuku.manager.databinding.StealthTutorialActivityBinding
import rikka.core.util.ClipboardUtils

class StealthTutorialActivity : AppBarActivity() {
    private val viewModel: StealthTutorialViewModel by viewModels()

    private lateinit var binding: StealthTutorialActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = StealthTutorialActivityBinding.inflate(layoutInflater, rootView, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            window.isNavigationBarContrastEnforced = false

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        with(binding) {
            viewModel.uiState.observe(this@StealthTutorialActivity) { state ->
                packageNameEditText.isEnabled = state is UiState.Hide
                fab.isCheckable = state is UiState.Unhide
                fab.updateIcon(state)

                when (state) {
                    UiState.Hide -> {
                        fab.setOnClickListener { startPatcherActivity() }
                        packageNameLayout.helperText = getString(R.string.stealth_package_name_helper_text)
                    }

                    is UiState.Unhide -> {
                        packageNameEditText.setText(ORIGINAL_PACKAGE_NAME)
                    }
                }
            }

            packageNameEditText.apply {
                addTextChangedListener { text ->
                    val input = text.toString()

                    packageNameLayout.error =
                        input.validatePackageName()?.let { getString(it) }

                    packageNameLayout.helperText =
                        if (input.isEmpty()) getString(R.string.stealth_package_name_helper_text)
                        else null

                    fab.isEnabled = (packageNameLayout.error == null)
                }
            }

            fab.addOnCheckedChangeListener { _, isChecked ->
                fabMenu.animate(isChecked)
                fab.updateIcon(viewModel.uiState.value ?: UiState.Hide)
            }

            unhideTemp.setOnClickListener {
                fab.isChecked = false
                startPatcherActivity(uninstallAfter = false)
            }
            unhidePerm.setOnClickListener {
                fab.isChecked = false
                startPatcherActivity()
            }

            compatibilityCard.setOnClickListener { v: View ->
                val context = v.context
                if (ClipboardUtils.put(context, codeSnippet) && Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_copied_to_clipboard),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    private fun startPatcherActivity(uninstallAfter: Boolean = true) {
        val packageName = binding.packageNameEditText.text.toString()                                
        val intent = Intent(this, StealthPatcherActivity::class.java).apply {
            if (!packageName.isEmpty()) putExtra("package_name", packageName)
            putExtra("uninstall_after", uninstallAfter)
        }
        startActivity(intent)
    }

    private fun MaterialButton.updateIcon(state: UiState) {
        val iconRes =
            when (state) {
                UiState.Hide -> R.drawable.ic_visibility_off_filled_24
                UiState.Unhide -> {
                    if (isChecked) R.drawable.ic_close_24
                    else R.drawable.ic_visibility_on_filled_24
                }
            }
        setIconResource(iconRes)
    }

    private fun View.animate(shouldShow: Boolean) {
        val transition =
            MaterialSharedAxis(MaterialSharedAxis.X, shouldShow).apply {
                duration = 300
            }
        TransitionManager.beginDelayedTransition(parent as ViewGroup, transition)
        isVisible = shouldShow
    }
}

private const val codeSnippet = """
import android.content.Context
import rikka.shizuku.ShizukuProvider

private fun Context.shizukuPermission() =
    runCatching {
        packageManager.getPermissionInfo(ShizukuProvider.PERMISSION, 0)
    }.getOrNull()

fun Context.isShizukuInstalled() =
    shizukuPermission() != null

fun Context.getShizukuPackageName() =
    shizukuPermission()?.packageName
"""
