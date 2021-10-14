package org.a5calls.android.a5calls.controller;

//import com.google.android.gms.analytics.HitBuilders;
//import com.google.android.gms.analytics.Tracker;
import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OneSignal;

import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.model.AccountManager;

/**
 * Communicates with OneSignal about notification settings.
 */
public class OneSignalNotificationController {

    public static void setUp(final FiveCallsApplication application) {
        OneSignal.startInit(application)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .setNotificationOpenedHandler(new OneSignal.NotificationOpenedHandler() {
                    @Override
                    public void notificationOpened(OSNotificationOpenResult result) {
                        // Check whether notifications are allowed when the item is clicked.
                        if (AccountManager.Instance.allowAnalytics(
                                application.getApplicationContext())) {
//                            Tracker tracker = application.getDefaultTracker();
//                            tracker.send(new HitBuilders.EventBuilder()
//                                    .setCategory("Notifications")
//                                    .setAction("LaunchFromNotification")
//                                    .setValue(1)
//                                    .build());
                        }
                    }
                })
                .disableGmsMissingPrompt(true) // We won't worry about out-of-date GMS
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();
        // Disable notifications if we haven't prompted the user about them yet.
        if (!AccountManager.Instance.isNotificationDialogShown(application)) {
            OneSignal.setSubscription(false);
        }
    }

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
