package moe.shizuku.manager.stealth

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppBarActivity
import moe.shizuku.manager.databinding.StealthTutorialActivityBinding
import moe.shizuku.manager.utils.ApkUtils.buildApkFilename
import rikka.core.util.ClipboardUtils

class StealthTutorialActivity : AppBarActivity() {

    private val viewModel: StealthTutorialViewModel by viewModels()
    private lateinit var binding: StealthTutorialActivityBinding

    private lateinit var outDir: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = StealthTutorialActivityBinding.inflate(layoutInflater, rootView, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            window.isNavigationBarContrastEnforced = false

        onBackPressedDispatcher.addCallback(this, backCallback)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        with(binding) {
            viewModel.uiState.observe(this@StealthTutorialActivity) { state ->
                fab.updateIcon(state)
                loadingFab.isVisible = state is UiState.Loading

                when (state) {

                    is UiState.Idle -> {
                        val isShizukuHidden = state.isShizukuHidden

                        packageNameEditText.isEnabled = !isShizukuHidden
                        if (isShizukuHidden) {
                            packageNameEditText.setText(ORIGINAL_PACKAGE_NAME)
                        } else {
                            fab.setOnClickListener { onHide() }
                            packageNameLayout.helperText = getString(R.string.stealth_package_name_helper_text)
                        }
                    }

                    is UiState.Loading -> null

                    is UiState.Success -> {
                        try {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(
                                outDir,
                                DocumentsContract.getTreeDocumentId(outDir)
                            )

                            val doc = DocumentsContract.createDocument(
                                contentResolver,
                                docUri,
                                "application/vnd.android.package-archive",
                                buildApkFilename()
                            )

                            if (doc == null) throw Exception("Could not create file in selected folder")

                            contentResolver.openOutputStream(doc)?.use { output ->
                                state.apk.inputStream().use { input ->
                                    input.copyTo(output)
                                }
                            }

                            showUninstallDialog()
                        } catch (e: Exception) {
                            showErrorDialog(e)
                        }
                    }

                    is UiState.Error -> showErrorDialog(state.error)
                    
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

            compatibilityCard.setOnClickListener { v: View ->
                val context = v.context
                if (
                    ClipboardUtils.put(context, codeSnippet) &&
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2
                ) {
                    Toast
                        .makeText(
                            context,
                            context.getString(R.string.toast_copied_to_clipboard),
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }
        }
    }

    private fun onHide() {
        // TO-DO: IF CLONE IS INSTALLED, THEN UNINSTALL THE STUB INSTEAD
        val packageName =
            binding.packageNameEditText.text
                .toString()
                .ifEmpty { null }
        viewModel.setPackageName(packageName)

        showChooseFolderDialog()
    }

    private fun onUnhide() {
        // TO-DO: IF THE STUB ISN'T INSTALLED, THEN CREATE THE STUB
        showInstallDialog()
    }

    private fun showChooseFolderDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Choose folder")
            .setMessage("Shizuku needs to save the new APK to your device. Please choose a folder.")
            .setNegativeButton("Cancel") { _, _ -> }
            .setPositiveButton("Choose folder") { _, _ ->
                pickFolderLauncher.launch(null)
            }.show()
    }

    private val pickFolderLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { folder ->
            if (folder == null) return@registerForActivityResult      
            outDir = folder

            viewModel.createClone()
        }

    private fun showUninstallDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Uninstall required")
            .setMessage("The system will now prompt you to uninstall the original version of Shizuku.")
            .setPositiveButton("Uninstall") { _, _ ->
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.parse("package:$ORIGINAL_PACKAGE_NAME")
                }
                startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showInstallDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Success")
            .setMessage("The system will now prompt you to install the Shizuku stub.")
            .setPositiveButton("Install") { _, _ ->
                null
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showErrorDialog(error: Exception) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Error")
            .setMessage(error.message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun FloatingActionButton.updateIcon(state: UiState) {
        isVisible = state !is UiState.Loading

        if (state is UiState.Idle) {
            val iconRes =
                if (state.isShizukuHidden) R.drawable.ic_visibility_on_filled_24
                else R.drawable.ic_visibility_off_filled_24
            setImageResource(iconRes)
        }
    }

    private val backCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.uiState.value !is UiState.Loading) {
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
