package org.a5calls.android.a5calls.controller;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.android.gms.common.util.Strings;

import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;

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
        if (getArguments() != null) {
            previousSearch = getArguments().getString(KEY_PREVIOUS_SEARCH, "");
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
        if (!Strings.isEmptyOrWhitespace(previousSearch)) {
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
                ((MainActivity) getActivity()).onIssueSearchCleared();
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
