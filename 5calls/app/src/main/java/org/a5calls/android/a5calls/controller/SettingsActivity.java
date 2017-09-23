package org.a5calls.android.a5calls.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.NotificationUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Settings for the app
 */
// TODO: Analytics and Notification settings need a way to retry if connection was not available.
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

    public static void turnOnReminders(Context context, AccountManager manager) {
        // Set up the notification firing logic when the settings activity ends, so as not
        // to do the work too frequently.
        if (manager.getAllowReminders(context)) {
            NotificationUtils.setReminderTime(context, manager.getReminderMinutes(context));
        } else {
            NotificationUtils.cancelFutureReminders(context);
        }
    }

    public static void updateAllowAnalytics(Context context, boolean allowAnalytics) {
        if (allowAnalytics) {
            ((FiveCallsApplication) context.getApplicationContext()).enableAnalyticsHandler();
        } else {
            ((FiveCallsApplication) context.getApplicationContext()).disableAnalyticsHandler();
        }
    }

    public static void updateNotificationsPreference(FiveCallsApplication application,
                                                     AccountManager accountManager,
                                                     String result) {
        accountManager.setNotificationPreference(application, result);
        if (TextUtils.equals("0", result)) {
            OneSignalNotificationController.enableTopNotifications();
        } else if (TextUtils.equals("1", result)) {
            OneSignalNotificationController.enableAllNotifications();
        } else if (TextUtils.equals("2", result)) {
            OneSignalNotificationController.disableNotifications();
        }
        // If the user changes the settings there's no need to show the dialog in the future.
        accountManager.setNotificationDialogShown(application, true);
        // Log this to Analytics
        if (accountManager.allowAnalytics(application)) {
            Tracker tracker = application.getDefaultTracker();
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory("NotificationSettings")
                    .setAction("NotificationSettingsChange")
                    .setLabel(application.getApplicationContext().getResources()
                            .getStringArray(R.array.notification_options)[Integer.valueOf(result)])
                    .setValue(1)
                    .build());
        }
    }

    public static class SettingsFragment extends PreferenceFragment implements
            SharedPreferences.OnSharedPreferenceChangeListener{
        private final AccountManager accountManager = AccountManager.Instance;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            boolean hasReminders = accountManager.getAllowReminders(getActivity());
            ((SwitchPreference) findPreference(AccountManager.KEY_ALLOW_REMINDERS))
                    .setChecked(hasReminders);

            Set<String> reminderDays = accountManager.getReminderDays(getActivity());
            MultiSelectListPreference daysPreference =
                    (MultiSelectListPreference) findPreference(AccountManager.KEY_REMINDER_DAYS);
            daysPreference.setValues(reminderDays);
            updateReminderDaysSummary(daysPreference, reminderDays);

            String notificationSetting = accountManager.getNotificationPreference(getActivity());
            ListPreference notificationPref =
                    (ListPreference) findPreference(AccountManager.KEY_NOTIFICATIONS);
            notificationPref.setValue(notificationSetting);
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
            if (TextUtils.equals(key, AccountManager.KEY_ALLOW_ANALYTICS)) {
                boolean result = sharedPreferences.getBoolean(key, true);
                accountManager.setAllowAnalytics(getActivity(), result);
                updateAllowAnalytics(getActivity(), result);
            } else if (TextUtils.equals(key, AccountManager.KEY_ALLOW_REMINDERS)) {
                boolean result = sharedPreferences.getBoolean(key, true);
                accountManager.setAllowReminders(getActivity(), result);
            } else if (TextUtils.equals(key, AccountManager.KEY_REMINDER_DAYS)) {
                Set<String> result = sharedPreferences.getStringSet(key,
                        AccountManager.DEFAULT_REMINDER_DAYS);
                accountManager.setReminderDays(getActivity(), result);
                updateReminderDaysSummary((MultiSelectListPreference) findPreference(
                        AccountManager.KEY_REMINDER_DAYS), result);
            } else if (TextUtils.equals(key, AccountManager.KEY_NOTIFICATIONS)) {
                String result = sharedPreferences.getString(key,
                        AccountManager.DEFAULT_NOTIFICATION_SELECTION);
                updateNotificationsPreference((FiveCallsApplication) getActivity().getApplication(),
                        accountManager, result);
            }
        }

        @Override
        public void onStop() {
            turnOnReminders(getActivity(), accountManager);
            super.onStop();
        }

        private void updateReminderDaysSummary(MultiSelectListPreference daysPreference,
                                               Set<String> savedValues) {
            if (savedValues == null || savedValues.size() == 0) {
                daysPreference.setSummary(getActivity().getResources().getString(
                        R.string.no_days_selected));
                return;
            }
            List<String> daysEntries = Arrays.asList(getActivity().getResources()
                    .getStringArray(R.array.reminder_days_titles));
            List<String> daysEntriesValues = Arrays.asList(getActivity().getResources()
                    .getStringArray(R.array.reminder_days_values));
            String summary = "";
            for (int i = 0; i < daysEntriesValues.size(); i++) {
                if (savedValues.contains(daysEntriesValues.get(i))) {
                    if (!TextUtils.isEmpty(summary)) {
                        summary += ", ";
                    }
                    summary += daysEntries.get(i);
                }
            }
            daysPreference.setSummary(summary);
        }
    }
}
