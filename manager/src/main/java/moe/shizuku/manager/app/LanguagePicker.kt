package moe.shizuku.manager.app

import android.content.Context
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.LocaleListCompat
import moe.shizuku.manager.R
import org.xmlpull.v1.XmlPullParser
import java.text.Collator
import java.util.Locale

/**
 * The app's language dropdown, offered on every screen (toolbar) and in Settings. Uses AppCompat
 * per-app locales, so it works on every Android version (AppLocalesMetadataHolderService stores the
 * choice below Android 13), unlike the system's per-app language screen (Android 13+ only).
 */
object LanguagePicker {

    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

    /** Shown first: the languages most WhtsAgent users need. */
    private val PINNED = listOf("en-US", "ar")

    fun show(context: Context, anchor: View) {
        val entries = entries(context)
        val current = AppCompatDelegate.getApplicationLocales()[0]
        val popup = PopupMenu(context, anchor)
        entries.forEachIndexed { index, (tag, label) ->
            popup.menu.add(0, index, index, label).isChecked = matches(tag, current)
        }
        popup.menu.setGroupCheckable(0, true, true)
        popup.setOnMenuItemClickListener { item ->
            apply(entries[item.itemId].first)
            true
        }
        popup.show()
    }

    /** "" means follow the system language. */
    private fun apply(tag: String) {
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
        )
    }

    private fun entries(context: Context): List<Pair<String, String>> {
        val collator = Collator.getInstance()
        val tags = supportedTags(context)
        val pinned = PINNED.filter { it in tags }
        val rest = (tags - pinned.toSet()).sortedWith(compareBy(collator) { label(it) })
        return listOf("" to context.getString(R.string.follow_system)) + (pinned + rest).map { it to label(it) }
    }

    /** The languages the app is translated into: AGP's generated locale config (one per values-* folder). */
    private fun supportedTags(context: Context): List<String> {
        val tags = mutableListOf<String>()
        context.resources.getXml(R.xml._generated_res_locale_config).use { parser ->
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                    parser.getAttributeValue(ANDROID_NS, "name")?.let { tags += it }
                }
            }
        }
        return tags
    }

    /** The current app language's name as the dropdown shows it, or null when following the system. */
    fun currentLabel(): String? =
        AppCompatDelegate.getApplicationLocales()[0]?.takeIf { it.language.isNotEmpty() }?.let { label(it.toLanguageTag()) }

    /** Each language in its own name and script, e.g. "العربية", "Deutsch"; the default is just "English". */
    private fun label(tag: String): String {
        val locale = if (tag == "en-US") Locale.ENGLISH else Locale.forLanguageTag(tag)
        return locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
    }

    private fun matches(tag: String, current: Locale?): Boolean {
        if (current == null || current.language.isEmpty()) return tag.isEmpty()
        if (tag.isEmpty()) return false
        val candidate = Locale.forLanguageTag(tag)
        return candidate.language == current.language &&
                (candidate.country.isEmpty() || current.country.isEmpty() || candidate.country == current.country)
    }
}
