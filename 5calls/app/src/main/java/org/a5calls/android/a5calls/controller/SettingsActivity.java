package org.a5calls.android.a5calls.controller;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.MenuItem;


import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.net.PushRegistration;
import org.a5calls.android.a5calls.model.NotificationUtils;

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
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_settings);

        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.settings);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() |
                            WindowInsetsCompat.Type.displayCutout());
            findViewById(R.id.appbar).setPadding(insets.left, insets.top, insets.right, 0);
            findViewById(R.id.contentFrame).setPadding(insets.left, 0, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        if (getIntent().getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)) {
            FiveCallsApplication.analyticsManager().trackPageview("/settings", this);
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

    /**
     * Has to be called before the caller starts, so every screen that might ask for the
     * notification permission registers one up front. Null when the permission doesn't exist.
     */
    public static ActivityResultLauncher<String> createNotificationPermissionRequest(
            ActivityResultCaller caller, Consumer<Boolean> isGranted) {
        // Only needed on SDK 33 (Tiramisu) and newer
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return null;
        }
        return caller.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted::accept
        );
    }

    /**
     * Makes sure notifications are allowed before turning on something that needs them.
     * Android's prompt is only shown once: after someone has answered it we don't ask again,
     * and send them to the app's notification settings instead, where they can change their
     * mind. That's also the only way to turn notifications on before SDK 33.
     *
     * @return true if isGranted will hear the answer, either right away because notifications
     * are already allowed or once the prompt is answered. False if we sent them to settings,
     * where we won't hear what they chose.
     */
    public static boolean requestNotificationPermission(
            Activity activity, @Nullable ActivityResultLauncher<String> permissionRequest,
            Consumer<Boolean> isGranted) {
        if (NotificationManagerCompat.from(activity).areNotificationsEnabled()) {
            isGranted.accept(true);
            return true;
        }

        // The rationale check catches people who denied before we started keeping track.
        if (permissionRequest != null
                && !AccountManager.Instance.isNotificationPermissionRequested(activity)
                && !ActivityCompat.shouldShowRequestPermissionRationale(
                        activity, Manifest.permission.POST_NOTIFICATIONS)) {
            AccountManager.Instance.setNotificationPermissionRequested(activity, true);
            permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS);
            return true;
        }

        openNotificationSettings(activity);
        return false;
    }

    private static void openNotificationSettings(Context context) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.getPackageName(), null));
        }
        context.startActivity(intent);
    }

    /**
     * Opts in to push notifications, once we know notifications are allowed. The launcher's
     * callback should pass its result to onPushPermissionResult.
     */
    public static void enablePushNotifications(
            Activity activity, @Nullable ActivityResultLauncher<String> permissionRequest) {
        FiveCallsApplication application = (FiveCallsApplication) activity.getApplication();
        if (!requestNotificationPermission(activity, permissionRequest,
                isGranted -> onPushPermissionResult(application, isGranted))) {
            // Keep their choice while they're in settings. If they allow notifications there,
            // the next launch registers the token; if not, it resets the choice.
            updateNotificationsPreference(application, AccountManager.Instance, "0");
        }
    }

    public static void onPushPermissionResult(FiveCallsApplication application,
                                              boolean isGranted) {
        updateNotificationsPreference(application, AccountManager.Instance,
                isGranted ? "0" : "1");
    }

    public static void updateNotificationsPreference(FiveCallsApplication application,
                                                     AccountManager accountManager,
                                                     String result) {
        accountManager.setNotificationPreference(application, result);
        if (TextUtils.equals("0", result)) {
            // Without the permission the token would be registered but nothing would show up.
            // If it's granted later, the next launch registers it.
            if (NotificationManagerCompat.from(application).areNotificationsEnabled()) {
                PushRegistration.INSTANCE.refreshToken(application);
            }
        } else if (TextUtils.equals("1", result)) {
            PushRegistration.INSTANCE.unregister(application);
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
        private ActivityResultLauncher<String> mNotificationPermissionRequest;
        private ActivityResultLauncher<String> mPushPermissionRequest;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mNotificationPermissionRequest = createNotificationPermissionRequest(
                    this, this::onRemindersPermissionResult);
            mPushPermissionRequest = createNotificationPermissionRequest(
                    this, this::onPushPermissionResult);
        }

        private void onRemindersPermissionResult(boolean isGranted) {
            // If the user denied the notification permission, set the preference to false
            // Otherwise they granted and we will set the permission to true
            accountManager.setAllowReminders(getActivity(), isGranted);
            if (!isGranted) {
                SwitchPreference remindersPref =
                        findPreference(AccountManager.KEY_ALLOW_REMINDERS);
                if (remindersPref == null) {
                    return;
                }
                remindersPref.setChecked(false);
                remindersPref.setSummary(R.string.reminders_disabled_summary);
            }
        }

        private void onPushPermissionResult(boolean isGranted) {
            SettingsActivity.onPushPermissionResult(
                    (FiveCallsApplication) requireActivity().getApplication(), isGranted);
            if (!isGranted) {
                ListPreference notificationsPref = findPreference(AccountManager.KEY_NOTIFICATIONS);
                if (notificationsPref != null) {
                    notificationsPref.setValue("1");
                }
            }
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            addPreferencesFromResource(R.xml.settings);

            boolean hasReminders = accountManager.getAllowReminders(getActivity());
            ((SwitchPreference) findPreference(AccountManager.KEY_ALLOW_REMINDERS))
                    .setChecked(hasReminders);

            Set<String> reminderDays = accountManager.getReminderDays(getActivity());
            MultiSelectListPreference daysPreference =
                    findPreference(AccountManager.KEY_REMINDER_DAYS);
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
                    findPreference(AccountManager.KEY_NOTIFICATIONS);
            notificationPref.setValue(notificationSetting);

            boolean showPlaceholderIssue = accountManager.showPlaceholderIssue(getActivity());
            ((SwitchPreference) findPreference(AccountManager.KEY_SHOW_PLACEHOLDER_CALLED))
                    .setChecked(showPlaceholderIssue);

            boolean enableUndo = accountManager.getEnableUndo((getActivity()));
            ((SwitchPreference) findPreference(AccountManager.KEY_ENABLE_UNDO))
                    .setChecked(enableUndo);
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
                if (result) {
                    if (!requestNotificationPermission(requireActivity(),
                            mNotificationPermissionRequest, this::onRemindersPermissionResult)) {
                        // Sent to settings, so the switch stays off until they come back and
                        // turn it on with notifications allowed.
                        onRemindersPermissionResult(false);
                    }
                } else {
                    accountManager.setAllowReminders(getActivity(), false);
                }
            } else if (TextUtils.equals(key, AccountManager.KEY_REMINDER_DAYS)) {
                Set<String> result = sharedPreferences.getStringSet(key,
                        AccountManager.DEFAULT_REMINDER_DAYS);
                accountManager.setReminderDays(getActivity(), result);
                updateReminderDaysSummary(findPreference(
                        AccountManager.KEY_REMINDER_DAYS), result);
            } else if (TextUtils.equals(key, AccountManager.KEY_NOTIFICATIONS)) {
                String result = sharedPreferences.getString(key,
                        AccountManager.DEFAULT_NOTIFICATION_SELECTION);
                if (TextUtils.equals("0", result)) {
                    enablePushNotifications(requireActivity(), mPushPermissionRequest);
                } else {
                    updateNotificationsPreference(
                            (FiveCallsApplication) getActivity().getApplication(),
                            accountManager, result);
                }
            } else if (TextUtils.equals(key, AccountManager.KEY_USER_NAME)) {
                String result = sharedPreferences.getString(key, null);
                if (result != null) {
                    result = result.trim();
                    AccountManager.Instance.setUserName(getActivity(), result);
                } else {
                    AccountManager.Instance.setUserName(getActivity(), null);
                }
            } else if (TextUtils.equals(key, AccountManager.KEY_SCRIPT_TEXT_SIZE_SP)) {
                String value = sharedPreferences.getString(AccountManager.KEY_SCRIPT_TEXT_SIZE_SP, getString(R.string.script_text_size_normal_sp));
                AccountManager.Instance.setScriptTextSize(getActivity(), Float.parseFloat(value));
            } else if (TextUtils.equals(key, AccountManager.KEY_SHOW_PLACEHOLDER_CALLED)) {
                boolean result = sharedPreferences.getBoolean(key, false);
                AccountManager.Instance.setShowPlaceholderIssue(getActivity(), result);
            } else if (TextUtils.equals(key, AccountManager.KEY_ENABLE_UNDO)) {
                boolean result = sharedPreferences.getBoolean(key, true);
                AccountManager.Instance.setEnableUndo(getActivity(), result);
            }
        }

        @Override
        public void onStop() {
            turnOnReminders(getActivity(), accountManager);
            super.onStop();
        }

        private void updateReminderDaysSummary(MultiSelectListPreference daysPreference,
                                               Set<String> savedValues) {
            if (savedValues == null || savedValues.isEmpty()) {
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
