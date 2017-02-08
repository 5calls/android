package org.a5calls.android.a5calls.controller;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.a5calls.android.a5calls.R;

/**
 * Creates the notification to make more calls with 5calls.
 */
public class NotifyBroadcastReceiver extends BroadcastReceiver {
    private static final int NOTIFICATION_ID = 42;
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent resultIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                MainActivity.NOTIFICATION_REQUEST, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Notification builder = new Notification.Builder(context)
                .setContentTitle(context.getResources().getString(R.string.notification_title))
                .setContentText(context.getResources().getString(R.string.notification_text))
                .setContentIntent(pendingIntent) // Launch main activity
                .setAutoCancel(true) // Goes away when clicked
                .setSmallIcon(R.drawable.app_icon_bw)
                .build();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder);
        // TODO: Add some intent when this is clicked
        // TODO: Add a notification settings option too which goes direct to Settings
        // TODO: Do not notify if the app is in the foreground (how can we check?)
    }
}
