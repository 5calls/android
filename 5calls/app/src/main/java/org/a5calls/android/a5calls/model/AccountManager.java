package org.a5calls.android.a5calls.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.a5calls.android.a5calls.FiveCallsApplication;

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
        return (getLat(context) != null && getLng(context) != null) || !TextUtils.isEmpty(getZip(context));
    }

    // Defaults to true
    public boolean isFirstTimeInApp(Context context) {
        return getSharedPrefs(context).getBoolean(KEY_INITIALIZED, true);
    }

    public void setIsFirstTimeInApp(Context context, boolean isFirstTime) {
        getSharedPrefs(context).edit().putBoolean(KEY_INITIALIZED, isFirstTime).apply();
    }

    public String getZip(Context context) {
        return getSharedPrefs(context).getString(KEY_USER_ZIP, "");
    }

    public void setZip(Context context, String zip) {
        getSharedPrefs(context).edit().putString(KEY_USER_ZIP, zip).apply();
    }

    // Unfortunately, this has to be nullable because 0,0 is a valid lat,lng, so there's not really a sensical default value.
    @Nullable
    public Double getLat(Context context) {
        // Use a long to store the bits, http://stackoverflow.com/questions/16319237/cant-put-double-sharedpreferences.
        long bits = getSharedPrefs(context).getLong(KEY_LATITUDE, Long.MAX_VALUE);
        if (bits == Long.MAX_VALUE) {
            return null;
        }
        return Double.longBitsToDouble(bits);
    }

    public void setLat(Context context, Double lat) {
        if (lat != null) {
            getSharedPrefs(context).edit().putLong(KEY_LATITUDE, Double.doubleToLongBits(lat)).apply();
        } else {
            getSharedPrefs(context).edit().remove(KEY_LATITUDE).apply();
        }
    }

    @Nullable
    public Double getLng(Context context) {
        // Use a long to store the bits, http://stackoverflow.com/questions/16319237/cant-put-double-sharedpreferences.
        Long bits = getSharedPrefs(context).getLong(KEY_LONGITUDE, Long.MAX_VALUE);
        if (bits == Long.MAX_VALUE) {
            return null;
        }
        return Double.longBitsToDouble(bits);
    }

    public void setLng(Context context, Double lng) {
        if (lng != null) {
            getSharedPrefs(context).edit().putLong(KEY_LONGITUDE, Double.doubleToLongBits(lng)).apply();
        } else {
            getSharedPrefs(context).edit().remove(KEY_LONGITUDE).apply();
        }
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
