package moe.shizuku.manager.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import moe.shizuku.manager.HelpURLs

class BugReportDialogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (HelpURLs.HAS_BUG_REPORT_PAGES) {
            BugReportDialog().show(supportFragmentManager, "BugReportDialog")
        } else {
            BugReportDialog.sendEmail(this)
            finish()
        }
    }
}