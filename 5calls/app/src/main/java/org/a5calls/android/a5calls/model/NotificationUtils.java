package org.a5calls.android.a5calls.model;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.a5calls.android.a5calls.controller.MainActivity;
import org.a5calls.android.a5calls.controller.NotifyBroadcastReceiver;

import java.util.Calendar;

/**
 * Manages scheduling notifications
 */
public class NotificationUtils {
    private static final long MS_PER_MIN = 1000 * 60;

    public static void setNotificationTime(Context context, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, minutes / 60);
        calendar.set(Calendar.MINUTE, minutes % 60);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        // Note, if the calendar is in the past, then it just fires now and starts at the right
        // time next time.
        long dayMillis = MS_PER_MIN * 60 * 24;

        PendingIntent pendingIntent = cancelPendingIntent(context);
        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE))
                .setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), dayMillis,
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
