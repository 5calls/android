package org.a5calls.android.a5calls.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A singleton responsible for managing all local account-related information
 */
public enum AccountManager {
    Instance;

    private static final String PREFS_FILE = "fiveCallsPrefs";

    private static final String KEY_INITIALIZED = "prefsKeyInitialized";
    public static final String KEY_ALLOW_ANALYTICS = "prefsKeyAllowAnalytics";
    private static final String KEY_USER_ZIP = "prefsKeyUserZip";
    private static final String KEY_LATITUDE = "prefsKeyLatitude";
    private static final String KEY_LONGITUDE = "prefsKeyLongitude";
    private static final String KEY_DATABASE_SAVES_CONTACTS = "prefsKeyDbSavesContacts";
    public static final String KEY_REMINDER_MINUTES = "prefsKeyNotificationMinutes";
    public static final String KEY_REMINDER_DAYS = "prefsKeyReminderDays";
    public static final String KEY_ALLOW_REMINDERS = "prefsKeyEnableReminders";
    private static final String KEY_REMINDERS_INFO_SHOWN = "prefsKeyRemindersInfoShown";

    // Default to 11 am.
    public static final int DEFAULT_REMINDER_MINUTES = 60 * 11;

    // Default to Monday, Wednesday and Friday only.
    public static final Set<String> DEFAULT_REMINDER_DAYS =
            new HashSet<>(Arrays.asList("2", "4", "6"));

    public boolean hasLocation(Context context) {
        // If there's a lat/lng or a zip.
        return (!TextUtils.isEmpty(getLat(context)) && !TextUtils.isEmpty(getLng(context)))
                || !TextUtils.isEmpty(getZip(context));
    }

    // Defaults to true, we'eve already seen the tutorial.
    public boolean isTutorialSeen(Context context) {
        return getSharedPrefs(context).getBoolean(KEY_INITIALIZED, /* not seen yet */ false);
    }

    public void setTutorialSeen(Context context, boolean tutorialSeen) {
        getSharedPrefs(context).edit().putBoolean(KEY_INITIALIZED, tutorialSeen).apply();
    }

    public boolean isRemindersInfoShown(Context context) {
        return getSharedPrefs(context).getBoolean(KEY_REMINDERS_INFO_SHOWN,
                /* not shown yet */ false);
    }

    public void setRemindersInfoShown(Context context, boolean remindersInfoShown) {
        getSharedPrefs(context).edit().putBoolean(KEY_REMINDERS_INFO_SHOWN, remindersInfoShown)
                .apply();
    }

    public String getZip(Context context) {
        return getSharedPrefs(context).getString(KEY_USER_ZIP, "");
    }

    public void setZip(Context context, String zip) {
        getSharedPrefs(context).edit().putString(KEY_USER_ZIP, zip).apply();
    }

    public String getLat(Context context) {
        return getSharedPrefs(context).getString(KEY_LATITUDE, "");
    }

    public void setLat(Context context, String lat) {
        getSharedPrefs(context).edit().putString(KEY_LATITUDE, lat).apply();
    }

    @Nullable
    public String getLng(Context context) {
        return getSharedPrefs(context).getString(KEY_LONGITUDE, "");
    }

    public void setLng(Context context, String lng) {
        getSharedPrefs(context).edit().putString(KEY_LONGITUDE, lng).apply();
    }

    // Defaults to true
    public boolean allowAnalytics(Context context) {
        return getSharedPrefs(context).getBoolean(KEY_ALLOW_ANALYTICS, true);
    }

    public void setAllowAnalytics(Context context, boolean shouldAllow) {
        getSharedPrefs(context).edit().putBoolean(KEY_ALLOW_ANALYTICS, shouldAllow).apply();
    }

    public boolean getDatabaseSavesContacts(Context context) {
        return getSharedPrefs(context).getBoolean(KEY_DATABASE_SAVES_CONTACTS, false);
    }

    public void setDatabaseSavesContacts(Context context, boolean savesContacts) {
        getSharedPrefs(context).edit().putBoolean(KEY_DATABASE_SAVES_CONTACTS,
                savesContacts).apply();
    }

    public boolean getAllowReminders(Context context) {
        return getSharedPrefs(context).getBoolean(KEY_ALLOW_REMINDERS, true);
    }

    public void setAllowReminders(Context context, boolean allowReminders) {
        getSharedPrefs(context).edit().putBoolean(KEY_ALLOW_REMINDERS, allowReminders)
                .apply();
    }

    public int getReminderMinutes(Context context) {
        return getSharedPrefs(context).getInt(KEY_REMINDER_MINUTES,
                DEFAULT_REMINDER_MINUTES);
    }

    public void setReminderMinutes(Context context, int reminderMinutes) {
        getSharedPrefs(context).edit().putInt(KEY_REMINDER_MINUTES, reminderMinutes)
                .apply();
    }

    public Set<String> getReminderDays(Context context) {
        return getSharedPrefs(context).getStringSet(KEY_REMINDER_DAYS,
                DEFAULT_REMINDER_DAYS);
    }

    public void setReminderDays(Context context, Set<String> reminderDays) {
        getSharedPrefs(context).edit().putStringSet(KEY_REMINDER_DAYS, reminderDays)
                .apply();
    }

    private SharedPreferences getSharedPrefs(Context context) {
        return context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    }
}
