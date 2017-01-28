package org.a5calls.android.a5calls;

import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private JsonController mJsonController;
    private IssuesAdapter mIssuesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Consider using fragments
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: Option to get user's location from GPS instead of just entering a zip code
        EditText zipEdit = (EditText) findViewById(R.id.zip_code);
        zipEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submitZip();
                    return true;
                }
                return false;
            }
        });
        final Button zipButton = (Button) findViewById(R.id.zip_code_submit);
        zipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitZip();
            }
        });

        RecyclerView issuesRecyclerView = (RecyclerView) findViewById(R.id.issues_recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        issuesRecyclerView.setLayoutManager(layoutManager);
        mIssuesAdapter = new IssuesAdapter();
        issuesRecyclerView.setAdapter(mIssuesAdapter);

        mJsonController = new JsonController(getApplicationContext(),
                new JsonController.RequestStatusListener() {
                    @Override
                    public void onRequestError() {
                        Snackbar.make(findViewById(R.id.activity_main),
                                getResources().getString(R.string.request_error),
                                Snackbar.LENGTH_LONG).show();
                    }

                    @Override
                    public void onJsonError() {
                        Snackbar.make(findViewById(R.id.activity_main),
                                getResources().getString(R.string.json_error),
                                Snackbar.LENGTH_LONG).show();
                    }

                    @Override
                    public void onIssuesReceived(List<Issue> issues) {
                        mIssuesAdapter.setIssues(issues);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        mJsonController.onDestroy();
        super.onDestroy();
    }

    private void submitZip() {
        EditText zipEdit = (EditText) findViewById(R.id.zip_code);
        String code = zipEdit.getText().toString();
        // Is it a string that is exactly 5 characters long?
        if (TextUtils.isEmpty(code) || code.length() != 5) {
            zipEdit.setError(getResources().getString(R.string.zip_error));
            return;
        }
        try {
            // Make sure it is a number, too, by trying to parse it.
            int unused = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            zipEdit.setError(getResources().getString(R.string.zip_error));
            return;
        }
        // If we made it here, the zip is valid! Update the UI and send the request.
        mJsonController.getIssuesForZip(code);

        // TODO: Update the UI to show the zip code we've requested for with less vertical space
        // usage. And have a button to edit the zip.
    }

    private class IssuesAdapter extends RecyclerView.Adapter<IssueViewHolder> {
        private List<Issue> mIssues = Collections.emptyList();

        public IssuesAdapter() {

        }

        public void setIssues(List<Issue> issues) {
            mIssues = issues;
            notifyDataSetChanged();
        }

        @Override
        public IssueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RelativeLayout v = (RelativeLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.issue_view, parent, false);
            IssueViewHolder vh = new IssueViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(IssueViewHolder holder, int position) {
            Issue issue = mIssues.get(position);
            holder.name.setText(issue.name);
            holder.numCalls.setText(String.format(getResources().getString(R.string.call_count),
                    issue.contacts.length));
            // TODO: Set click listeners and more params.
        }

        @Override
        public int getItemCount() {
            return mIssues.size();
        }
    }

    private class IssueViewHolder extends RecyclerView.ViewHolder {
        public TextView name;
        public TextView numCalls;
        public IssueViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.issue_name);
            numCalls = (TextView) itemView.findViewById(R.id.issue_call_count);
        }
    }
}
