package org.a5calls.android.a5calls.controller;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.DialogFragment;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

//import com.google.android.gms.analytics.HitBuilders;
//import com.google.android.gms.analytics.Tracker;
import com.google.firebase.auth.FirebaseAuth;
import com.wbrawner.plausible.android.Plausible;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.Category;
import org.a5calls.android.a5calls.model.Contact;
import org.a5calls.android.a5calls.net.FiveCallsApi;
import org.a5calls.android.a5calls.model.Issue;
import org.a5calls.android.a5calls.util.AnalyticsManager;
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
    private static final String KEY_SEARCH_TEXT = "searchText";
    private final AccountManager accountManager = AccountManager.Instance;

    private ArrayAdapter<String> mFilterAdapter;
    private String mFilterText = "";
    private String mSearchText = "";
    private IssuesAdapter mIssuesAdapter;
    private FiveCallsApi.IssuesRequestListener mIssuesRequestListener;
    private FiveCallsApi.ContactsRequestListener mContactsRequestListener;
    private String mAddress;
    private String mLatitude;
    private String mLongitude;
    private FirebaseAuth mAuth;

    @BindView(R.id.swipe_container) SwipeRefreshLayout swipeContainer;
    @BindView(R.id.issues_recycler_view) RecyclerView issuesRecyclerView;
    @BindView(R.id.drawer_layout) DrawerLayout drawerLayout;
    @BindView(R.id.toolbar) Toolbar actionBar;
    @BindView(R.id.nav_view) NavigationView navigationView;
    @BindView(R.id.collapsing_toolbar) CollapsingToolbarLayout collapsingToolbarLayout;
    @BindView(R.id.action_bar_subtitle) TextView actionBarSubtitle;
    @BindView(R.id.filter) AppCompatSpinner filter;
    @BindView(R.id.search_bar) ViewGroup searchBar;
    @BindView(R.id.clear_search_button) ImageButton clearSearchButton;
    @BindView(R.id.search_text) TextView searchTextView;
    private Snackbar mSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Consider using fragments
        super.onCreate(savedInstanceState);

        new AnalyticsManager().trackPageview("/");
        mAuth = FirebaseAuth.getInstance();

        // See if we've had this user before. If not, start them at tutorial type page.
        if (!accountManager.isTutorialSeen(this)) {
            Intent intent = new Intent(this, TutorialActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Confirm the user has set a location.
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
//                FiveCallsApplication application = (FiveCallsApplication) getApplication();
//                Tracker tracker = application.getDefaultTracker();
//                tracker.send(new HitBuilders.EventBuilder()
//                        .setCategory("Reminders")
//                        .setAction("LaunchFromReminder")
//                        .setValue(1)
//                        .build());
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
            mSearchText = savedInstanceState.getString(KEY_SEARCH_TEXT);
            if (TextUtils.isEmpty(mSearchText)) {
                searchBar.setVisibility(View.GONE);
                filter.setVisibility(View.VISIBLE);
            } else {
                searchBar.setVisibility(View.VISIBLE);
                filter.setVisibility(View.GONE);
                searchTextView.setText(mSearchText);
            }
        } else {
            mFilterText = mFilterAdapter.getItem(0);  // Default value
        }
        searchTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchSearchDialog();
            }
        });
        clearSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onIssueSearchCleared();
            }
        });

        registerApiListener();

        swipeContainer.setColorSchemeResources(R.color.colorPrimary);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override public void onRefresh() {
                refreshIssues();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        mAuth.signInAnonymously();
    }

    @Override
    protected void onDestroy() {
        FiveCallsApi api = AppSingleton.getInstance(getApplicationContext()).getJsonController();
        api.unregisterIssuesRequestListener(mIssuesRequestListener);
        api.unregisterContactsRequestListener(mContactsRequestListener);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadStats();

        mAddress = accountManager.getAddress(this);
        mLatitude = accountManager.getLat(this);
        mLongitude = accountManager.getLng(this);

        String location = getLocationString();
        if (!TextUtils.isEmpty(location)) {
            AppSingleton.getInstance(getApplicationContext()).getJsonController()
                    .getContacts(location);
        }

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
//            FiveCallsApplication application = (FiveCallsApplication) getApplication();
//            Tracker tracker = application.getDefaultTracker();
//            tracker.setScreenName(TAG);
//            tracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_FILTER_ITEM_SELECTED, mFilterText);
        outState.putString(KEY_SEARCH_TEXT, mSearchText);
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
        } else if (item.getItemId() == R.id.menu_search) {
            launchSearchDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void launchSearchDialog() {
        DialogFragment searchIssuesDialog = SearchIssuesDialog.newInstance(mSearchText);
        getSupportFragmentManager().beginTransaction().add(searchIssuesDialog,
                SearchIssuesDialog.TAG).commit();
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
                            CustomTabsUtil.launchUrl(MainActivity.this, Uri.parse(getString(R.string.faq_url)));
                            return true;
                        } else if (item.getItemId() == R.id.menu_location) {
                            launchLocationActivity();
                            return true;
                        }

                        return true;
                    }
                });
    }

    private void launchLocationActivity() {
        // Clear it in case they change they location.
        mIssuesAdapter.setContacts(new ArrayList<Contact>());
        Intent intent = new Intent(this, LocationActivity.class);
        intent.putExtra(LocationActivity.ALLOW_HOME_UP_KEY, true);
        startActivity(intent);
    }

    private void registerApiListener() {
        mIssuesRequestListener = new FiveCallsApi.IssuesRequestListener() {
            @Override
            public void onRequestError() {
                showSnackbar(R.string.request_error, Snackbar.LENGTH_LONG);
                // Our only type of request in MainActivity is a GET. If it doesn't work, clear the
                // active issues list to avoid showing a stale list.
                mIssuesAdapter.setIssues(Collections.<Issue>emptyList(),
                        IssuesAdapter.ERROR_REQUEST);
                swipeContainer.setRefreshing(false);
            }

            @Override
            public void onJsonError() {
                showSnackbar(R.string.json_error, Snackbar.LENGTH_LONG);
                // Our only type of request in MainActivity is a GET. If it doesn't work, clear the
                // active issues list to avoid showing a stale list.
                mIssuesAdapter.setIssues(Collections.<Issue>emptyList(),
                        IssuesAdapter.ERROR_REQUEST);
                swipeContainer.setRefreshing(false);
            }

            @Override
            public void onIssuesReceived(List<Issue> issues) {
                populateFilterAdapterIfNeeded(issues);
                mIssuesAdapter.setIssues(issues, IssuesAdapter.NO_ERROR);
                mIssuesAdapter.setFilterAndSearch(mFilterText, mSearchText);
                swipeContainer.setRefreshing(false);
            }
        };

        mContactsRequestListener = new FiveCallsApi.ContactsRequestListener() {

            @Override
            public void onRequestError() {
                showSnackbar(R.string.request_error, Snackbar.LENGTH_LONG);
                // Our only type of request in MainActivity is a GET. If it doesn't work, clear the
                // active issues list to avoid showing a stale list.
                mIssuesAdapter.setIssues(Collections.<Issue>emptyList(),
                        IssuesAdapter.ERROR_REQUEST);
                swipeContainer.setRefreshing(false);
            }

            @Override
            public void onJsonError() {
                showSnackbar(R.string.json_error, Snackbar.LENGTH_LONG);
                // Our only type of request in MainActivity is a GET. If it doesn't work, clear the
                // active issues list to avoid showing a stale list.
                mIssuesAdapter.setIssues(Collections.<Issue>emptyList(),
                        IssuesAdapter.ERROR_REQUEST);
                swipeContainer.setRefreshing(false);
            }

            @Override
            public void onAddressError() {
                hideSnackbars();
                showSnackbar(R.string.error_address_invalid, Snackbar.LENGTH_LONG);
                // Clear the issues but don't show the refresh button because this is an address
                // problem.
                mIssuesAdapter.setIssues(Collections.<Issue>emptyList(),
                        IssuesAdapter.ERROR_ADDRESS);
                swipeContainer.setRefreshing(false);
            }

            @Override
            public void onContactsReceived(String locationName, List<Contact> contacts) {
                locationName = TextUtils.isEmpty(locationName) ?
                        getResources().getString(R.string.unknown_location) : locationName;
                collapsingToolbarLayout.setTitle(String.format(getResources().getString(
                        R.string.title_main), locationName));
                mIssuesAdapter.setContacts(contacts);
                
                hideSnackbars();

                // Check if this is a split district by seeing if there are >2 reps in the house.
                int houseCount = 0;
                for (Contact contact : contacts) {
                    if (TextUtils.equals(contact.area, "US House")) {
                        houseCount++;
                    }
                }
                if (houseCount > 1) {
                    mSnackbar = Snackbar.make(swipeContainer, R.string.split_district_warning,
                            Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.update, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    launchLocationActivity();
                                }
                            });
                    mSnackbar.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            super.onDismissed(transientBottomBar, event);
                            mSnackbar = null;
                        }
                    });
                    mSnackbar.show();
                }
            }
        };

        FiveCallsApi api = AppSingleton.getInstance(getApplicationContext()).getJsonController();
        api.registerIssuesRequestListener(mIssuesRequestListener);
        api.registerContactsRequestListener(mContactsRequestListener);
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
                mIssuesAdapter.setFilterAndSearch(mFilterText, mSearchText);
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
        FiveCallsApi api = AppSingleton.getInstance(getApplicationContext()).getJsonController();

        if (mIssuesAdapter.mContacts.size() == 0) {
            String location = getLocationString();
            if (!TextUtils.isEmpty(location)) {
                api.getContacts(location);
            }
        }
        api.getIssues();
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

    public void onIssueSearchSet(String searchText) {
        if (TextUtils.isEmpty(searchText.trim())) {
            onIssueSearchCleared();
            return;
        }
        filter.setVisibility(View.GONE);
        searchBar.setVisibility(View.VISIBLE);
        setSearchText(searchText);
    }

    public void onIssueSearchCleared() {
        filter.setVisibility(View.VISIBLE);
        searchBar.setVisibility(View.GONE);
        setSearchText("");
    }

    private void setSearchText(String searchText) {
        searchTextView.setText(searchText);
        if (TextUtils.equals(mSearchText, searchText)) {
            // Already set, no need to do work.
            return;
        }
        mSearchText = searchText;
        if (swipeContainer.isRefreshing()) {
            // Already loading issues!
            return;
        }
        mIssuesAdapter.setFilterAndSearch(mFilterText, mSearchText);
    }

    private void hideSnackbars() {
        // Hide any existing snackbars.
        if (mSnackbar != null) {
            mSnackbar.dismiss();
            mSnackbar = null;
        }
    }

    private void showSnackbar(int string, int length) {
        if (mSnackbar == null) {
            mSnackbar = Snackbar.make(findViewById(R.id.activity_main),
                    getResources().getString(string),
                    length);
            mSnackbar.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                @Override
                public void onDismissed(Snackbar transientBottomBar, int event) {
                    super.onDismissed(transientBottomBar, event);
                    mSnackbar = null;
                }
            });
            mSnackbar.show();
        }
    }

    private class IssuesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        // TODO: Use an enum.
        public static final int NO_ERROR = 10;
        public static final int ERROR_REQUEST = 11;
        public static final int ERROR_ADDRESS = 12;
        public static final int NO_ISSUES_YET = 13;
        public static final int ERROR_SEARCH_NO_MATCH = 14;

        private static final int VIEW_TYPE_EMPTY_REQUEST = 0;
        private static final int VIEW_TYPE_ISSUE = 1;
        private static final int VIEW_TYPE_EMPTY_ADDRESS = 2;
        private static final int VIEW_TYPE_NO_SEARCH_MATCH = 3;

        private List<Issue> mIssues = new ArrayList<>();
        private List<Issue> mAllIssues = new ArrayList<>();
        private int mErrorType = NO_ISSUES_YET;

        private List<Contact> mContacts = new ArrayList<>();

        public IssuesAdapter() {
        }

        // |searchText| takes priority over |filterText|.
        public void setIssues(List<Issue> issues, int errorType) {
            mAllIssues = issues;
            mErrorType = errorType;
            mIssues = new ArrayList<>();
        }

        public void setContacts(List<Contact> contacts) {
            // Check if the contacts have returned after the issues list. If so, notify data set
            // changed.
            Boolean notify = false;
            if (mAllIssues.size() > 0 && mContacts.size() == 0) {
                notify = true;
            }
            mContacts = contacts;
            if (notify) {
                notifyDataSetChanged();
            }
        }

        public void setFilterAndSearch(String filterText, String searchText) {
            if (!TextUtils.isEmpty(searchText)) {
                mIssues = new ArrayList<>();
                // Should we .trim() the whitespace?
                String lowerSearchText = searchText.toLowerCase();
                for (Issue issue : mAllIssues) {
                    // Search the name and the categories for the search term.
                    // TODO: Searching full text is less straight forward, as a simple "contains"
                    // matches things like "ice" to "avarice" or whatever.
                    if (issue.name.toLowerCase().contains(lowerSearchText)) {
                        mIssues.add(issue);
                    } else {
                        for (int i = 0; i < issue.categories.length; i++) {
                            if (issue.categories[i].name.toLowerCase().contains(lowerSearchText)) {
                                mIssues.add(issue);
                            }
                        }
                    }
                }
                // If there's no other error, show a search error.
                if (mIssues.size() == 0 && mErrorType == NO_ERROR) {
                    mErrorType = ERROR_SEARCH_NO_MATCH;
                }
            } else {
                if (TextUtils.equals(filterText,
                        getResources().getString(R.string.all_issues_filter))) {
                    // Include everything
                    mIssues = mAllIssues;
                } else if (TextUtils.equals(filterText,
                        getResources().getString(R.string.top_issues_filter))) {
                    // Add only the active ones.
                    mIssues = new ArrayList<>();
                    for (Issue issue : mAllIssues) {
                        if (issue.active) {
                            mIssues.add(issue);
                        }
                    }
                } else {
                    // Filter by the string
                    mIssues = new ArrayList<>();
                    for (Issue issue : mAllIssues) {
                        for (Category category : issue.categories) {
                            if (TextUtils.equals(filterText, category.name)) {
                                mIssues.add(issue);
                            }
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
            } else if (viewType == VIEW_TYPE_NO_SEARCH_MATCH) {
                View empty = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.empty_issues_search_view, parent, false);
                return new EmptySearchViewHolder(empty);
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

                issue.contacts = new ArrayList<Contact>();
                int houseCount = 0;  // Only add the first contact in the house for each issue.
                for (String contactArea : issue.contactAreas) {
                    for (Contact contact : mContacts) {
                        if (TextUtils.equals(contact.area, contactArea) &&
                                !issue.contacts.contains(contact)) {
                            if (TextUtils.equals(contact.area, "US House")) {
                                houseCount++;
                                if (houseCount > 1) {
                                    issue.isSplit = true;
                                    continue;
                                }
                            }

                            issue.contacts.add(contact);
                        }
                    }
                }

                int totalCalls = issue.contacts.size();
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
            } else if (type == VIEW_TYPE_NO_SEARCH_MATCH) {
                EmptySearchViewHolder vh = (EmptySearchViewHolder) holder;
                vh.searchButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        launchSearchDialog();
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
            } else if (holder instanceof EmptySearchViewHolder) {
                ((EmptySearchViewHolder) holder).searchButton.setOnClickListener(null);
            }
            super.onViewRecycled(holder);
        }

        @Override
        public int getItemCount() {
            if (mIssues.size() == 0 && (mErrorType == ERROR_REQUEST || mErrorType == ERROR_ADDRESS
                    || mErrorType == ERROR_SEARCH_NO_MATCH)) {
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
                if (mErrorType == ERROR_SEARCH_NO_MATCH) {
                    return VIEW_TYPE_NO_SEARCH_MATCH;
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

    private class EmptySearchViewHolder extends RecyclerView.ViewHolder {
        public Button searchButton;

        public EmptySearchViewHolder(View itemView) {
            super(itemView);
            searchButton = (Button) itemView.findViewById(R.id.search_btn);
            // Tinting the compound drawable only works API 23+, so do this manually.
            searchButton.getCompoundDrawables()[0].mutate().setColorFilter(
                    searchButton.getResources().getColor(R.color.colorAccent),
                    PorterDuff.Mode.MULTIPLY);
        }
    }
}
