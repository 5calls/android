package org.a5calls.android.a5calls;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Settings for the app
 */
public class SettingsActivity extends AppCompatActivity {
    String TAG = "SettingsActivity";

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
        SharedPreferences pref = getSharedPreferences(MainActivity.PREFS_FILE, MODE_PRIVATE);
        if (pref.getBoolean(MainActivity.KEY_ALLOW_ANALYTICS, true)) {
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

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            final SharedPreferences mPreferences = getActivity().getSharedPreferences(
                    MainActivity.PREFS_FILE, MODE_PRIVATE);
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .registerOnSharedPreferenceChangeListener(
                            new SharedPreferences.OnSharedPreferenceChangeListener() {
                                @Override
                                public void onSharedPreferenceChanged(
                                        SharedPreferences sharedPreferences, String key) {
                                    // TODO: If we add more preferences, this needs to change.
                                    boolean result = sharedPreferences.getBoolean(key, true);
                                    mPreferences.edit().putBoolean(MainActivity.KEY_ALLOW_ANALYTICS,
                                            result).apply();

                                }
            });
        }
    }
}
