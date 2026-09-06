package org.a5calls.android.a5calls.controller;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    public static NotificationSettingsDialog newInstance() {
        return new NotificationSettingsDialog();
    }

    private int mSelectedOption = 0;

    // has to be registered before the fragment starts, so it can't wait until
    // the save button is tapped. Null when the permission isn't needed.
    private ActivityResultLauncher<String> mPermissionRequest;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPermissionRequest = SettingsActivity.createNotificationPermissionRequest(
                this, isGranted -> {});
    }

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
                if (mSelectedOption == 0 && mPermissionRequest != null) {
                    mPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS);
                    // TODO(#139): Do not turn on notifications preference if they did not enable
                    // permissions.
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
