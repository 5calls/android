package org.a5calls.android.a5calls.controller;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import org.a5calls.android.a5calls.R;

/**
 * DialogFragment for when settings change and need to be shown to the user.
 */
public class NewSettingsDialog extends DialogFragment {
    public static String TAG = "NewSettingsDialog";

    private static String KEY_TITLE_STRING_ID = "keyTitleStringId";
    private static String KEY_CONTENT_STRING_ID = "keyContentsStringId";

    public static NewSettingsDialog newInstance(int titleStringId, int contentStringId) {
        Bundle args = new Bundle();
        args.putInt(KEY_TITLE_STRING_ID, titleStringId);
        args.putInt(KEY_CONTENT_STRING_ID, contentStringId);
        NewSettingsDialog fragment = new NewSettingsDialog();
        fragment.setArguments(args);
        return fragment;
    }

    public NewSettingsDialog() {

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                R.style.AppTheme_Dialog);
        builder.setMessage(getArguments().getInt(KEY_CONTENT_STRING_ID))
                .setTitle(getArguments().getInt(KEY_TITLE_STRING_ID))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        onCancel(dialog);
                    }
                })
                .setNegativeButton(R.string.go_to_settings, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(getActivity(), SettingsActivity.class);
                        startActivity(intent);
                        onCancel(dialog);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
