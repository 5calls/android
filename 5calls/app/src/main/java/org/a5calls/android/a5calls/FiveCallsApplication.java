package org.a5calls.android.a5calls;

/*
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;

import com.onesignal.OneSignal;

import org.a5calls.android.a5calls.controller.SettingsActivity;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.NotificationUtils;
import org.a5calls.android.a5calls.util.AnalyticsManager;

import java.util.UUID;

/**
 * This is a subclass of {@link Application} used to provide shared objects for this app.
 */
public class FiveCallsApplication extends Application {
    private int mRunningActivities;
    public static AnalyticsManager analyticsManager = new AnalyticsManager();

    private static final String ONESIGNAL_APP_ID = "5fd4ca41-9f6c-4149-a312-ae3e71b35c0e";

    public FiveCallsApplication() {
        super();
        mRunningActivities = 0;
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {
                mRunningActivities++;
            }

            @Override
            public void onActivityResumed(Activity activity) {
                // Clear reminder notifications whenever the app is re-opened.
                NotificationUtils.clearNotifications(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {
                mRunningActivities--;
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize analytics
        analyticsManager.init(this);

        // Set up OneSignal.
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);

        String callerID = AccountManager.Instance.getCallerID(this);
        if (callerID.isEmpty()) {
            callerID = UUID.randomUUID().toString();
            AccountManager.Instance.setCallerID(this, callerID);
        }
        OneSignal.login(callerID);

        // Check if notification permission has been revoked outside of the app since the last run
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            // Reset the notifications preference
            SettingsActivity.updateNotificationsPreference(
                    this,
                    AccountManager.Instance,
                    AccountManager.DEFAULT_NOTIFICATION_SELECTION
            );
        }
    }

    public boolean isRunning() {
        return mRunningActivities > 0;
    }
}
