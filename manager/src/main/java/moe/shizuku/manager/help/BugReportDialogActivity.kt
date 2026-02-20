package moe.shizuku.manager.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class BugReportDialogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BugReportDialog().show(supportFragmentManager, "BugReportDialog")
    }
}