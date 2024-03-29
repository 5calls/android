package org.a5calls.android.a5calls.controller;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;

import org.a5calls.android.a5calls.R;

/**
 * Dialog for letting a user enter a search term.
 */
public class SearchIssuesDialog extends DialogFragment {
    private static final String KEY_PREVIOUS_SEARCH = "previousSearch";
    public static String TAG = "SearchIssuesDialog";
    private EditText mSearchBox;

    public static SearchIssuesDialog newInstance(String previousSearch) {
        SearchIssuesDialog dialog = new SearchIssuesDialog();
        Bundle args = new Bundle();
        args.putString(KEY_PREVIOUS_SEARCH, previousSearch);
        dialog.setArguments(args);
        return dialog;
    }

    public SearchIssuesDialog() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String previousSearch = "";
        final boolean clearOnCancel;
        if (getArguments() != null) {
            previousSearch = getArguments().getString(KEY_PREVIOUS_SEARCH, "");
            // We won't clear when the user taps "cancel" if a search was pre-populated from
            // the previous search.
            clearOnCancel = TextUtils.isEmpty(previousSearch);
        } else {
            clearOnCancel = false;
        }
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_PREVIOUS_SEARCH)) {
            previousSearch = savedInstanceState.getString(KEY_PREVIOUS_SEARCH);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                R.style.AppTheme_Dialog);

        builder.setTitle(R.string.search_dialog_title);

        ViewGroup layout = (ViewGroup) LayoutInflater.from(getActivity()).inflate(
                R.layout.issue_search_box, null);
        mSearchBox = layout.findViewById(R.id.search_box);
        if (!TextUtils.isEmpty(previousSearch.trim())) {
            mSearchBox.setText(previousSearch);
            mSearchBox.selectAll();
        }
        builder.setView(layout);

        builder.setPositiveButton(R.string.menu_search, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ((MainActivity) getActivity()).onIssueSearchSet(mSearchBox.getText().toString());
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (clearOnCancel) {
                    ((MainActivity) getActivity()).onIssueSearchCleared();
                }
            }
        });

        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_PREVIOUS_SEARCH, mSearchBox.getText().toString());
        super.onSaveInstanceState(outState);
    }
}
