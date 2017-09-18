package org.a5calls.android.a5calls.controller;

import com.onesignal.OneSignal;

/**
 * Communicates with OneSignal about notification settings.
 */
public class OneSignalNotificationController {
    public static void enableAllNotifications() {
        OneSignal.setSubscription(true);
        OneSignal.sendTag("all", "all");
    }

    public static void enableTopNotifications() {
        OneSignal.setSubscription(true);
        OneSignal.deleteTag("all");
    }

    public static void disableNotifications() {
        OneSignal.setSubscription(false);
    }
}
