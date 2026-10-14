package org.a5calls.android.a5calls

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnitRunner
import java.util.Locale

/**
 * Enables running the instrumentation tests in Spanish, either by explicitly setting the app locale via CLI argument,
 * or simply running the emulator/device with a Spanish system-wide locale setting and the test detects same.
 *
 * For instrumentation testing forcing the app to run in the US English locale no matter the settings on the phone/emulator:
 * Emulator system locale irrelevant, ./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.locale=en-US
 *
 * For instrumentation testing forcing the app to run in the US Spanish locale no matter the settings on the phone/emulator:
 * Emulator system locale irrelevant,./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.locale=es-US
 */
class LocaleAwareTestRunner : AndroidJUnitRunner() {
    private var mArguments: Bundle? = null

    override fun onCreate(arguments: Bundle) {
        mArguments = arguments
        super.onCreate(arguments)

        val localeTag = getLocaleTag()
        System.err.println("LocaleAwareTestRunner onCreate resolved localeTag: $localeTag")
        if (!localeTag.isNullOrEmpty()) {
            applyLocaleToContext(targetContext, localeTag)
            targetContext?.applicationContext?.let { applyLocaleToContext(it, localeTag) }
        }
    }

    private fun getInitialArguments(): Bundle? {
        runCatching { InstrumentationRegistry.getArguments() }
            .getOrNull()
            ?.let { return it }

        mArguments?.let { return it }

        return runCatching {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread")
            currentActivityThreadMethod.isAccessible = true
            val activityThread = currentActivityThreadMethod.invoke(null) ?: return@runCatching null

            val boundApplicationField = activityThreadClass.getDeclaredField("mBoundApplication")
            boundApplicationField.isAccessible = true
            val boundApplication = boundApplicationField.get(activityThread) ?: return@runCatching null

            val instrumentationArgsField = boundApplication.javaClass.getDeclaredField("instrumentationArgs")
            instrumentationArgsField.isAccessible = true
            instrumentationArgsField.get(boundApplication) as? Bundle
        }.getOrNull()
    }

    private fun getLocaleTag(): String? {
        val args = getInitialArguments()
        var localeTag = args?.getString("locale")

        if (localeTag.isNullOrEmpty() && targetContext != null) {
            val prefs = targetContext.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
            localeTag = prefs.getString("test_locale", null)
        }

        if (localeTag.isNullOrEmpty()) {
            val sysLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && targetContext != null) {
                targetContext.resources.configuration.locales.get(0)
            } else {
                Locale.getDefault()
            }
            if (sysLocale?.language == "es") {
                localeTag = sysLocale.toLanguageTag()
            }
        }
        return localeTag
    }

    private fun applyLocaleToContext(context: Context?, localeTag: String) {
        if (context == null) return
        val locale = Locale.forLanguageTag(localeTag)
        Locale.setDefault(locale)

        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("test_locale", localeTag).commit()

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(android.os.LocaleList(locale))
        } else {
            config.setLocale(locale)
        }
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
        val localeTag = getLocaleTag()
        System.err.println("LocaleAwareTestRunner newApplication intercepted localeTag: $localeTag")
        if (!localeTag.isNullOrEmpty()) {
            applyLocaleToContext(context, localeTag)
        }
        return super.newApplication(cl, className, context)
    }

    override fun callApplicationOnCreate(app: Application?) {
        val localeTag = getLocaleTag()
        if (!localeTag.isNullOrEmpty() && app != null) {
            applyLocaleToContext(app, localeTag)
        }
        super.callApplicationOnCreate(app)
    }

    override fun callActivityOnCreate(activity: Activity?, icicle: Bundle?) {
        val localeTag = getLocaleTag()
        if (!localeTag.isNullOrEmpty() && activity != null) {
            applyLocaleToContext(activity, localeTag)
        }
        super.callActivityOnCreate(activity, icicle)
    }

    override fun onStart() {
        val localeTag = getLocaleTag()
        System.err.println("LocaleAwareTestRunner onStart resolved localeTag: $localeTag")
        if (!localeTag.isNullOrEmpty()) {
            applyLocaleToContext(targetContext, localeTag)
            runOnMainSync {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(localeTag)
                )
            }
        } else {
            runOnMainSync {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            }
        }
        super.onStart()
    }
}
