package moe.shizuku.manager

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate

object HelpURLs {

    // WhtsAgent Helper's support page (whats-m: app/[locale]/helper), opened in the app's language
    // when the page has it (English, Arabic). The anchors must match the page's section ids.
    // A null link hides the button, settings entry, notification tap or sentence that would open it.
    private const val PAGE = "https://whtsagent.com/helper"
    private const val PAGE_AR = "https://whtsagent.com/ar/helper"

    private val page: String get() = if (appLanguage() == "ar") PAGE_AR else PAGE
    private fun section(anchor: String) = "$page#$anchor"

    val ADB: String? get() = section("computer")
    val ADB_ANDROID11: String? get() = section("wireless-debugging")
    val ADB_PERMISSION: String? get() = section("extra-step")
    val HELP: String? get() = page
    val TROUBLESHOOTING_BOOT: String? get() = section("start-on-boot")
    val TROUBLESHOOTING_STOPPING: String? get() = section("keeps-stopping")
    // TODO(brand): section("latest-version") once the page has a real download link (it shows a
    // placeholder today).
    val LATEST_VERSION: String? = null
    // No public issue tracker: bug reports go to support_email.
    val ISSUES: String? = null
    val NEW_ISSUE: String? = null

    /** The app's chosen language, else the phone's. */
    private fun appLanguage(): String =
        AppCompatDelegate.getApplicationLocales()[0]?.language?.takeIf { it.isNotEmpty() }
            ?: Resources.getSystem().configuration.locales[0].language

    /**
     * Bug reports always have a destination (support_email). These pages are only what the report
     * dialog asks users to check first; with none of them, "Report a bug" opens the email directly.
     */
    val HAS_BUG_REPORT_PAGES: Boolean get() = listOf(LATEST_VERSION, HELP, ISSUES, NEW_ISSUE).any { it != null }

    val SUI = "https://github.com/RikkaApps/Sui"
    val RISH = "https://github.com/RikkaApps/Shizuku-API/tree/master/rish"

    /** [res] formatted with [url] as its link; without a url, the sentence holding the link is dropped. */
    fun linkedText(context: Context, @StringRes res: Int, url: String?): String =
        if (url != null) context.getString(res, url) else withoutLastSentence(context.getString(res, ""))

    private val sentenceEnds = listOf(". ", "。", "۔ ", "। ", "! ", "? ", "！", "？", "։ ")

    /**
     * Drops the sentence that points users at a hidden help page: the one holding the `<a>` link,
     * or the final sentence when there is no link. Works on every translation by markup and
     * punctuation rather than wording — a `<p>` break first, then the language's sentence end,
     * then (for scripts without one, e.g. Thai) the last space.
     */
    fun withoutLastSentence(text: String): String {
        val link = text.indexOf("<a ")
        val end = if (link >= 0) link else text.trimEnd().length - 1
        // Every match must finish before `end`, hence the `- length` start offsets.
        val paragraph = text.lastIndexOf("<p>", end - 3)
        if (paragraph >= 0) return text.substring(0, paragraph)
        var cut = sentenceEnds.maxOf { e -> text.lastIndexOf(e, end - e.length).let { if (it < 0) -1 else it + e.length } }
        if (cut <= 0) cut = text.lastIndexOf(' ', end - 1)
        return if (cut > 0) text.substring(0, cut).trimEnd() else text
    }
}
