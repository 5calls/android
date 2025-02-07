package org.a5calls.android.a5calls.model;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
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

    // Do not change these key values. They reference SharedPreference values.
    private static final String KEY_INITIALIZED = "prefsKeyInitialized";
    public static final String KEY_ALLOW_ANALYTICS = "prefsKeyAllowAnalytics";
    private static final String KEY_USER_ADDRESS = "prefsKeyUserZip";
    private static final String KEY_LATITUDE = "prefsKeyLatitude";
    private static final String KEY_LONGITUDE = "prefsKeyLongitude";
    private static final String KEY_DATABASE_SAVES_CONTACTS = "prefsKeyDbSavesContacts";
    public static final String KEY_REMINDER_MINUTES = "prefsKeyNotificationMinutes";
    public static final String KEY_REMINDER_DAYS = "prefsKeyReminderDays";
    public static final String KEY_ALLOW_REMINDERS = "prefsKeyEnableReminders";
    private static final String KEY_REMINDERS_INFO_SHOWN = "prefsKeyRemindersInfoShown";
    public static final String KEY_NOTIFICATIONS = "prefsKeyNotifications";
    private static final String KEY_NOTIFICATION_DIALOG_SHOWN = "prefsKeyNotificationDialog";
    private static final String KEY_CALLER_ID = "prefsKeyCallerID";
    private static final String KEY_REVIEW_DIALOG_SHOWN = "prefsKeyReviewDialog";
    private static final String KEY_LOCATION_NAME = "prefsKeyLocationName";

    // Default to 11 am.
    public static final int DEFAULT_REMINDER_MINUTES = 60 * 11;

    // Default to Monday, Wednesday and Friday only.
    public static final Set<String> DEFAULT_REMINDER_DAYS =
            new HashSet<>(Arrays.asList("2", "4", "6"));

    // Default to no notifications.
    public static final String DEFAULT_NOTIFICATION_SELECTION = "2";

    public boolean hasLocation(Context context) {
        // If there's a lat/lng or an address.
        return (!TextUtils.isEmpty(getLat(context)) && !TextUtils.isEmpty(getLng(context)))
                || !TextUtils.isEmpty(getAddress(context));
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

    public String getAddress(Context context) {
        return getSharedPrefs(context).getString(KEY_USER_ADDRESS, "");
    }

    public void setAddress(Context context, String address) {
        getSharedPrefs(context).edit().putString(KEY_USER_ADDRESS, address).apply();
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
        return getSharedPrefs(context).getBoolean(KEY_DATABASE_SAVES_CONTACTS, true);
    }

    public void setDatabaseSavesContacts(Context context, boolean savesContacts) {
        getSharedPrefs(context).edit().putBoolean(KEY_DATABASE_SAVES_CONTACTS,
                savesContacts).apply();
    }

    public boolean getAllowReminders(Context context) {
        return getSharedPrefs(context).getBoolean(KEY_ALLOW_REMINDERS, true);
    }

    public void setAllowReminders(Context context, boolean allowReminders) {
        getSharedPrefs(context).edit().putBoolean(KEY_ALLOW_REMINDERS, allowReminders).apply();
    }

    public int getReminderMinutes(Context context) {
        return getSharedPrefs(context).getInt(KEY_REMINDER_MINUTES, DEFAULT_REMINDER_MINUTES);
    }

    public void setReminderMinutes(Context context, int reminderMinutes) {
        getSharedPrefs(context).edit().putInt(KEY_REMINDER_MINUTES, reminderMinutes).apply();
    }

    public Set<String> getReminderDays(Context context) {
        return getSharedPrefs(context).getStringSet(KEY_REMINDER_DAYS, DEFAULT_REMINDER_DAYS);
    }

    public void setReminderDays(Context context, Set<String> reminderDays) {
        getSharedPrefs(context).edit().putStringSet(KEY_REMINDER_DAYS, reminderDays).apply();
    }

    public void setNotificationPreference(Context context, String preference) {
        getSharedPrefs(context).edit().putString(KEY_NOTIFICATIONS, preference).apply();
    }

    public String getNotificationPreference(Context context) {
        return getSharedPrefs(context).getString(KEY_NOTIFICATIONS, DEFAULT_NOTIFICATION_SELECTION);
    }

    public boolean isNotificationDialogShown(Context context) {
        return getSharedPrefs(context).getBoolean(KEY_NOTIFICATION_DIALOG_SHOWN,
                /* not seen yet */ false);
    }

    public void setNotificationDialogShown(Context context, boolean shown) {
        getSharedPrefs(context).edit().putBoolean(KEY_NOTIFICATION_DIALOG_SHOWN, shown).apply();
    }

    public void setCallerID(Context context, String callerId) {
        getSharedPrefs(context).edit().putString(KEY_CALLER_ID, callerId).apply();
    }

    public String getCallerID(Context context) {
        return getSharedPrefs(context).getString(KEY_CALLER_ID, "");
    }

    public boolean hasReviewDialogBeenShown(Context context) {
        return getSharedPrefs(context).getBoolean(KEY_REVIEW_DIALOG_SHOWN,
                /* not seen yet */ false);
    }

    public void setReviewDialogShown(Context context, boolean shown) {
        getSharedPrefs(context).edit().putBoolean(KEY_REVIEW_DIALOG_SHOWN, shown).apply();
    }

    public String getLocationName(Context context) {
        return getSharedPrefs(context).getString(KEY_LOCATION_NAME, null);
    }

    public void setLocationName(Context context, String locationName) {
        getSharedPrefs(context).edit().putString(KEY_LOCATION_NAME, locationName).apply();
    }

    private SharedPreferences getSharedPrefs(Context context) {
        return context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    }
}
