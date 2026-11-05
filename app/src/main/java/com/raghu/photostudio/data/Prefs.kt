package com.raghu.photostudio.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Off by default: the shop usually shoots against a real white wall already. */
    var whiteBackgroundDefault: Boolean
        get() = sp.getBoolean("white_bg", false)
        set(v) = sp.edit().putBoolean("white_bg", v).apply()

    var startWithBackCamera: Boolean
        get() = sp.getBoolean("back_camera", true)
        set(v) = sp.edit().putBoolean("back_camera", v).apply()

    var cutLines: Boolean
        get() = sp.getBoolean("cut_lines", true)
        set(v) = sp.edit().putBoolean("cut_lines", v).apply()

    // ---- Quick bill ----

    /** Shop name printed at the top of every bill. */
    var shopName: String
        get() = sp.getString("shop_name", "").orEmpty()
        set(v) = sp.edit().putString("shop_name", v.trim()).apply()

    /** Line under the shop name: address, phone. */
    var shopDetails: String
        get() = sp.getString("shop_details", "").orEmpty()
        set(v) = sp.edit().putString("shop_details", v.trim()).apply()

    /** Number the next bill gets. Goes up by one each time a bill is shared or printed. */
    var nextBillNumber: Int
        get() = sp.getInt("next_bill_no", 1)
        set(v) = sp.edit().putInt("next_bill_no", v).apply()

    /** Items billed recently, newest first, with their last rate — offered as one-tap chips. */
    var recentItems: List<Pair<String, Double>>
        get() = sp.getString("recent_items", "").orEmpty()
            .lines()
            .mapNotNull { line ->
                val parts = line.split('\t')
                val rate = parts.getOrNull(1)?.toDoubleOrNull()
                if (parts[0].isBlank() || rate == null) null else parts[0] to rate
            }
        set(v) = sp.edit().putString(
            "recent_items",
            v.joinToString("\n") { (name, rate) -> name.replace('\t', ' ').replace('\n', ' ') + "\t" + rate },
        ).apply()

    /** Puts these items at the front of the recent list (no repeats, at most 12). */
    fun rememberItems(items: List<Pair<String, Double>>) {
        val fresh = items.filter { it.first.isNotBlank() }.map { it.first.trim() to it.second }
        val names = fresh.map { it.first.lowercase() }.toSet()
        recentItems = (fresh.distinctBy { it.first.lowercase() } +
            recentItems.filter { it.first.lowercase() !in names }).take(12)
    }

    companion object {
        const val LANG_EN = "en"
        const val LANG_TE = "te"

        /** Current app language: "te" or "en". Follows the phone language until changed in Settings. */
        fun currentLanguage(): String {
            val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            val lang = if (tags.isBlank()) Locale.getDefault().language else tags
            return if (lang.startsWith(LANG_TE)) LANG_TE else LANG_EN
        }

        /** Switches language; the screen reloads automatically and the choice is remembered. */
        fun setLanguage(tag: String) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }
}
