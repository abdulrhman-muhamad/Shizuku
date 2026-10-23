package moe.shizuku.manager

import android.content.Context
import androidx.annotation.StringRes

object HelpURLs {

    // TODO(brand): WhtsAgent has no support site yet, so every help, troubleshooting and
    // bug-report link is null. While a link is null, the button, settings entry, notification
    // tap or sentence that would open it is hidden, so nothing routes users to NLL.
    // Set these to WhtsAgent pages to bring them back. Previous (NLL) targets, for reference:
    //   ADB, ADB_ANDROID11            https://github.com/NLLAPPS/Shizuku/wiki/User-manual
    //   ADB_PERMISSION                …/wiki/User-manual#start-via-wireless-debuggingstart-by-connecting-to-a-computer-the-permission-of-adb-is-limited
    //   HELP                          https://github.com/NLLAPPS/Shizuku/wiki
    //   TROUBLESHOOTING_BOOT          …/wiki/Troubleshooting#shizuku-isnt-starting-on-boot
    //   TROUBLESHOOTING_STOPPING      …/wiki/troubleshooting#shizuku-keeps-stopping-randomly
    //   LATEST_VERSION                https://acr.app
    //   ISSUES, NEW_ISSUE             https://github.com/NLLAPPS/Shizuku/issues[/new]
    val ADB: String? = null
    val ADB_ANDROID11: String? = null
    val ADB_PERMISSION: String? = null
    val HELP: String? = null
    val TROUBLESHOOTING_BOOT: String? = null
    val TROUBLESHOOTING_STOPPING: String? = null
    val LATEST_VERSION: String? = null
    val ISSUES: String? = null
    val NEW_ISSUE: String? = null

    /** The bug-report dialog only offers NLL destinations today; show its entry points once ours exist. */
    val BUG_REPORTS_ENABLED: Boolean get() = NEW_ISSUE != null

    val SUI = "https://github.com/RikkaApps/Sui"
    val RISH = "https://github.com/RikkaApps/Shizuku-API/tree/master/rish"

    // Apache-2.0 attribution for the upstream project; keep it.
    val UPSTREAM = "https://github.com/RikkaApps/Shizuku"

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
