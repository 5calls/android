package org.a5calls.android.a5calls.model;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.a5calls.android.a5calls.BuildConfig;
import org.a5calls.android.a5calls.controller.NotifyBroadcastReceiver;

import java.util.Calendar;

/**
 * Manages scheduling notifications
 */
public class NotificationUtils {
    private static final String TAG = "NotificationUtils";
    private static final long MS_PER_MIN = 1000 * 60;

    // This causes notifications to go off more frequently on debug builds.
    private static final boolean FREQUENT_NOTIFICATION_DEBUG_MODE = false;

    private static final int NOTIFICATION_REQUEST_CODE = 0;

    public static void setNotificationTime(Context context, int minutes) {
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.HOUR_OF_DAY, minutes / 60);
        calendar.set(Calendar.MINUTE, minutes % 60);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long intervalMillis = MS_PER_MIN * 60 * 24;
        if (BuildConfig.DEBUG && FREQUENT_NOTIFICATION_DEBUG_MODE) {
            intervalMillis = MS_PER_MIN * 5;
        } else {
            Calendar now = Calendar.getInstance();
            // If the time is set to before now, the even will fire immediately, which may result in an
            // extra notification. Go ahead and add a full day so it won't fire until tomorrow.
            if (calendar.before(now)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        // We try firing the alarm every day, but will only set the notification if it is one of
        // the user's selected days.
        PendingIntent pendingIntent = cancelPendingIntent(context);
        Log.d(TAG, "setting reminder");
        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE))
                .setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), intervalMillis,
                        pendingIntent);
    }

    public static void clearNotifications(Context context) {
        cancelPendingIntent(context);
    }

    private static PendingIntent cancelPendingIntent(Context context) {
        Intent intent = new Intent(context, NotifyBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, NOTIFICATION_REQUEST_CODE,
                intent, 0);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent); // Clear the old intent, if there was one.
        Log.d(TAG, "canceling previous intent");
        return pendingIntent;
    }

    public static void snoozeNotification(Context context) {
        Intent snoozeIntent = new Intent(context, NotifyBroadcastReceiver.class);
        PendingIntent pendingSnooze = PendingIntent.getBroadcast(context, NOTIFICATION_REQUEST_CODE,
                snoozeIntent, 0);
        Calendar when = Calendar.getInstance();
        if (BuildConfig.DEBUG && FREQUENT_NOTIFICATION_DEBUG_MODE) {
            when.add(Calendar.SECOND, 30);
        } else {
            when.add(Calendar.MINUTE, 60);
        }
        Log.d(TAG, "snoozing until " + when.getTime().toString());
        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE))
                .set(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pendingSnooze);
    }
}
