package com.hlrms.mobile.data.local

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object AppLocaleManager {

    const val ARABIC = "ar"
    const val ENGLISH = "en"

    private const val PREFERENCES_NAME =
        "hlrms_locale_preferences"

    private const val LANGUAGE_KEY =
        "app_language"

    fun getLanguage(
        context: Context
    ): String {

        return context
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            .getString(
                LANGUAGE_KEY,
                ARABIC
            )
            ?: ARABIC
    }

    fun setLanguage(
        context: Context,
        language: String
    ) {

        val normalizedLanguage =
            when (language) {

                ENGLISH ->
                    ENGLISH

                else ->
                    ARABIC
            }

        /*
         * commit() مقصودة هنا.
         * نريد حفظ اللغة قبل recreate مباشرة.
         */
        context
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                LANGUAGE_KEY,
                normalizedLanguage
            )
            .commit()
    }

    fun wrapContext(
        context: Context
    ): Context {

        val language =
            getLanguage(
                context
            )

        val locale =
            Locale.forLanguageTag(
                language
            )

        Locale.setDefault(
            locale
        )

        val configuration =
            Configuration(
                context.resources.configuration
            )

        configuration.setLocale(
            locale
        )

        configuration.setLayoutDirection(
            locale
        )

        return context
            .createConfigurationContext(
                configuration
            )
    }
}