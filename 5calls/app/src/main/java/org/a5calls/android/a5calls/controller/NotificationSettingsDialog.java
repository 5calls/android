package org.a5calls.android.a5calls.controller;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

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
                SettingsActivity.updateNotificationsPreference(getActivity(),
                        AccountManager.Instance, String.format("%s", mSelectedOption));
                // TODO show a toast when done to say saved, you can change this in settings.
            }
        });

        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        AccountManager.Instance.setNotificationDialogShown(getActivity(), true);
    }
}
