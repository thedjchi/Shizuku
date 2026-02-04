package moe.shizuku.manager.stealth

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.databinding.StealthPatcherActivityBinding

class StealthPatcherActivity : AppActivity() {
    private val viewModel: StealthPatcherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = StealthPatcherActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this, backCallback)

        viewModel.success.observe(this) {
            binding.progressIndicator.isIndeterminate = false
            binding.progressIndicator.progress = if (it) 100 else 0
        }

        viewModel.log.observe(this) {
            binding.terminal.text = it
        }

        val packageName = intent.getStringExtra("package_name")
        val uninstallAfter = intent.getBooleanExtra("uninstall_after", true)

        viewModel.patchAndInstall(packageName, uninstallAfter)
    }

    private val backCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.success.value != null) {
                    finish()
                    return
                }
                MaterialAlertDialogBuilder(this@StealthPatcherActivity)
                    .setTitle("${getString(android.R.string.cancel)}?")
                    .setPositiveButton("Yes") { _, _ -> finish() }
                    .setNegativeButton("No", null)
                    .show()
            }
        }

    override fun onSupportNavigateUp(): Boolean {
        backCallback.handleOnBackPressed()
        return true
    }
}
