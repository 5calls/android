package org.a5calls.android.a5calls.model;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.a5calls.android.a5calls.BuildConfig;
import org.a5calls.android.a5calls.controller.NotifyBroadcastReceiver;

import java.util.Calendar;

/**
 * Manages scheduling notifications
 */
public class NotificationUtils {
    private static final long MS_PER_MIN = 1000 * 60;

    // This causes notifications to go off more frequently on debug builds.
    private static final boolean FREQUENT_NOTIFICATION_DEBUG_MODE = false;

    // TODO: Got to deal with daylight savings time...
    public static void setNotificationTime(Context context, int minutes) {
        Calendar calendar = Calendar.getInstance();

        // TODO: May want to add one to the day if the time is before now, because that causes
        // an sort of immediate reminder firing event.
        calendar.set(Calendar.HOUR_OF_DAY, minutes / 60);
        calendar.set(Calendar.MINUTE, minutes % 60);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long intervalMillis = MS_PER_MIN * 60 * 24;
        if (BuildConfig.DEBUG && FREQUENT_NOTIFICATION_DEBUG_MODE) {
            intervalMillis = 6000;
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
        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE))
                .setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), intervalMillis,
                        pendingIntent);
    }

    public static void clearNotifications(Context context) {
        cancelPendingIntent(context);
    }

    private static PendingIntent cancelPendingIntent(Context context) {
        Intent intent = new Intent(context, NotifyBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent); // Clear the old intent, if there was one.
        return pendingIntent;
    }
}
