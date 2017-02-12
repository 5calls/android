package org.a5calls.android.a5calls.controller;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.DatabaseHelper;
import org.a5calls.android.a5calls.model.FiveCallsApi;
import org.a5calls.android.a5calls.model.Issue;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * The activity which handles zip code lookup and showing the issues list.
 * 
 * TODO: Add an email address sign-up field.
 * TODO: Sort issues based on which are "done" and which are not done or hide ones which are "done".
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int ISSUE_DETAIL_REQUEST = 1;
    public static final int NOTIFICATION_REQUEST = 2;
    public static final String EXTRA_FROM_NOTIFICATION = "extraFromNotification";
    private final AccountManager accountManager = AccountManager.Instance;

    private IssuesAdapter mIssuesAdapter;
    private FiveCallsApi.RequestStatusListener mStatusListener;
    private String mZip;
    private String mLatitude;
    private String mLongitude;

    @BindView(R.id.swipe_container) SwipeRefreshLayout swipeContainer;
    @BindView(R.id.issues_recycler_view) RecyclerView issuesRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Consider using fragments
        super.onCreate(savedInstanceState);

        // See if we've had this user before. If not, start them at tutorial type page.
        if (!accountManager.isTutorialSeen(this)) {
            Intent intent = new Intent(this, TutorialActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Load the location the user last used, if any.
        if (!accountManager.hasLocation(this)) {
            // No location set, go to LocationActivity!
            Intent intent = new Intent(this, LocationActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        if (!accountManager.isRemindersInfoShown(this)) {
            // We haven't yet told the user that reminders exist, they probably upgraded to get here
            // instead of learning about it in the tutorial. Give a dialog explaining more.
            DialogFragment fragment = NewSettingsDialog.newInstance(R.string.reminders_dialog_title,
                    R.string.reminders_dialog_content);
            getSupportFragmentManager().beginTransaction().add(fragment, NewSettingsDialog.TAG)
                    .commit();
            accountManager.setRemindersInfoShown(this, true);
            SettingsActivity.turnOnReminders(this, accountManager);
        }

        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null &&
                intent.getExtras().getBoolean(EXTRA_FROM_NOTIFICATION, false)) {
            if (accountManager.allowAnalytics(this)) {
                // Obtain the shared Tracker instance.
                FiveCallsApplication application = (FiveCallsApplication) getApplication();
                Tracker tracker = application.getDefaultTracker();
                tracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Reminders")
                        .setAction("LaunchFromReminder")
                        .setValue(1)
                        .build());
            }
        }

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        issuesRecyclerView.setLayoutManager(layoutManager);
        mIssuesAdapter = new IssuesAdapter();
        issuesRecyclerView.setAdapter(mIssuesAdapter);

        registerApiListener();

        swipeContainer.setColorSchemeResources(R.color.colorPrimary);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override public void onRefresh() {
                refreshIssues();
            }
        });
    }

    @Override
    protected void onDestroy() {
        AppSingleton.getInstance(getApplicationContext()).getJsonController()
                .unregisterStatusListener(mStatusListener);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mZip = accountManager.getZip(this);
        mLatitude = accountManager.getLat(this);
        mLongitude = accountManager.getLng(this);

        // Refresh on resume.  The post is necessary to start the spinner animation.
        swipeContainer.post(new Runnable() {
            @Override public void run() {
                swipeContainer.setRefreshing(true);
                refreshIssues();
            }
        });

        // We allow Analytics opt-out.
        if (accountManager.allowAnalytics(this)) {
            // Obtain the shared Tracker instance.
            FiveCallsApplication application = (FiveCallsApplication) getApplication();
            Tracker tracker = application.getDefaultTracker();
            tracker.setScreenName(TAG);
            tracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
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
            return true;
        } else if (item.getItemId() == R.id.menu_refresh) {
            swipeContainer.post(new Runnable() {
                @Override public void run() {
                    swipeContainer.setRefreshing(true);
                    refreshIssues();
                }
            });
            return true;
        } else if (item.getItemId() == R.id.menu_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_location) {
            Intent intent = new Intent(this, LocationActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void registerApiListener() {
        mStatusListener = new FiveCallsApi.RequestStatusListener() {
            @Override
            public void onRequestError() {
                Snackbar.make(findViewById(R.id.activity_main),
                        getResources().getString(R.string.request_error),
                        Snackbar.LENGTH_LONG).show();
                // Our only type of request in MainActivity is a GET. If it doesn't work, clear the
                // active issues list to avoid showing a stale list.
                mIssuesAdapter.setIssues(Collections.<Issue>emptyList());
                swipeContainer.setRefreshing(false);
            }

            @Override
            public void onJsonError() {
                Snackbar.make(findViewById(R.id.activity_main),
                        getResources().getString(R.string.json_error),
                        Snackbar.LENGTH_LONG).show();
                // Our only type of request in MainActivity is a GET. If it doesn't work, clear the
                // active issues list to avoid showing a stale list.
                mIssuesAdapter.setIssues(Collections.<Issue>emptyList());
                swipeContainer.setRefreshing(false);
            }

            @Override
            public void onIssuesReceived(String locationName, List<Issue> issues) {
                getSupportActionBar().setTitle(String.format(getResources().getString(
                        R.string.title_main), locationName));
                // If this is the first time we've set issues ever, add all the contacts and
                // issue IDs to the database to keep track of things the user has already called
                // about. This allows upgrade from app version 0.07 without "unknown" contacts or
                // issues in stats.
                // Note: If the user has made calls in other locations, then we won't be saving
                // the issues and contacts to the DB from those locations. However, that's probably
                // OK.
                // TODO: Remove this extra save when all users are upgraded past version 0.07.
                if (!AccountManager.Instance.getDatabaseSavesContacts(getApplicationContext())) {
                    AppSingleton.getInstance(getApplicationContext()).getDatabaseHelper()
                            .saveIssuesToDatabaseForUpgrade(issues);
                    AccountManager.Instance.setDatabaseSavesContacts(getApplicationContext(), true);
                }
                if (TextUtils.isEmpty(locationName)) {
                    locationName = getResources().getString(R.string.unknown_location);
                }
                mIssuesAdapter.setIssues(issues);
                swipeContainer.setRefreshing(false);
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

    private void showStats() {
        Intent intent = new Intent(this, StatsActivity.class);
        startActivity(intent);
    }

    private void refreshIssues() {
        if (!TextUtils.isEmpty(mLatitude) && !TextUtils.isEmpty(mLongitude)) {
            AppSingleton.getInstance(getApplicationContext()).getJsonController()
                    .getIssuesForLocation(mLatitude + "," + mLongitude);

        } else if (!TextUtils.isEmpty(mZip)) {
            AppSingleton.getInstance(getApplicationContext()).getJsonController()
                    .getIssuesForLocation(mZip);
        } else {
            String message = getString(R.string.main_activity_location_error);
            Snackbar.make(findViewById(R.id.activity_main), message, Snackbar.LENGTH_LONG).show();
        }
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
            CardView v = (CardView) LayoutInflater.from(parent.getContext())
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
                    .getDatabaseHelper().getCallsForIssueAndContacts(issue.id, issue.contacts);
            int callsLeft = totalCalls - contacted.size();
            if (callsLeft == totalCalls) {
                if (totalCalls == 1) {
                    holder.numCalls.setText(getResources().getString(R.string.call_count_one));
                } else {
                    holder.numCalls.setText(String.format(
                            getResources().getString(R.string.call_count), totalCalls));
                }
            } else {
                if (callsLeft == 1) {
                    holder.numCalls.setText(String.format(
                            getResources().getString(R.string.call_count_remaining_one),
                            totalCalls));
                } else {
                    holder.numCalls.setText(String.format(
                            getResources().getString(R.string.call_count_remaining), callsLeft,
                            totalCalls));
                }
            }
            holder.doneIcon.setImageLevel(callsLeft == 0 ? 1 : 0);
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
