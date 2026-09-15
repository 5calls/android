package org.a5calls.android.a5calls

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.runner.AndroidJUnitRunner
import java.util.Locale

class LocaleAwareTestRunner : AndroidJUnitRunner() {
    private var mArguments: Bundle? = null

    override fun onCreate(arguments: Bundle) {
        mArguments = arguments
        val localeTag = arguments.getString("locale")
        System.err.println("LocaleAwareTestRunner intercepted localeTag: $localeTag")

        if (!localeTag.isNullOrEmpty()) {
            val locale = Locale.forLanguageTag(localeTag)
            setGlobalLocale(locale)
        }
        super.onCreate(arguments)
    }

    override fun onStart() {
        // Use AppCompatDelegate to set locales globally for the app.
        // This is the most reliable way for AppCompat-based activities.
        val localeTag = if (mArguments != null) mArguments!!.getString("locale") else null
        runOnMainSync {
            if (!localeTag.isNullOrEmpty()) {
                System.err.println("LocaleAwareTestRunner setting AppCompatDelegate locales to: $localeTag")
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(
                        localeTag
                    )
                )
            } else {
                // Reset to system default if no locale specified to avoid persistence from previous runs
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            }
        }
        super.onStart()
    }

    @Suppress("deprecation")
    private fun setGlobalLocale(locale: Locale) {
        System.err.println("LocaleAwareTestRunner Setting global locale to $locale")
        Locale.setDefault(locale)

        // Update configuration for both target context and application context
        updateContextLocale(targetContext, locale)
        updateContextLocale(targetContext.applicationContext, locale)
    }

    @Suppress("deprecation")
    private fun updateContextLocale(context: Context?, locale: Locale) {
        if (context == null) return

        val resources = context.resources
        val configuration = resources.configuration

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(locale))
        } else {
            configuration.locale = locale
        }

        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
}
