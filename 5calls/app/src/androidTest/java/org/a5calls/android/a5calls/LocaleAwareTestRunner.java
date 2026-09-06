package org.a5calls.android.a5calls;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnitRunner;
import java.util.Locale;

public class LocaleAwareTestRunner extends AndroidJUnitRunner {

    @Override
    public void onCreate(Bundle arguments) {
        System.err.println("LocaleAwareTestRunner onCreate");
        // 1. Intercept the custom CLI parameter 'locale'
        String localeTag = arguments.getString("locale");
        System.err.println("LocaleAwareTestRunner localeTag: " + localeTag);

        Bundle testArguments = InstrumentationRegistry.getArguments();
        localeTag = testArguments.getString("locale");
        System.err.println("LocaleAwareTestRunner localeTag: " + localeTag);


        if (localeTag != null && !localeTag.isEmpty()) {
            Locale locale = Locale.forLanguageTag(localeTag);
            setGlobalLocale(locale);
        }

        // 1. Extract the device language
        String systemLanguage = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                ? LocaleList.getDefault().get(0).toLanguageTag()
                : Locale.getDefault().toLanguageTag();

        System.err.println("LocaleAwareTestRunner systemLanguage: " + systemLanguage);
        // 2. Put it into the arguments bundle
        arguments.putString("device_language", systemLanguage);

        // 2. Continue initializing the rest of the runner configuration
        super.onCreate(arguments);
    }

    @SuppressWarnings("deprecation")
    private void setGlobalLocale(Locale locale) {
        System.err.println("LocaleAwareTestRunner Setting global locale to " + locale);
        Locale.setDefault(locale);

        // Modify Target App Context configuration before tests boot up
        Context context = getTargetContext();
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
        } else {
            configuration.locale = locale;
        }

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }
}
