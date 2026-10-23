package moe.shizuku.manager.settings

import android.app.Dialog
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.HelpURLs
import moe.shizuku.manager.R
import moe.shizuku.manager.databinding.BugReportDialogBinding
import moe.shizuku.manager.ktx.asLink
import moe.shizuku.manager.ktx.applyTemplateArgs
import moe.shizuku.manager.utils.CustomTabsHelper
import moe.shizuku.manager.worker.AdbStartWorker
import androidx.core.net.toUri
import androidx.core.view.isVisible

class BugReportDialog : DialogFragment() {

    companion object {
        /** Opens a pre-filled bug-report email to support_email. Returns false if no email app exists. */
        fun sendEmail(context: Context): Boolean {
            val plainBody = """
                Please describe the bug. Include steps to reproduce if possible, as well as any relevant images/logs.

                Device: ${Build.MANUFACTURER} ${Build.MODEL}
                Android Version: ${Build.VERSION.RELEASE}
                ${context.getString(R.string.app_name)} Version: ${BuildConfig.VERSION_NAME}
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SENDTO, ("mailto:" + context.getString(R.string.support_email) +
                    "?subject=" + Uri.encode("[ISSUE TITLE]") +
                    "&body=" + Uri.encode(plainBody)).toUri())
            return try {
                context.startActivity(intent)
                true
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, context.getString(R.string.toast_no_email_app), Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    private lateinit var binding: BugReportDialogBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        binding = BugReportDialogBinding.inflate(layoutInflater)

        binding.apply {
            // Each "please check first" row needs its page; hide rows (and the header) without one.
            HelpURLs.LATEST_VERSION?.let { updateText.applyTemplateArgs(getString(R.string.bug_report_dialog_link_update).asLink(it)) }
            HelpURLs.HELP?.let { wikiText.applyTemplateArgs(getString(R.string.bug_report_dialog_link_wiki).asLink(it)) }
            HelpURLs.ISSUES?.let { issuesText.applyTemplateArgs(getString(R.string.bug_report_dialog_link_issues).asLink(it)) }
            updateText.isVisible = HelpURLs.LATEST_VERSION != null
            wikiText.isVisible = HelpURLs.HELP != null
            issuesText.isVisible = HelpURLs.ISSUES != null
            val anyCheck = updateText.isVisible || wikiText.isVisible || issuesText.isVisible
            beforeText.isVisible = anyCheck
            divider.isVisible = anyCheck

            methodText.applyTemplateArgs("GitHub")
            methodText.isVisible = HelpURLs.NEW_ISSUE != null
            methodText2.isVisible = HelpURLs.NEW_ISSUE != null
        }

        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_report_bug)
            .setView(binding.root)
        HelpURLs.NEW_ISSUE?.let { url ->
            builder.setPositiveButton("GitHub") { _, _ -> CustomTabsHelper.launchUrlOrCopy(context, url) }
        }
        return builder
            .setNegativeButton(R.string.bug_report_dialog_button_email) { _, _ ->
                if (sendEmail(context)) dismiss()
            }
            .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .create()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        val nm = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(AdbStartWorker.NOTIFICATION_ID)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (activity is BugReportDialogActivity) activity?.finish()
    }

}