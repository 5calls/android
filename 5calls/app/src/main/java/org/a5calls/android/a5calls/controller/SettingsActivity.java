package org.a5calls.android.a5calls.controller;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.MenuItem;

import com.onesignal.Continue;
import com.onesignal.OneSignal;

import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.NotificationUtils;
import org.a5calls.android.a5calls.util.AnalyticsManager;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Settings for the app
 */
// TODO: Analytics and Notification settings need a way to retry if connection was not available.
public class SettingsActivity extends AppCompatActivity {
    public static final String EXTRA_FROM_NOTIFICATION = "fromNotification";
    static String TAG = "SettingsActivity";

    private final AccountManager accountManager = AccountManager.Instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (getIntent().getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)) {
            new AnalyticsManager().trackPageview("/settings", this);
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent upIntent = getParentActivityIntent();
            if (shouldUpRecreateTask(upIntent) || isTaskRoot()) {
                // This activity is NOT part of this app's task, so create a new task
                // when navigating up, with a synthesized back stack.
                // This is probably because we opened settings from the notification.
                if (upIntent != null) {
                    TaskStackBuilder.create(this)
                            // Add all of this activity's parents to the back stack
                            .addNextIntentWithParentStack(upIntent)
                            // Navigate up to the closest parent
                            .startActivities();
                }
            } else {
                navigateUpTo(upIntent);
            }
            return true;
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

    public static void updateNotificationsPreference(FiveCallsApplication application,
                                                     AccountManager accountManager,
                                                     String result) {
        accountManager.setNotificationPreference(application, result);
        if (TextUtils.equals("0", result)) {
            OneSignal.getNotifications().requestPermission(true, Continue.none());
            OneSignal.getUser().getPushSubscription().optIn();
            // TODO: Wait for permission request result before opting in
        } else if (TextUtils.equals("1", result)) {
            OneSignal.getUser().getPushSubscription().optOut();
        }
        // If the user changes the settings there's no need to show the dialog in the future.
        accountManager.setNotificationDialogShown(application, true);
        // Log this to Analytics
        if (accountManager.allowAnalytics(application)) {
//            Tracker tracker = application.getDefaultTracker();
//            tracker.send(new HitBuilders.EventBuilder()
//                    .setCategory("Notifications")
//                    .setAction("NotificationSettingsChange")
//                    .setLabel(application.getApplicationContext().getResources()
//                            .getStringArray(R.array.notification_options)[Integer.valueOf(result)])
//                    .setValue(1)
//                    .build());
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements
            SharedPreferences.OnSharedPreferenceChangeListener {
        private final AccountManager accountManager = AccountManager.Instance;

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            addPreferencesFromResource(R.xml.settings);

            boolean hasReminders = accountManager.getAllowReminders(getActivity());
            ((SwitchPreference) findPreference(AccountManager.KEY_ALLOW_REMINDERS))
                    .setChecked(hasReminders);

            Set<String> reminderDays = accountManager.getReminderDays(getActivity());
            MultiSelectListPreference daysPreference =
                    (MultiSelectListPreference) findPreference(AccountManager.KEY_REMINDER_DAYS);
            daysPreference.setValues(reminderDays);
            updateReminderDaysSummary(daysPreference, reminderDays);

            Preference timePreference = findPreference("prefsKeyReminderTimePlaceholder");
            timePreference.setOnPreferenceClickListener(preference -> {
                final TimePickerFragment dialog = new TimePickerFragment();
                dialog.setCallback((hourOfDay, minute) -> {
                    final int reminderMinutes = hourOfDay * 60 + minute;
                    accountManager.setReminderMinutes(requireContext(), reminderMinutes);
                    updateReminderTimeSummary(timePreference);
                });
                dialog.show(getParentFragmentManager(), "timePicker");
                return true;
            });
            updateReminderTimeSummary(timePreference);

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
            } else if (TextUtils.equals(key, AccountManager.KEY_ALLOW_REMINDERS)) {
                boolean result = sharedPreferences.getBoolean(key, false);
                if (result && !NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                    // Trying to enable reminders and notification permission is not granted
                    requestNotificationPermission((isGranted) -> {
                        // If the user denied the notification permission, set the preference to false
                        // Otherwise they granted and we will set the permission to true
                        accountManager.setAllowReminders(getActivity(), isGranted);
                    });
                } else {
                    // Either disabling reminders or notification permission is already granted
                    // so we don't need to prompt
                    accountManager.setAllowReminders(getActivity(), result);
                }

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
            } else if (TextUtils.equals(key, AccountManager.KEY_USER_NAME)) {
                String result = sharedPreferences.getString(key, null);
                if (result != null) {
                    result = result.trim();
                    AccountManager.Instance.setUserName(getActivity(), result);
                } else {
                    AccountManager.Instance.setUserName(getActivity(), null);
                }
            } else if (TextUtils.equals(key, "prefsKeyScriptTextSize")) {
                String value = sharedPreferences.getString(AccountManager.KEY_SCRIPT_TEXT_SIZE_SP, getString(R.string.script_text_size_normal_sp));
                AccountManager.Instance.setScriptTextSize(getActivity(), Float.parseFloat(value));
            }
        }

        @Override
        public void onStop() {
            turnOnReminders(getActivity(), accountManager);
            super.onStop();
        }

        private void requestNotificationPermission(Consumer<Boolean> isGranted) {
            // Only needed on SDK 33 (Tiramisu) and newer
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return;
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                return;
            }
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(), isGranted::accept
            );
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

        private void updateReminderTimeSummary(Preference timePreference) {
            Calendar c = Calendar.getInstance();
            int storedMinutes = accountManager.getReminderMinutes(requireContext());
            int hour = storedMinutes / 60;
            int minutes = storedMinutes % 60;

            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, minutes);

            final SimpleDateFormat dateFormat;
            if (DateFormat.is24HourFormat(requireContext())) {
                dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            } else {
                dateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            }
            timePreference.setSummary(dateFormat.format(c.getTime()));
        }
    }
}
