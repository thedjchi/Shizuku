package moe.shizuku.manager.stealth

import android.content.Context
import android.content.Intent
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
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
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

        onBackPressedDispatcher.addCallback(this, backCallback)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        with(binding) {
            viewModel.uiState.observe(this@StealthTutorialActivity) { state ->
                fab.isVisible = state !is UiState.Loading
                loadingFab.isVisible = state is UiState.Loading

                when (state) {
                    is UiState.Idle -> {
                        val action = state.action

                        packageNameContainer.isVisible = (action == Action.HIDE)
                        if (packageNameContainer.isVisible) makeNavBarTransparent()
                        ViewCompat.requestApplyInsets(root)

                        fab.updateIcon(action)
                        fab.setOnClickListener { onClick(action) }
                    }

                    is UiState.Loading -> null

                    is UiState.Success -> {
                        try {
                            val apk = state.apk
                            when (state.apkType) {
                                ApkType.CLONE -> {
                                    export(apk)
                                    showUninstallDialog()
                                }

                                ApkType.STUB -> {
                                    install(apk)
                                }
                            }
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
        }
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
            Action.UNHIDE -> viewModel.createApk(ApkType.STUB)
            Action.REHIDE -> uninstall()
        }
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

    private fun install(apk: File) {
        val apkUri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            apk
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(intent)
    }

    private fun uninstall() {
        val intent =
            Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$ORIGINAL_PACKAGE_NAME")
            }
        startActivity(intent)
    }

    private fun showUninstallDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Uninstall required")
            .setMessage("The system will now prompt you to uninstall Shizuku. Then, install the clone.")
            .setPositiveButton("Uninstall") { _, _ ->
                uninstall()
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showErrorDialog(error: Exception) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Error")
            .setMessage(error.message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun FloatingActionButton.updateIcon(action: Action) {
        val iconRes =
            if (action == Action.UNHIDE) R.drawable.ic_visibility_on_filled_24
            else R.drawable.ic_visibility_off_filled_24
        setImageResource(iconRes)
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

    private fun makeNavBarTransparent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            window.isNavigationBarContrastEnforced = false
    }

    val Int.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()
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
