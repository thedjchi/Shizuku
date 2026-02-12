package moe.shizuku.manager.stealth

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.doOnNextLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppBarActivity
import moe.shizuku.manager.databinding.StealthTutorialActivityBinding
import moe.shizuku.manager.utils.ApkUtils.*
import rikka.core.util.ClipboardUtils
import java.io.File

class StealthTutorialActivity : AppBarActivity() {
    private val viewModel: StealthTutorialViewModel by viewModels()
    private lateinit var binding: StealthTutorialActivityBinding

    private lateinit var outDir: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = StealthTutorialActivityBinding.inflate(layoutInflater, rootView, true)

        onBackPressedDispatcher.addCallback(this, backCallback)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        with(binding) {
            viewModel.uiState.observe(this@StealthTutorialActivity) { state ->
                val isLoadingOrPending = (state is UiState.Loading || state is UiState.Pending)
                fab.isInvisible = isLoadingOrPending
                loadingFab.isVisible = isLoadingOrPending

                when (state) {
                    is UiState.Idle -> {
                        val action = state.action

                        packageNameContainer.isVisible = (action == Action.HIDE)
                        if (packageNameContainer.isVisible) makeNavBarTransparent()

                        fab.update(action)
                        fab.setOnClickListener { onClick(action) }
                    }

                    is UiState.Loading -> {
                        null
                    }

                    is UiState.Pending -> {
                        try {
                            val apk = state.apk
                            when (state.apkType) {
                                ApkType.CLONE -> {
                                    export(apk)
                                    viewModel.refresh()
                                    showUninstallDialog()
                                }

                                ApkType.STUB -> {
                                    installPackage(apk) { isSuccess, msg -> handleInstallerResult(isSuccess, msg) }
                                }
                            }
                        } catch (e: Exception) {
                            showErrorDialog(e)
                        }
                    }

                    is UiState.Error -> {
                        showErrorDialog(state.error)
                    }
                }
            }

            packageNameEditText.apply {
                addTextChangedListener { text ->
                    val input = text.toString()

                    packageNameLayout.error =
                        input.validatePackageName()?.let { getString(it) }

                    packageNameLayout.helperText =
                        if (input.isEmpty()) {
                            getString(R.string.stealth_package_name_helper_text)
                        } else {
                            null
                        }

                    fab.isEnabled = (packageNameLayout.error == null)
                }
            }

            ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
                val systemBarsInsets = insets.getInsets(Type.systemBars())
                val fabBottomMargin =
                    if (packageNameContainer.isVisible) -12.dp else (systemBarsInsets.bottom + 16.dp)

                listOf(fab, loadingFab).forEach {
                    it.updateLayoutParams<MarginLayoutParams> {
                        bottomMargin = fabBottomMargin
                    }
                }

                insets
            }

            root.viewTreeObserver.addOnGlobalLayoutListener {
                scrollView.updatePadding(bottom = root.height - fab.top)
            }
        }
    }

    override fun onDestroy() {
        runCatching {
            unregisterReceiver(installerReceiver)
        }
        super.onDestroy()
    }

    private fun onClick(action: Action) {
        when (action) {
            Action.HIDE -> {
                val packageName =
                    binding.packageNameEditText.text
                        .toString()
                        .ifEmpty { null }
                viewModel.setPackageName(packageName)

                showChooseFolderDialog()
            }

            Action.UNHIDE -> {
                viewModel.createApk(ApkType.STUB)
            }

            Action.REHIDE -> {
                uninstallPackage(ORIGINAL_PACKAGE_NAME) { isSuccess, msg -> handleInstallerResult(isSuccess, msg) }
            }
        }
    }

    private fun showChooseFolderDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.stealth_choose_folder)
            .setMessage(R.string.stealth_choose_folder_message)
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .setPositiveButton(R.string.stealth_choose_folder) { _, _ ->
                pickFolderLauncher.launch(null)
            }.show()
    }

    private val pickFolderLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { folder ->
            if (folder == null) return@registerForActivityResult
            outDir = folder

            viewModel.createApk(ApkType.CLONE)
        }

    private fun export(apk: File) {
        val docUri =
            DocumentsContract.buildDocumentUriUsingTree(
                outDir,
                DocumentsContract.getTreeDocumentId(outDir),
            )

        val doc =
            DocumentsContract.createDocument(
                contentResolver,
                docUri,
                "application/vnd.android.package-archive",
                buildApkFilename(),
            )

        if (doc == null) throw Exception("Could not create file in selected folder")

        contentResolver.openOutputStream(doc)?.use { output ->
            apk.inputStream().use { input ->
                input.copyTo(output)
            }
        }
    }

    private fun showUninstallDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.stealth_uninstall_required)
            .setMessage(R.string.stealth_uninstall_message)
            .setPositiveButton(R.string.uninstall) { _, _ ->
                uninstallPackage(packageName)
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun handleInstallerResult(
        isSuccess: Boolean,
        msg: String?,
    ) {
        viewModel.refresh()
        if (isSuccess) {
            Toast
                .makeText(
                    this,
                    getString(R.string.success),
                    Toast.LENGTH_SHORT,
                ).show()
        } else {
            showErrorDialog(Exception(msg ?: "Unknown error"))
        }
    }

    private fun showErrorDialog(error: Exception) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.error)
            .setMessage(error.message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun ExtendedFloatingActionButton.update(action: Action) {
        if (action == Action.UNHIDE) {
            setIconResource(R.drawable.ic_visibility_on_filled_24)
            text = getString(R.string.unhide)
        } else {
            setIconResource(R.drawable.ic_visibility_off_filled_24)
            text = getString(R.string.hide)
        }
    }

    private val backCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val uiState = viewModel.uiState.value
                if (uiState is UiState.Idle || uiState is UiState.Error) {
                    finish()
                    return
                }
                MaterialAlertDialogBuilder(this@StealthTutorialActivity)
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

    private fun makeNavBarTransparent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }

    val Int.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()
}
