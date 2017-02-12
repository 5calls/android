package org.a5calls.android.a5calls.controller;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.BuildConfig;
import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

/**
 * Creates the notification to make more calls with 5calls.
 */
public class NotifyBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "NotifyBroadcastRcvr";
    private static final int NOTIFICATION_ID = 42;
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Broadcast received");
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
                MainActivity.NOTIFICATION_REQUEST, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle(context.getResources().getString(R.string.notification_title))
                .setContentText(context.getResources().getString(R.string.notification_text))
                .setContentIntent(pendingIntent) // Launch main activity
                .setAutoCancel(true) // Goes away when clicked
                .setSmallIcon(R.drawable.app_icon_bw);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(context.getResources().getColor(R.color.colorPrimary));
        }
        Notification notification = builder.build();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
        // TODO: Add a notification settings option too which goes direct to Settings
        // TODO: Ask UX about the icon
    }
}
