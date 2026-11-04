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
