package org.a5calls.android.a5calls;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

/**
 * The activity which handles zip code lookup and showing the issues list.
 *
 * TODO: Add loading spinners when making Volley requests.
 * TODO: Add error message if the device is offline?
 * TODO: Add full "personal stats" DialogFragment that shows lots of information.
 * TODO: Add an email address sign-up field.
 * TODO: Sort issues based on which are "done" and which are not done or hide ones which are "done".
 * TODO: After making a call, jump the screen up to show the next contact!
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final String PREFS_FILE = "fiveCallsPrefs";
    public static final String KEY_INITIALIZED = "prefsKeyInitialized";
    private static final String KEY_USER_ZIP = "prefsKeyUserZip";
    private static final int ISSUE_DETAIL_REQUEST = 1;

    private IssuesAdapter mIssuesAdapter;
    private JsonController.RequestStatusListener mStatusListener;
    private String mZip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Consider using fragments
        super.onCreate(savedInstanceState);

        // See if we've had this user before. If not, start them at tutorial type page.
        SharedPreferences pref = getSharedPreferences(PREFS_FILE, 0);
        boolean initialized = pref.getBoolean(KEY_INITIALIZED, false);
        if (!initialized) {
            Intent intent = new Intent(this, TutorialActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // TODO: Use SavedInstanceState first, in case they were editing the zip or something.
        // Load the zip code the user last used, if any.
        String code = pref.getString(KEY_USER_ZIP, "");
        if (!TextUtils.isEmpty(code)) {
            onZipUpdated(code);
        }

        // TODO: Option to get user's location from GPS instead of just entering a zip code.
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
                updateZipUi(true, null);
            }
        });

        RecyclerView issuesRecyclerView = (RecyclerView) findViewById(R.id.issues_recycler_view);
        issuesRecyclerView.setNestedScrollingEnabled(false);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        issuesRecyclerView.setLayoutManager(layoutManager);
        mIssuesAdapter = new IssuesAdapter();
        issuesRecyclerView.setAdapter(mIssuesAdapter);

        mStatusListener = new JsonController.RequestStatusListener() {
            @Override
            public void onRequestError() {
                Snackbar.make(findViewById(R.id.activity_main),
                        getResources().getString(R.string.request_error),
                        Snackbar.LENGTH_LONG).show();
                // Our only type of request in MainActivity is a GET. If it doesn't work, clear the
                // active issues list to avoid showing a stale list.
                mIssuesAdapter.setIssues(Collections.<Issue>emptyList());
            }

            @Override
            public void onJsonError() {
                Snackbar.make(findViewById(R.id.activity_main),
                        getResources().getString(R.string.json_error),
                        Snackbar.LENGTH_LONG).show();
                // Our only type of request in MainActivity is a GET. If it doesn't work, clear the
                // active issues list to avoid showing a stale list.
                mIssuesAdapter.setIssues(Collections.<Issue>emptyList());
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
        };
        AppSingleton.getInstance(getApplicationContext()).getJsonController()
                .registerStatusListener(mStatusListener);
    }

    @Override
    protected void onDestroy() {
        AppSingleton.getInstance(getApplicationContext()).getJsonController()
                .unregisterStatusListener(mStatusListener);
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
        } else if (item.getItemId() == R.id.menu_stats) {
            showStats();
        } else if (item.getItemId() == R.id.menu_refresh) {
            onZipUpdated(mZip);
        }
        return super.onOptionsItemSelected(item);
    }

    private void showStats() {
        // TODO: Show the stats in a DialogFragment or even an activity.
        int callsCount = AppSingleton.getInstance(getApplicationContext()).getDatabaseHelper()
                .getCallsCount();
        String message = String.format(getResources().getString(R.string.stats_message),
                callsCount);
        Snackbar.make(findViewById(R.id.activity_main), message, Snackbar.LENGTH_LONG).show();
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
            Integer.parseInt(code);
        } catch (NumberFormatException e) {
            zipEdit.setError(getResources().getString(R.string.zip_error));
            return;
        }
        // If we made it here, the zip is valid! Update the UI and send the request.
        onZipUpdated(code);
    }

    private void onZipUpdated(String code) {
        AppSingleton.getInstance(getApplicationContext()).getJsonController().getIssuesForZip(code);
        mZip = code;

        SharedPreferences pref = getSharedPreferences(PREFS_FILE, 0);
        pref.edit().putString(KEY_USER_ZIP, code).apply();

        // Update the UI to show the zip code we've requested for with less vertical space
        // usage. And have a button to edit the zip.
        updateZipUi(false, code);
    }

    private void updateZipUi(boolean showEditZip, String zip) {
        findViewById(R.id.zip_code_edit).setVisibility(showEditZip ? View.GONE : View.VISIBLE);
        TextView repsFor = (TextView) findViewById(R.id.included_reps_for);
        repsFor.setVisibility(showEditZip ? View.GONE : View.VISIBLE);
        if (!showEditZip) {
            repsFor.setText(String.format(getResources().getString(R.string.reps_for_zip), zip));
            // TODO: Hide the keyboard if it is visible.
        }
        findViewById(R.id.zip_code_submit).setVisibility(showEditZip ? View.VISIBLE : View.GONE);
        findViewById(R.id.zip_code_prompt).setVisibility(showEditZip ? View.VISIBLE : View.GONE);
        findViewById(R.id.zip_code).setVisibility(showEditZip ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ISSUE_DETAIL_REQUEST && resultCode == RESULT_OK) {
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
            int totalCalls = issue.contacts.length;
            List<String> contacted = AppSingleton.getInstance(getApplicationContext())
                    .getDatabaseHelper().getCallsForIssueAndZip(issue.id, mZip);
            int callsLeft = totalCalls - contacted.size();
            if (callsLeft == totalCalls) {
                if (totalCalls == 1) {
                    holder.numCalls.setText(getResources().getString(R.string.call_count_one));
                } else {
                    holder.numCalls.setText(String.format(
                            getResources().getString(R.string.call_count), totalCalls));
                }
            } else {
                holder.numCalls.setText(String.format(
                        getResources().getString(R.string.call_count_remaining), callsLeft,
                        totalCalls));
            }
            holder.doneIcon.setVisibility(callsLeft == 0 ? View.VISIBLE : View.GONE);
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
        public ImageView doneIcon;

        public IssueViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.issue_name);
            numCalls = (TextView) itemView.findViewById(R.id.issue_call_count);
            doneIcon = (ImageView) itemView.findViewById(R.id.issue_done_img);
        }
    }
}
