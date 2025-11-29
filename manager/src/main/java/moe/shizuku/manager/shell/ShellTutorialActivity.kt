package moe.shizuku.manager.shell

import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import kotlin.math.roundToInt
import moe.shizuku.manager.Helps
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppBarActivity
import moe.shizuku.manager.databinding.TerminalTutorialActivityBinding
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.utils.CustomTabsHelper
import rikka.compatibility.DeviceCompatibility
import rikka.html.text.HtmlCompat
import rikka.insets.*

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

            cr.query(
                child,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
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
                    cr.openOutputStream(this)?.let { assets.open(name).copyTo(it) }
                }
            }

            writeToDocument(SH_NAME)
            writeToDocument(DEX_NAME)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = TerminalTutorialActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.content.apply {
            setInitialPadding(
                initialPaddingLeft,
                initialPaddingTop + (resources.displayMetrics.density * 8).roundToInt(),
                initialPaddingRight,
                initialPaddingBottom
            )
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.apply {
            if (DeviceCompatibility.isMiui()) {
                miui.isVisible = true
            }

            val shName = "<font face=\"monospace\">$SH_NAME</font>"
            val dexName = "<font face=\"monospace\">$DEX_NAME</font>"

            summary.text = getString(R.string.rish_description, shName)
                .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)

            text1.text = getString(R.string.terminal_tutorial_1)
            summary1.text = getString(R.string.terminal_tutorial_1_description, shName, dexName)
                .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)

            text2.text = getString(R.string.terminal_tutorial_2, shName).toHtml()
            command2.text = "cp /sdcard/chosen-folder/* /data/data/terminal.package.name/files"
            summary2.text = getString(R.string.terminal_tutorial_2_description, shName, shName, ".bashrc").toHtml()

            text3.text = getString(R.string.terminal_tutorial_3)
            command3.text = "sh /path/to/$SH_NAME"

            button1.setOnClickListener { openDocumentsTree.launch(null) }
            button2.setOnClickListener { v: View -> CustomTabsHelper.launchUrlOrCopy(v.context, Helps.RISH.get()) }
        }
    }
}
