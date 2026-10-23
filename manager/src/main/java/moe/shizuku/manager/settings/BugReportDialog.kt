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

class BugReportDialog : DialogFragment() {

    private lateinit var binding: BugReportDialogBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        binding = BugReportDialogBinding.inflate(layoutInflater)

        val updateLink = getString(R.string.bug_report_dialog_link_update)
            .asLink(HelpURLs.LATEST_VERSION.orEmpty())

        val wikiLink = getString(R.string.bug_report_dialog_link_wiki)
            .asLink(HelpURLs.HELP.orEmpty())

        val issuesLink = getString(R.string.bug_report_dialog_link_issues)
            .asLink(HelpURLs.ISSUES.orEmpty())

        binding.apply {
            updateText.applyTemplateArgs(updateLink)
            wikiText.applyTemplateArgs(wikiLink)
            issuesText.applyTemplateArgs(issuesLink)
            methodText.applyTemplateArgs("GitHub")
        }

        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_report_bug)
            .setView(binding.root)
            .setPositiveButton("GitHub") { _, _ ->
                HelpURLs.NEW_ISSUE?.let { CustomTabsHelper.launchUrlOrCopy(context, it) }
            }
            .setNegativeButton(R.string.bug_report_dialog_button_email) { _, _ ->
                val plainBody = """
                    Please describe the bug. Include steps to reproduce if possible, as well as any relevant images/logs.

                    Device: ${Build.MANUFACTURER} ${Build.MODEL}
                    Android Version: ${Build.VERSION.RELEASE}
                    ${context.getString(R.string.app_name)} Version: ${BuildConfig.VERSION_NAME}
                """.trimIndent()

                val intent = Intent(Intent.ACTION_SENDTO, ("mailto:" + context.getString(R.string.support_email) +
                        "?subject=" + Uri.encode("[ISSUE TITLE]") +
                        "&body=" + Uri.encode(plainBody)).toUri())
                try {
                    context.startActivity(intent)
                    dismiss()
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, context.getString(R.string.toast_no_email_app), Toast.LENGTH_SHORT).show()
                }
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