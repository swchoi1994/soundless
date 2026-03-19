package com.soundless

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class LanguageManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("soundless", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(loadLanguage())
    val language: StateFlow<Language> = _language

    private fun loadLanguage(): Language {
        val saved = prefs.getString("selected_language", null)
        if (saved != null) {
            return Language.entries.find { it.code == saved } ?: detectSystemLanguage()
        }
        return detectSystemLanguage()
    }

    private fun detectSystemLanguage(): Language {
        val locale = Locale.getDefault()
        return when (locale.language) {
            "ko" -> Language.KO
            "ja" -> Language.JA
            "ar" -> Language.AR
            "zh" -> {
                val script = locale.script
                val country = locale.country
                when {
                    script == "Hant" || country in listOf("TW", "HK", "MO") -> Language.ZH_TW
                    else -> Language.ZH_CN
                }
            }
            else -> Language.EN
        }
    }

    fun setLanguage(lang: Language) {
        prefs.edit().putString("selected_language", lang.code).apply()
        _language.value = lang
    }
}
