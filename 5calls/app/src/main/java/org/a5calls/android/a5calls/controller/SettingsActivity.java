package org.a5calls.android.a5calls.controller;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;

/**
 * Settings for the app
 */
public class SettingsActivity extends AppCompatActivity {
    String TAG = "SettingsActivity";

    private final AccountManager accountManager = AccountManager.Instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        getFragmentManager().beginTransaction().replace(R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // We allow Analytics opt-out.
        if (accountManager.allowAnalytics(this)) {
            // Obtain the shared Tracker instance.
            FiveCallsApplication application = (FiveCallsApplication) getApplication();
            Tracker tracker = application.getDefaultTracker();
            tracker.setScreenName(TAG);
            tracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment implements
            SharedPreferences.OnSharedPreferenceChangeListener{
        private final AccountManager accountManager = AccountManager.Instance;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            // TODO: Get this from accountManager instead
            boolean hasReminders = getPreferenceManager().getSharedPreferences()
                    .getBoolean("prefsKeyEnableReminders", true);
            findPreference("prefsKeyRemindersTime").setEnabled(hasReminders);
        }

        @Override
        public void onResume() {
            super.onResume();
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (TextUtils.equals(key, "prefsKeyAllowAnalytics")) {
                boolean result = sharedPreferences.getBoolean(key, true);
                accountManager.setAllowAnalytics(getActivity(), result);
            } else if (TextUtils.equals(key, "prefsKeyEnableReminders")) {
                boolean result = sharedPreferences.getBoolean(key, true);
                findPreference("prefsKeyRemindersTime").setEnabled(result);
                // TODO
            }
        }
    }
}
