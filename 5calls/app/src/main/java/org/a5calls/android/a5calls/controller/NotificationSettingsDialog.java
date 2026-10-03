package org.a5calls.android.a5calls.controller;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;


import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;

/**
 * DialogFragment for picking notification settings.
 */
public class NotificationSettingsDialog extends DialogFragment {
    public static String TAG = "NotificationDialog";

    /**
     * Implemented by the activity showing this dialog. The dialog closes before the permission
     * prompt is answered, so the activity has to be the one asking.
     */
    public interface Host {
        void enablePushNotifications();
    }

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
                    ((Host) requireActivity()).enablePushNotifications();
                } else {
                    SettingsActivity.updateNotificationsPreference(
                            (FiveCallsApplication) getActivity().getApplication(),
                            AccountManager.Instance, String.format("%s", mSelectedOption));
                }
            }
        });

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
