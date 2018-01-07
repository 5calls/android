package org.a5calls.android.a5calls.controller;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.authentication.storage.CredentialsManagerException;
import com.auth0.android.callback.BaseCallback;
import com.auth0.android.provider.AuthCallback;
import com.auth0.android.result.Credentials;
import com.auth0.android.result.UserProfile;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.AuthenticationManager;
import org.a5calls.android.a5calls.model.Category;
import org.a5calls.android.a5calls.model.User;
import org.a5calls.android.a5calls.net.FiveCallsApi;
import org.a5calls.android.a5calls.model.Issue;
import org.a5calls.android.a5calls.util.CustomTabsUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private static final String KEY_FILTER_ITEM_SELECTED = "filterItemSelected";
    private final AccountManager accountManager = AccountManager.Instance;

    private ArrayAdapter<String> mFilterAdapter;
    private String mFilterText = "";
    private IssuesAdapter mIssuesAdapter;
    private FiveCallsApi.IssuesRequestListener mIssuesRequestListener;
    private String mAddress;
    private String mLatitude;
    private String mLongitude;

    @BindView(R.id.swipe_container) SwipeRefreshLayout swipeContainer;
    @BindView(R.id.issues_recycler_view) RecyclerView issuesRecyclerView;
    @BindView(R.id.drawer_layout) DrawerLayout drawerLayout;
    @BindView(R.id.toolbar) Toolbar actionBar;
    @BindView(R.id.nav_view) NavigationView navigationView;
    @BindView(R.id.collapsing_toolbar) CollapsingToolbarLayout collapsingToolbarLayout;
    @BindView(R.id.action_bar_subtitle) TextView actionBarSubtitle;
    @BindView(R.id.filter) AppCompatSpinner filter;

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
            // This should only happen for users upgrading from a pretty old version of the app.
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

        setSupportActionBar(actionBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
        }

        if (navigationView != null) {
            setupDrawerContent(navigationView);
        }

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        issuesRecyclerView.setLayoutManager(layoutManager);
        DividerItemDecoration divider = new DividerItemDecoration(this, RecyclerView.VERTICAL);
        issuesRecyclerView.addItemDecoration(divider);
        mIssuesAdapter = new IssuesAdapter();
        issuesRecyclerView.setAdapter(mIssuesAdapter);

        mFilterAdapter = new ArrayAdapter<>(this, R.layout.filter_item);
        mFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFilterAdapter.addAll(getResources().getStringArray(R.array.default_filters));
        filter.setAdapter(mFilterAdapter);
        if (savedInstanceState != null) {
            mFilterText = savedInstanceState.getString(KEY_FILTER_ITEM_SELECTED);
        } else {
            mFilterText = mFilterAdapter.getItem(0);  // Default value
        }

        registerApiListener();
        tryLoggingIn();

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
                .unregisterIssuesRequestListener(mIssuesRequestListener);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadStats();

        mAddress = accountManager.getAddress(this);
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
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_FILTER_ITEM_SELECTED, mFilterText);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }
        else if (item.getItemId() == R.id.menu_refresh) {
            swipeContainer.post(new Runnable() {
                @Override public void run() {
                    swipeContainer.setRefreshing(true);
                    refreshIssues();
                }
            });
            return true;
        } else if (item.getItemId() == R.id.menu_location) {
            launchLocationActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem item) {
                        //menuItem.setChecked(true); // don't use this atm
                        drawerLayout.closeDrawers();

                        if (item.getItemId() == R.id.menu_about) {
                            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                            startActivity(intent);
                            return true;
                        } else if (item.getItemId() == R.id.menu_stats) {
                            showStats();
                            return true;
                        } else if (item.getItemId() == R.id.menu_settings) {
                            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                            startActivity(intent);
                            return true;
                        } else if (item.getItemId() == R.id.menu_faq) {
                            CustomTabsUtil.launchUrl(MainActivity.this,
                                    Uri.parse(getString(R.string.faq_url)));
                        } else if (item.getItemId() == R.id.menu_login) {
                            login();
                            return true;
                        } else if (item.getItemId() == R.id.menu_logout) {
                            logout();
                            return true;
                        }

                        return true;
                    }
                });
    }

    // Try logging in in the background.
    private void tryLoggingIn() {
        final AuthenticationManager authManager = AppSingleton.getInstance(getApplicationContext())
                .getAuthenticationManager();
        if (!authManager.hasSavedCredentials()) {
            // Can't log in in the background -- no credentials exist yet.
            updateLoginUi(false);
            return;
        }
        User user = authManager.getCachedUserProfile(getApplicationContext());
        if (user != null) {
            // We've already got a cached user profile. Use this one, but still try to log in
            // again in the background in case something has changed.
            onLoginSuccess(user);
        }
        // Log in in the background.
        // If this doesn't work, we shouldn't log the user back out. Instead, we should keep
        // the cached state and just not update the server until connection is re-established.
        authManager.loginWithSavedCredentials(new AuthenticationManager.BackgroundLoginCallback() {
            @Override
            public void onCredentialsFailure(CredentialsManagerException error) {
                // Show an error like "can't connect to the server right now to log in,
                // data will not be backed up". And maybe a "try again" button.
                // May need to make sure this doesn't overlap with any other "failed to connect"
                // snackbars.
                Log.d("Main::tryLoggingIn", error.toString());
            }
        }, new BaseCallback<UserProfile, AuthenticationException>() {
            @Override
            public void onSuccess(UserProfile payload) {
                final User user = new User(payload);
                authManager.cacheUserProfile(getApplicationContext(), user);
                runOnUiThread(new Runnable() {

                    public void run() {
                        onLoginSuccess(user);
                    }
                });
            }

            @Override
            public void onFailure(AuthenticationException error) {
                // Show an error like "can't connect to the server right now to log in,
                // data will not be backed up". And maybe a "try again" button.
                // May need to make sure this doesn't overlap with any other "failed to connect"
                // snackbars.
                Log.d("Main::tryLoggingIn", error.toString());
            }
        });
    }

    // Shows/hides the login buttons
    private void updateLoginUi(boolean isLoggedIn) {
        navigationView.getMenu().findItem(R.id.menu_login).setVisible(!isLoggedIn);
        navigationView.getMenu().findItem(R.id.menu_logout).setVisible(isLoggedIn);
    }

    // Login from a totally logged out state.
    // TODO: If this succeeds, we need to back up user stats or sync stats with the server.
    private void login() {
        final AuthenticationManager authManager = AppSingleton.getInstance(getApplicationContext())
                .getAuthenticationManager();
        authManager.doLogin(this, new AuthCallback() {
            @Override
            public void onFailure(@NonNull Dialog dialog) {
                dialog.show();
            }

            @Override
            public void onFailure(AuthenticationException exception) {
                // TODO: Show exception to user.
                Log.d("MainActivity::login", exception.toString());
            }

            @Override
            public void onSuccess(@NonNull Credentials credentials) {
                // Save and then use new credentials to try logging in.
                authManager.onLogin(credentials);
                authManager.getUserInfo(credentials,
                        new BaseCallback<UserProfile, AuthenticationException>() {

                            @Override
                            public void onFailure(AuthenticationException error) {
                                // Maybe give the user a message and show the login button.
                                // Delete current credentials and try again. Since the user
                                // hasn't fully logged in yet, it's ok to delete before we
                                // sync.
                                logout();
                                Log.d("MainActivity", error.getCode() + ", " +
                                        error.getDescription());
                            }

                            @Override
                            public void onSuccess(UserProfile payload) {
                                final User user = new User(payload);
                                authManager.cacheUserProfile(getApplicationContext(), user);
                                runOnUiThread(new Runnable() {

                                    public void run() {
                                        onLoginSuccess(user);
                                    }
                                });
                            }
                        });
            }
        });
    }

    private void onLoginSuccess(User user) {
        // Show logout button
        updateLoginUi(true);

    }

    private void logout() {
        // TODO: Show the user a dialog that logging out (will? will not?) clear their local data.
        AuthenticationManager authManager = AppSingleton.getInstance(getApplicationContext())
                .getAuthenticationManager();
        authManager.removeAccount(getApplicationContext());
        updateLoginUi(false);
    }

    private void launchLocationActivity() {
        Intent intent = new Intent(this, LocationActivity.class);
        intent.putExtra(LocationActivity.ALLOW_HOME_UP_KEY, true);
        startActivity(intent);
    }

    private void registerApiListener() {
        mIssuesRequestListener = new FiveCallsApi.IssuesRequestListener() {
            @Override
            public void onRequestError() {
                Snackbar.make(findViewById(R.id.activity_main),
                        getResources().getString(R.string.request_error),
                        Snackbar.LENGTH_LONG).show();
                // Our only type of request in MainActivity is a GET. If it doesn't work, clear the
                // active issues list to avoid showing a stale list.
                mIssuesAdapter.setIssues(Collections.<Issue>emptyList(),
                        IssuesAdapter.ERROR_REQUEST, mFilterText);
                swipeContainer.setRefreshing(false);
            }

            @Override
            public void onJsonError() {
                Snackbar.make(findViewById(R.id.activity_main),
                        getResources().getString(R.string.json_error),
                        Snackbar.LENGTH_LONG).show();
                // Our only type of request in MainActivity is a GET. If it doesn't work, clear the
                // active issues list to avoid showing a stale list.
                mIssuesAdapter.setIssues(Collections.<Issue>emptyList(),
                        IssuesAdapter.ERROR_REQUEST, mFilterText);
                swipeContainer.setRefreshing(false);
            }

            @Override
            public void onAddressError() {
                Snackbar.make(findViewById(R.id.activity_main),
                        getResources().getString(R.string.error_address_invalid),
                        Snackbar.LENGTH_LONG).show();
                // Clear the issues but don't show the refresh button because this is an address
                // problem.
                mIssuesAdapter.setIssues(Collections.<Issue>emptyList(),
                        IssuesAdapter.ERROR_ADDRESS, mFilterText);
                swipeContainer.setRefreshing(false);
            }

            @Override
            public void onIssuesReceived(String locationName, boolean splitDistrict,
                                         List<Issue> issues) {
                locationName = TextUtils.isEmpty(locationName) ?
                        getResources().getString(R.string.unknown_location) : locationName;
                collapsingToolbarLayout.setTitle(String.format(getResources().getString(
                        R.string.title_main), locationName));

                if (splitDistrict) {
                    Snackbar.make(swipeContainer, R.string.split_district_warning,
                            Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.update, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    launchLocationActivity();
                                }
                            }).show();
                }

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

                populateFilterAdapterIfNeeded(issues);
                mIssuesAdapter.setIssues(issues, IssuesAdapter.NO_ERROR, mFilterText);
                swipeContainer.setRefreshing(false);
            }
        };

        AppSingleton.getInstance(getApplicationContext()).getJsonController()
                .registerIssuesRequestListener(mIssuesRequestListener);
    }

    private void populateFilterAdapterIfNeeded(List<Issue> issues) {
        if (mFilterAdapter.getCount() > 2) {
            // Already populated. Don't try again.
            // This assumes that the categories won't change much during the course of a session.
            return;
        }
        List<String> topics = new ArrayList<>();
        for (Issue issue : issues) {
            if (issue.categories == null) {
                continue;
            }
            for (Category category : issue.categories) {
                if (!topics.contains(category.name)) {
                    topics.add(category.name);
                }
            }
        }
        Collections.sort(topics);
        mFilterAdapter.addAll(topics);
        filter.setSelection(mFilterAdapter.getPosition(mFilterText));
        // Set this listener after manually setting the selection so it isn't fired right away.
        filter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String newFilter = mFilterAdapter.getItem(i);
                if (TextUtils.equals(newFilter, mFilterText)) {
                    // Already set!
                    return;
                }
                mFilterText = newFilter;
                if (swipeContainer.isRefreshing()) {
                    // Already loading issues!
                    return;
                }
                refreshIssues();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void loadStats() {
        int callCount = AppSingleton.getInstance(getApplicationContext())
                .getDatabaseHelper().getCallsCount();
        if (callCount > 1) {
            // Don't bother if it is less than 1.
            actionBarSubtitle.setText(String.format(
                    getResources().getString(R.string.your_call_count_summary), callCount));
        }
    }

    private void showStats() {
        Intent intent = new Intent(this, StatsActivity.class);
        startActivity(intent);
    }

    private void refreshIssues() {
        String location = getLocationString();
        if (!TextUtils.isEmpty(location)) {
            AppSingleton.getInstance(getApplicationContext()).getJsonController()
                    .getIssuesForLocation(location);
        } else {
            String message = getString(R.string.main_activity_location_error);
            Snackbar.make(findViewById(R.id.activity_main), message, Snackbar.LENGTH_LONG).show();
        }
    }

    private String getLocationString() {
        if (!TextUtils.isEmpty(mLatitude) && !TextUtils.isEmpty(mLongitude)) {
            return mLatitude + "," + mLongitude;

        } else if (!TextUtils.isEmpty(mAddress)) {
            return mAddress;
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ISSUE_DETAIL_REQUEST && resultCode == RESULT_OK) {
            Issue issue = data.getExtras().getParcelable(IssueActivity.KEY_ISSUE);
            mIssuesAdapter.updateIssue(issue);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class IssuesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        // TODO: Use an enum.
        public static final int NO_ERROR = 10;
        public static final int ERROR_REQUEST = 11;
        public static final int ERROR_ADDRESS = 12;
        public static final int NO_ISSUES_YET = 13;

        private static final int VIEW_TYPE_EMPTY_REQUEST = 0;
        private static final int VIEW_TYPE_ISSUE = 1;
        private static final int VIEW_TYPE_EMPTY_ADDRESS = 2;

        private List<Issue> mIssues = new ArrayList<>();
        private int mErrorType = NO_ISSUES_YET;

        public IssuesAdapter() {
        }

        public void setIssues(List<Issue> issues, int errorType, String filterText) {
            mErrorType = errorType;
            if (TextUtils.equals(filterText,
                    getResources().getString(R.string.all_issues_filter))) {
                // Include everything
                mIssues = issues;
            } else if (TextUtils.equals(filterText,
                    getResources().getString(R.string.top_issues_filter))) {
                // Add only the active ones.
                mIssues.clear();
                for (Issue issue : issues) {
                    if (!issue.inactive) {
                        mIssues.add(issue);
                    }
                }
            } else {
                // Filter by the string
                mIssues.clear();
                for (Issue issue : issues) {
                    for (Category category : issue.categories) {
                        if (TextUtils.equals(filterText, category.name)) {
                            mIssues.add(issue);
                        }
                    }
                }
            }
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
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_EMPTY_REQUEST) {
                View empty = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.empty_issues_view, parent, false);
                return new EmptyRequestViewHolder(empty);
            } else if (viewType == VIEW_TYPE_EMPTY_ADDRESS) {
                View empty = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.empty_issues_address_view, parent, false);
                return new EmptyAddressViewHolder(empty);
            } else {
                RelativeLayout v = (RelativeLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.issue_view, parent, false);
                return new IssueViewHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            int type = getItemViewType(position);
            if (type == VIEW_TYPE_ISSUE) {
                IssueViewHolder vh = (IssueViewHolder) holder;
                final Issue issue = mIssues.get(position);
                vh.name.setText(issue.name);
                vh.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent issueIntent = new Intent(holder.itemView.getContext(),
                                IssueActivity.class);
                        issueIntent.putExtra(IssueActivity.KEY_ISSUE, issue);
                        issueIntent.putExtra(RepCallActivity.KEY_ADDRESS, getLocationString());
                        startActivityForResult(issueIntent, ISSUE_DETAIL_REQUEST);
                    }
                });

                int totalCalls = issue.contacts.length;
                List<String> contacted = AppSingleton.getInstance(getApplicationContext())
                        .getDatabaseHelper().getCallsForIssueAndContacts(issue.id, issue.contacts);
                int callsLeft = totalCalls - contacted.size();
                if (callsLeft == totalCalls) {
                    if (totalCalls == 1) {
                        vh.numCalls.setText(getResources().getString(R.string.call_count_one));
                    } else {
                        vh.numCalls.setText(String.format(
                                getResources().getString(R.string.call_count), totalCalls));
                    }
                } else {
                    if (callsLeft == 1) {
                        vh.numCalls.setText(String.format(
                                getResources().getString(R.string.call_count_remaining_one),
                                totalCalls));
                    } else {
                        vh.numCalls.setText(String.format(
                                getResources().getString(R.string.call_count_remaining), callsLeft,
                                totalCalls));
                    }
                }
                vh.doneIcon.setImageLevel(callsLeft == 0 && totalCalls > 0 ? 1 : 0);
            } else if (type == VIEW_TYPE_EMPTY_REQUEST) {
                EmptyRequestViewHolder vh = (EmptyRequestViewHolder) holder;
                vh.refreshButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        refreshIssues();
                    }
                });
            } else if (type == VIEW_TYPE_EMPTY_ADDRESS) {
                EmptyAddressViewHolder vh = (EmptyAddressViewHolder) holder;
                vh.locationButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        launchLocationActivity();
                    }
                });
            }
        }

        @Override
        public void onViewRecycled(RecyclerView.ViewHolder holder) {
            if (holder instanceof IssueViewHolder) {
                holder.itemView.setOnClickListener(null);
            } else if (holder instanceof EmptyRequestViewHolder) {
                ((EmptyRequestViewHolder) holder).refreshButton.setOnClickListener(null);
            } else if (holder instanceof EmptyAddressViewHolder) {
                ((EmptyAddressViewHolder) holder).locationButton.setOnClickListener(null);
            }
            super.onViewRecycled(holder);
        }

        @Override
        public int getItemCount() {
            if (mIssues.size() == 0 && mErrorType == ERROR_REQUEST || mErrorType == ERROR_ADDRESS) {
                return 1;
            }
            return mIssues.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (mIssues.size() == 0 && position == 0) {
                if (mErrorType == ERROR_REQUEST) {
                    return VIEW_TYPE_EMPTY_REQUEST;
                }
                if (mErrorType == ERROR_ADDRESS) {
                    return VIEW_TYPE_EMPTY_ADDRESS;
                }
            }
            return VIEW_TYPE_ISSUE;
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

    // TODO: Combine EmptyRequestViewHolder and EmptyAddressViewHolder, change strings dynamically.
    private class EmptyRequestViewHolder extends RecyclerView.ViewHolder {
        public Button refreshButton;

        public EmptyRequestViewHolder(View itemView) {
            super(itemView);
            refreshButton = (Button) itemView.findViewById(R.id.refresh_btn);
            // Tinting the compound drawable only works API 23+, so do this manually.
            refreshButton.getCompoundDrawables()[0].mutate().setColorFilter(
                    refreshButton.getResources().getColor(R.color.colorAccent),
                    PorterDuff.Mode.MULTIPLY);
        }
    }

    private class EmptyAddressViewHolder extends RecyclerView.ViewHolder {
        public Button locationButton;

        public EmptyAddressViewHolder(View itemView) {
            super(itemView);
            locationButton = (Button) itemView.findViewById(R.id.location_btn);
            // Tinting the compound drawable only works API 23+, so do this manually.
            locationButton.getCompoundDrawables()[0].mutate().setColorFilter(
                    locationButton.getResources().getColor(R.color.colorAccent),
                    PorterDuff.Mode.MULTIPLY);
        }
    }
}
