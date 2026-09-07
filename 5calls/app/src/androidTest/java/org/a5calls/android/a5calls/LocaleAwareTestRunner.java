package org.a5calls.android.a5calls;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.test.runner.AndroidJUnitRunner;

import java.util.Locale;

public class LocaleAwareTestRunner extends AndroidJUnitRunner {

    private Bundle mArguments;

    @Override
    public void onCreate(Bundle arguments) {
        mArguments = arguments;
        // 1. Intercept the custom CLI parameter 'locale'
        final String localeTag = arguments.getString("locale");
        System.err.println("LocaleAwareTestRunner intercepted localeTag: " + localeTag);

        if (localeTag != null && !localeTag.isEmpty()) {
            Locale locale = Locale.forLanguageTag(localeTag);
            setGlobalLocale(locale);
        }

        // 2. Extract the device language for diagnostic purposes
        String systemLanguage = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) ? LocaleList.getDefault().get(0).toLanguageTag() : Locale.getDefault().toLanguageTag();

        System.err.println("LocaleAwareTestRunner systemLanguage: " + systemLanguage);
        arguments.putString("device_language", systemLanguage);

        super.onCreate(arguments);
    }

    @Override
    public void onStart() {
        // Use AppCompatDelegate to set locales globally for the app.
        // This is the most reliable way for AppCompat-based activities.
        final String localeTag = mArguments != null ? mArguments.getString("locale") : null;
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                if (localeTag != null && !localeTag.isEmpty()) {
                    System.err.println("LocaleAwareTestRunner setting AppCompatDelegate locales to: " + localeTag);
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag));
                } else {
                    // Reset to system default if no locale specified to avoid persistence from previous runs
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
                }
            }
        });
        super.onStart();
    }

    @SuppressWarnings("deprecation")
    private void setGlobalLocale(Locale locale) {
        System.err.println("LocaleAwareTestRunner Setting global locale to " + locale);
        Locale.setDefault(locale);

        // Update configuration for both target context and application context
        updateContextLocale(getTargetContext(), locale);
        updateContextLocale(getTargetContext().getApplicationContext(), locale);
    }

    @SuppressWarnings("deprecation")
    private void updateContextLocale(Context context, Locale locale) {
        if (context == null) return;

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(new LocaleList(locale));
        } else {
            configuration.locale = locale;
        }

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }
}
