package org.a5calls.android.a5calls.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.text.TextUtils;

/**
 * A singleton responsible for managing all local account-related information
 */
public enum AccountManager {
    Instance;

    private static final String PREFS_FILE = "fiveCallsPrefs";

    private static final String KEY_INITIALIZED = "prefsKeyInitialized";
    private static final String KEY_ALLOW_ANALYTICS = "prefsKeyAllowAnalytics";
    private static final String KEY_USER_ZIP = "prefsKeyUserZip";
    private static final String KEY_LATITUDE = "prefsKeyLatitude";
    private static final String KEY_LONGITUDE = "prefsKeyLongitude";

    public boolean hasLocation(Context context) {
        // If there's a lat/lng or a zip.
        return (!TextUtils.isEmpty(getLat(context)) && !TextUtils.isEmpty(getLng(context)))
                || !TextUtils.isEmpty(getZip(context));
    }

    // Defaults to true, we'eve already seen the tutorial.
    public boolean isTutorialSeen(Context context) {
        return getSharedPrefs(context).getBoolean(KEY_INITIALIZED, true);
    }

    public void setTutorialSeen(Context context, boolean tutorialSeen) {
        getSharedPrefs(context).edit().putBoolean(KEY_INITIALIZED, tutorialSeen).apply();
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

    private SharedPreferences getSharedPrefs(Context context) {
        return context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    }
}
