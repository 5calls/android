package org.a5calls.android.a5calls;

import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

/**
 * The activity which handles zip code lookup and showing the issues list.
 *
 * TODO: Add a TutorialActivity which shows the "about" information to first-time users, or similar?
 * TODO: Add a counter for calls this user has made, stored in prefs or something. Personal stats!
 *       This includes keeping track of which reps a user has called for which issues, so that we
 *       don't need to have them call those reps again.
 *       Maybe. What's the best user flow here?
 *       Then add a "personal stats" activity that shows this information.
 *       A database might be easier than SharedPrefs here.
 * TODO: Add an email address sign-up field.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int ISSUE_DETAIL_REQUEST = 1;

    private JsonController mJsonController;
    private IssuesAdapter mIssuesAdapter;
    private String mZip;

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
        Button zipButton = (Button) findViewById(R.id.zip_code_submit);
        zipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitZip();
            }
        });

        Button editZipButton = (Button) findViewById(R.id.zip_code_edit);
        editZipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateZipUi(true, 0);
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

                    @Override
                    public void onCallCount(int count) {
                        // unused
                    }

                    @Override
                    public void onCallReported() {
                        // unused
                    }
                });
    }

    @Override
    protected void onDestroy() {
        mJsonController.onDestroy();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void submitZip() {
        EditText zipEdit = (EditText) findViewById(R.id.zip_code);
        String code = zipEdit.getText().toString();
        // Is it a string that is exactly 5 characters long?
        if (TextUtils.isEmpty(code) || code.length() != 5) {
            zipEdit.setError(getResources().getString(R.string.zip_error));
            return;
        }
        int zip = 0;
        try {
            // Make sure it is a number, too, by trying to parse it.
            zip = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            zipEdit.setError(getResources().getString(R.string.zip_error));
            return;
        }
        // If we made it here, the zip is valid! Update the UI and send the request.
        mJsonController.getIssuesForZip(code);
        mZip = code;

        // TODO: Update the UI to show the zip code we've requested for with less vertical space
        // usage. And have a button to edit the zip.
        updateZipUi(false, zip);
    }

    private void updateZipUi(boolean showEditZip, int zip) {
        findViewById(R.id.zip_code_submit).setVisibility(showEditZip ? View.VISIBLE : View.GONE);
        findViewById(R.id.zip_code_edit).setVisibility(showEditZip ? View.GONE : View.VISIBLE);
        findViewById(R.id.zip_code_prompt).setVisibility(showEditZip ? View.VISIBLE : View.GONE);
        findViewById(R.id.zip_code).setVisibility(showEditZip ? View.VISIBLE : View.GONE);
        TextView repsFor = (TextView) findViewById(R.id.included_reps_for);
        repsFor.setVisibility(showEditZip ? View.GONE : View.VISIBLE);
        if (!showEditZip) {
            repsFor.setText(String.format(getResources().getString(R.string.reps_for_zip), zip));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ISSUE_DETAIL_REQUEST) {
            // TODO: Send back the issue as data in the intent, but with updates about calls made.
            // TODO: Update the server if anything changed.
            // TODO: Update the adapter if anything changed.
            Issue issue = data.getExtras().getParcelable(IssueActivity.KEY_ISSUE);
            mIssuesAdapter.updateIssue(issue);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class IssuesAdapter extends RecyclerView.Adapter<IssueViewHolder> {
        private List<Issue> mIssues = Collections.emptyList();

        public IssuesAdapter() {

        }

        public void setIssues(List<Issue> issues) {
            mIssues = issues;
            notifyDataSetChanged();
        }

        public void updateIssue(Issue issue) {
            for (int i = 0; i < mIssues.size(); i++) {
                if (TextUtils.equals(issue.id, mIssues.get(i).id)) {
                    mIssues.set(i, issue);
                    notifyItemChanged(i);
                    return;
                }
            }
        }

        @Override
        public IssueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RelativeLayout v = (RelativeLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.issue_view, parent, false);
            IssueViewHolder vh = new IssueViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(final IssueViewHolder holder, int position) {
            final Issue issue = mIssues.get(position);
            holder.name.setText(issue.name);
            if (issue.contacts.length == 1) {
                holder.numCalls.setText(getResources().getString(R.string.call_count_one));
            } else {
                holder.numCalls.setText(String.format(getResources().getString(R.string.call_count),
                        issue.contacts.length));
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent issueIntent = new Intent(holder.itemView.getContext(),
                            IssueActivity.class);
                    issueIntent.putExtra(IssueActivity.KEY_ISSUE, issue);
                    issueIntent.putExtra(IssueActivity.KEY_ZIP, mZip);
                    startActivityForResult(issueIntent, ISSUE_DETAIL_REQUEST);
                }
            });
            // TODO: If all contacts are done, update the UI for this issue.
        }

        @Override
        public void onViewRecycled(IssueViewHolder holder) {
            holder.itemView.setOnClickListener(null);
            super.onViewRecycled(holder);
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
