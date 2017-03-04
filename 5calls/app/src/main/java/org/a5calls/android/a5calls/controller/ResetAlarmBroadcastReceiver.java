package org.a5calls.android.a5calls.controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.a5calls.android.a5calls.model.AccountManager;

/**
 * Receives boot events, package replace events and others to reset the alarm if needed
 */
public class ResetAlarmBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "ResetAlarmBroadcastRcvr";
    @Override
    public void onReceive(Context context, Intent intent) {
        // Reset the alarm
        Log.d(TAG, "5 Calls resetting the reminder on action");
        SettingsActivity.turnOnReminders(context.getApplicationContext(), AccountManager.Instance);
    }
}
