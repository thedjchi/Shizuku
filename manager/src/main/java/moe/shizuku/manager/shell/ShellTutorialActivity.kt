package moe.shizuku.manager.shell

import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppBarActivity
import moe.shizuku.manager.databinding.TerminalTutorialActivityBinding
import moe.shizuku.manager.utils.CustomTabsHelper
import moe.shizuku.manager.utils.toHtml
import rikka.compatibility.DeviceCompatibility
import rikka.html.text.HtmlCompat
import rikka.insets.*
import kotlin.math.roundToInt

class ShellTutorialActivity : AppBarActivity() {
    companion object {
        private val SH_NAME = "rish"
        private val DEX_NAME = "rish_shizuku.dex"
    }

    private val openDocumentsTree =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree: Uri? ->
            if (tree == null) return@registerForActivityResult

            val cr = contentResolver
            val doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val child =
                DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))

            cr
                .query(
                    child,
                    arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null,
                    null,
                    null,
                )?.use {
                    while (it.moveToNext()) {
                        val id = it.getString(0)
                        val name = it.getString(1)
                        if (name == SH_NAME || name == DEX_NAME) {
                            DocumentsContract.deleteDocument(cr, DocumentsContract.buildDocumentUriUsingTree(tree, id))
                        }
                    }
                }

            fun writeToDocument(name: String) {
                DocumentsContract.createDocument(contentResolver, doc, "application/octet-stream", name)?.runCatching {
                    cr.openOutputStream(this)?.let { output ->
                        assets.open(name).use { input ->
                            if (name == SH_NAME) {
                                input.bufferedReader().use {
                                    val text =
                                        it
                                            .readText()
                                            .replace("MANAGER_PKG", applicationContext.packageName)
                                    output.write(text.toByteArray())
                                }
                            } else {
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }

            writeToDocument(SH_NAME)
            writeToDocument(DEX_NAME)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = TerminalTutorialActivityBinding.inflate(layoutInflater, rootView, true)

        binding.content.apply {
            setInitialPadding(
                initialPaddingLeft,
                initialPaddingTop + (resources.displayMetrics.density * 8).roundToInt(),
                initialPaddingRight,
                initialPaddingBottom,
            )
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.apply {
            if (DeviceCompatibility.isMiui()) {
                miui.isVisible = true
            }

            val shName = "<font face=\"monospace\">$SH_NAME</font>"
            val dexName = "<font face=\"monospace\">$DEX_NAME</font>"

            text1.text = getString(R.string.terminal_tutorial_1, shName, dexName).toHtml()

            text2.text = getString(R.string.terminal_tutorial_2, shName).toHtml()
            command2.text = "cp /sdcard/chosen-folder/* /data/data/terminal.package.name/files"
            summary2.text = getString(R.string.terminal_tutorial_2_tip, shName, shName, ".bashrc").toHtml()

            text3.text = getString(R.string.terminal_tutorial_3)
            command3.text = "sh /path/to/$SH_NAME"

            button1.setOnClickListener { openDocumentsTree.launch(null) }
        }
    }
}
