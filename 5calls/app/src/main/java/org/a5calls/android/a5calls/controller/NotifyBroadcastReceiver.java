package org.a5calls.android.a5calls.controller;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

//import com.google.android.gms.analytics.HitBuilders;
//import com.google.android.gms.analytics.Tracker;

import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.NotificationUtils;

import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Creates the notification to make more calls with 5calls when the alarm broadcast is received.
 */
public class NotifyBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "NotifyBroadcastRcvr";

    public static final String CHANNEL_ID = "5calls_notification_channel";
    public static final int NOTIFICATION_ID = 42;
    private static final int SNOOZE_REQUEST_CODE = 1;
    private static final int CANCEL_REQUEST_CODE = 2;
    private static final int GO_TO_SETTINGS_REQUEST_CODE = 3;

    private static final String ACTION_DO_SNOOZE = "org.a5calls.android.a5calls.controller.snooze";
    private static final String ACTION_CANCEL_NOTIFY =
            "org.a5calls.android.a5calls.controller.cancel";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "received broadcast");
        if (TextUtils.equals(intent.getAction(), ACTION_DO_SNOOZE)) {
            // Cancel the notification
            ((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE))
                    .cancel(NOTIFICATION_ID);
            // Do the snooze
            NotificationUtils.snoozeNotification(context);
            if (AccountManager.Instance.allowAnalytics(context)) {
                FiveCallsApplication application = (FiveCallsApplication)
                        context.getApplicationContext();
//                Tracker tracker = application.getDefaultTracker();
//                tracker.send(new HitBuilders.EventBuilder()
//                        .setCategory("Reminders")
//                        .setAction("SnoozeReminder")
//                        .setValue(1)
//                        .build());
            }
            return;
        }
        if (TextUtils.equals(intent.getAction(), ACTION_CANCEL_NOTIFY)) {
            // Cancel the notification and do nothing else
            ((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE))
                    .cancel(NOTIFICATION_ID);
            if (AccountManager.Instance.allowAnalytics(context)) {
                FiveCallsApplication application = (FiveCallsApplication)
                        context.getApplicationContext();
//                Tracker tracker = application.getDefaultTracker();
//                tracker.send(new HitBuilders.EventBuilder()
//                        .setCategory("Reminders")
//                        .setAction("CancelReminder")
//                        .setValue(1)
//                        .build());
            }
            return;
        }
        if (((FiveCallsApplication) context.getApplicationContext()).isRunning()) {
            // Don't notify if we are already in the foreground.
            return;
        }

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        String dayOfWeek = String.valueOf(calendar.get(Calendar.DAY_OF_WEEK));
        Set<String> notificationDays = AccountManager.Instance.getReminderDays(context);
        if (!notificationDays.contains(dayOfWeek)) {
            // Don't notify on unselected days of the week
            return;
        }

        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.putExtra(MainActivity.EXTRA_FROM_NOTIFICATION, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                MainActivity.NOTIFICATION_REQUEST, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle(context.getResources().getString(R.string.notification_title))
                .setContentText(context.getResources().getString(R.string.notification_text))
                .setContentIntent(pendingIntent) // Launch main activity
                .setAutoCancel(true) // Goes away when clicked
                .setSmallIcon(R.drawable.app_icon_bw);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // O and above require Notification Channels.
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(context.getResources().getColor(R.color.colorPrimary));
            // Set up a snooze action, which when clicked notifies this same broadcast receiver
            Intent snoozeIntent = new Intent(context, NotifyBroadcastReceiver.class)
                    .setAction(ACTION_DO_SNOOZE);
            PendingIntent pendingSnooze = PendingIntent.getBroadcast(context,
                    SNOOZE_REQUEST_CODE, snoozeIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Action snoozeAction = new Notification.Action.Builder(
                    R.drawable.ic_snooze_white_24dp,
                    context.getResources().getString(R.string.snooze),
                    pendingSnooze)
                    .build();
            builder.addAction(snoozeAction);
            // Add a link to get to settings
            Intent settingsIntent = new Intent(context, SettingsActivity.class);
            settingsIntent.putExtra(SettingsActivity.EXTRA_FROM_NOTIFICATION, true);
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingSettings = PendingIntent.getActivity(context,
                    GO_TO_SETTINGS_REQUEST_CODE, settingsIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Action settingsAction = new Notification.Action.Builder(
                    R.drawable.ic_settings_black_24dp,
                    context.getResources().getString(R.string.settings),
                    pendingSettings)
                    .build();
            builder.addAction(settingsAction);
            // Add a cancel action too
            Intent cancelIntent = new Intent(context, NotifyBroadcastReceiver.class)
                    .setAction(ACTION_CANCEL_NOTIFY);
            PendingIntent pendingCancel = PendingIntent.getBroadcast(context,
                    CANCEL_REQUEST_CODE, cancelIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Action cancelAction = new Notification.Action.Builder(
                    R.drawable.ic_close_white_24dp,
                    context.getResources().getString(R.string.dismiss),
                    pendingCancel)
                    .build();
            builder.addAction(cancelAction);
        }
        Notification notification = builder.build();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
}
