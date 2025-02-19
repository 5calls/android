package org.a5calls.android.a5calls.controller;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import com.onesignal.Continue;
import com.onesignal.OneSignal;

import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;

/**
 * DialogFragment for picking notification settings.
 */
public class NotificationSettingsDialog extends DialogFragment {
    public static String TAG = "NotificationDialog";

    public static NotificationSettingsDialog newInstance() {
        return new NotificationSettingsDialog();
    }

    private int mSelectedOption = 0;

    public NotificationSettingsDialog() {

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                R.style.AppTheme_Dialog);

        builder.setTitle(R.string.notifications_dialog_title);
        builder.setSingleChoiceItems(
                getActivity().getResources().getStringArray(R.array.notification_options),
                mSelectedOption, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mSelectedOption = i;
                    }
                });
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (mSelectedOption == 0) {
                    OneSignal.getUser().getPushSubscription().optIn();
                    OneSignal.getNotifications().requestPermission(true, Continue.none());
                }
                SettingsActivity.updateNotificationsPreference(
                        (FiveCallsApplication) getActivity().getApplication(),
                        AccountManager.Instance, String.format("%s", mSelectedOption));
            }
        });

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
