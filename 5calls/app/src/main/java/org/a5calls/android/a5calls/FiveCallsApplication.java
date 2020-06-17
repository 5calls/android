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

import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import org.a5calls.android.a5calls.controller.OneSignalNotificationController;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.NotificationUtils;

/**
 * This is a subclass of {@link Application} used to provide shared objects for this app, such as
 * the {@link Tracker}.
 */
public class FiveCallsApplication extends Application {
    private Tracker mTracker;
    private int mRunningActivities;
    private Thread.UncaughtExceptionHandler mDefaultHandler;

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
        // We could automatically report all exceptions with an XML change to the configuration file
        // but that wouldn't respect the user's setting to share data with 5 calls.
        if (AccountManager.Instance.allowAnalytics(getApplicationContext())) {
            enableAnalyticsHandler();
        }

        // Set up OneSignal.
        OneSignalNotificationController.setUp(this);

    }

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker("fake");
//            mTracker = analytics.newTracker(R.xml.global_tracker);
        }
        return mTracker;
    }

    public boolean isRunning() {
        return mRunningActivities > 0;
    }

    public void enableAnalyticsHandler() {
        if (mDefaultHandler == null) {
            mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        }
        Thread.UncaughtExceptionHandler analyticsHandler = new ExceptionReporter(
                getDefaultTracker(),
                Thread.getDefaultUncaughtExceptionHandler(),
                getApplicationContext());

        // Make myHandler the new default uncaught exception handler.
        Thread.setDefaultUncaughtExceptionHandler(analyticsHandler);
    }

    public void disableAnalyticsHandler() {
        if (mDefaultHandler != null) {
            // In this case, Analytics handler was enabled, so disable it.
            Thread.setDefaultUncaughtExceptionHandler(mDefaultHandler);
        }
    }
}
