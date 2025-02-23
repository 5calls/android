package org.a5calls.android.a5calls.controller;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.core.view.GravityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.DisplayMetrics;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.google.firebase.auth.FirebaseAuth;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.adapter.IssuesAdapter;
import org.a5calls.android.a5calls.databinding.ActivityMainBinding;
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
import java.util.Objects;

import static android.view.View.VISIBLE;

/**
 * The activity which handles zip code lookup and showing the issues list.
 */
public class MainActivity extends AppCompatActivity implements IssuesAdapter.Callback {
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
    private FiveCallsApi.CallRequestListener mReportListener;
    private OnBackPressedCallback mOnBackPressedCallback;
    private String mAddress;
    private String mLatitude;
    private String mLongitude;
    private String mLocationName;
    private boolean mIsLowAccuracy = false;
    private boolean mDonateIsOn = false;
    private FirebaseAuth mAuth = null;

    private ActivityMainBinding binding;

    private Snackbar mSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Consider using fragments
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        try {
            mAuth = FirebaseAuth.getInstance();
        } catch (RuntimeException ex) {
            Log.e(TAG, ex.getMessage());
            mAuth = null;
        }

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

        // TODO: Remove this as probably no one is running the old version of the app any more.
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
            new AnalyticsManager().trackPageview("/", this);
        }

        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
        }

        setupDrawerContent(binding.navigationView);

        if (!accountManager.isNewsletterPromptDone(this)) {
            binding.newsletterSignupView.setVisibility(View.VISIBLE);
            binding.newsletterView.newsletterDeclineButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    accountManager.setNewsletterPromptDone(v.getContext(), true);
                    findViewById(R.id.newsletter_card).setVisibility(View.GONE);
                    findViewById(R.id.newsletter_card_result_decline).setVisibility(VISIBLE);
                }
            });
            binding.newsletterView.newsletterSignupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String email = binding.newsletterView.newsletterEmail.getText().toString();
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        binding.newsletterView.newsletterEmail.setError(
                                getResources().getString(R.string.error_email_format));
                        return;
                    }
                    binding.newsletterView.newsletterSignupButton.setEnabled(false);
                    binding.newsletterView.newsletterDeclineButton.setEnabled(false);
                    FiveCallsApi api =
                            AppSingleton.getInstance(getApplicationContext()).getJsonController();
                    api.newsletterSubscribe(email, new FiveCallsApi.NewsletterSubscribeCallback() {
                        @Override
                        public void onSuccess() {
                            accountManager.setNewsletterPromptDone(v.getContext(), true);
                            accountManager.setNewsletterSignUpCompleted(v.getContext(), true);
                            findViewById(R.id.newsletter_card).setVisibility(View.GONE);
                            findViewById(R.id.newsletter_card_result_success).setVisibility(VISIBLE);
                        }

                        @Override
                        public void onError() {
                            binding.newsletterView.newsletterSignupButton.setEnabled(true);
                            binding.newsletterView.newsletterDeclineButton.setEnabled(true);
                            showSnackbar(R.string.newsletter_signup_error, Snackbar.LENGTH_LONG);
                        }
                    });
                }
            });
        }

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        binding.issuesRecyclerView.setLayoutManager(layoutManager);
        DividerItemDecoration divider = new DividerItemDecoration(this, RecyclerView.VERTICAL);
        binding.issuesRecyclerView.addItemDecoration(divider);
        mIssuesAdapter = new IssuesAdapter(this, this);
        binding.issuesRecyclerView.setAdapter(mIssuesAdapter);

        mFilterAdapter = new ArrayAdapter<>(this, R.layout.filter_item);
        mFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFilterAdapter.addAll(getResources().getStringArray(R.array.default_filters));
        binding.filter.setAdapter(mFilterAdapter);
        if (savedInstanceState != null) {
            mFilterText = savedInstanceState.getString(KEY_FILTER_ITEM_SELECTED);
            mSearchText = savedInstanceState.getString(KEY_SEARCH_TEXT);
            if (TextUtils.isEmpty(mSearchText)) {
                binding.searchBar.setVisibility(View.GONE);
                binding.filter.setVisibility(VISIBLE);
            } else {
                binding.searchBar.setVisibility(VISIBLE);
                binding.filter.setVisibility(View.GONE);
                binding.searchText.setText(mSearchText);
            }
        } else {
            // Safe to use index as the top two filters are hard-coded strings.
            mFilterText = mFilterAdapter.getItem(0);
        }
        binding.searchText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchSearchDialog();
            }
        });
        binding.clearSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onIssueSearchCleared();
            }
        });

        registerOnBackPressedCallback();

        registerApiListener();

        // Refresh the "donateOn" information. This doesn't change much so it's sufficient
        // to do it just once in the activity's lifecycle.
        AppSingleton.getInstance(getApplicationContext()).getJsonController().getReport();

        binding.swipeContainer.setColorSchemeResources(R.color.colorPrimary);
        binding.swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override public void onRefresh() {
                refreshIssues();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mAuth != null) {
            mAuth.signInAnonymously();
        }
    }

    @Override
    protected void onDestroy() {
        FiveCallsApi api = AppSingleton.getInstance(getApplicationContext()).getJsonController();
        api.unregisterIssuesRequestListener(mIssuesRequestListener);
        api.unregisterContactsRequestListener(mContactsRequestListener);
        api.unregisterCallRequestListener(mReportListener);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        binding.drawerLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int supportActionBarHeight =
                        getSupportActionBar() != null ? getSupportActionBar().getHeight() : 0;
                int searchHeight = binding.searchBar.getHeight();
                int filterHeight = binding.filter.getHeight();
                binding.swipeContainer.getLayoutParams().height = (int)
                        (getResources().getConfiguration().screenHeightDp * displayMetrics.density -
                                searchHeight - filterHeight - supportActionBarHeight);
                binding.filter.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        loadStats();

        mAddress = accountManager.getAddress(this);
        mLatitude = accountManager.getLat(this);
        mLongitude = accountManager.getLng(this);

        if (accountManager.isNewsletterPromptDone(this) ||
                accountManager.isNewsletterSignUpCompleted(this)) {
            binding.newsletterSignupView.setVisibility(View.GONE);
        }

        // Refresh on resume. The post is necessary to start the spinner animation.
        // Note that refreshing issues will also refresh the contacts list when it runs
        // on resume.
        binding.swipeContainer.post(new Runnable() {
            @Override public void run() {
                binding.swipeContainer.setRefreshing(true);
                refreshIssues();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
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
            binding.drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }
        else if (item.getItemId() == R.id.menu_refresh) {
            binding.swipeContainer.post(new Runnable() {
                @Override public void run() {
                    binding.swipeContainer.setRefreshing(true);
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

    @Override
    public void launchSearchDialog() {
        DialogFragment searchIssuesDialog = SearchIssuesDialog.newInstance(mSearchText);
        getSupportFragmentManager().beginTransaction().add(searchIssuesDialog,
                SearchIssuesDialog.TAG).commit();
    }

    @Override
    public void startIssueActivity(Context context, Issue issue) {
        Intent issueIntent = new Intent(context, IssueActivity.class);
        issueIntent.putExtra(IssueActivity.KEY_ISSUE, issue);
        issueIntent.putExtra(RepCallActivity.KEY_ADDRESS, getLocationString());
        issueIntent.putExtra(RepCallActivity.KEY_LOCATION_NAME, mLocationName);
        issueIntent.putExtra(IssueActivity.KEY_IS_LOW_ACCURACY, mIsLowAccuracy);
        issueIntent.putExtra(IssueActivity.KEY_DONATE_IS_ON, mDonateIsOn);
        startActivityForResult(issueIntent, ISSUE_DETAIL_REQUEST);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        //menuItem.setChecked(true); // don't use this atm
                        binding.drawerLayout.closeDrawers();

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

    @Override
    public void launchLocationActivity() {
        // Clear it in case they change they location.
        mIssuesAdapter.setContacts(new ArrayList<Contact>(), IssuesAdapter.NO_ERROR);
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
                mIssuesAdapter.setAllIssues(Collections.<Issue>emptyList(),
                        IssuesAdapter.ERROR_REQUEST);
                binding.swipeContainer.setRefreshing(false);
            }

            @Override
            public void onJsonError() {
                showSnackbar(R.string.json_error, Snackbar.LENGTH_LONG);
                // Our only type of request in MainActivity is a GET. If it doesn't work, clear the
                // active issues list to avoid showing a stale list.
                mIssuesAdapter.setAllIssues(Collections.<Issue>emptyList(),
                        IssuesAdapter.ERROR_REQUEST);
                binding.swipeContainer.setRefreshing(false);
            }

            @Override
            public void onIssuesReceived(List<Issue> issues) {
                populateFilterAdapterIfNeeded(issues);
                mIssuesAdapter.setAllIssues(issues, IssuesAdapter.NO_ERROR);
                mIssuesAdapter.setFilterAndSearch(mFilterText, mSearchText);
                binding.swipeContainer.setRefreshing(false);
            }
        };

        mContactsRequestListener = new FiveCallsApi.ContactsRequestListener() {
            @Override
            public void onRequestError() {
                showSnackbar(R.string.request_error, Snackbar.LENGTH_LONG);
                // Our only type of request in MainActivity is a GET. If it doesn't work, clear the
                // active issues list to avoid showing a stale list.
                mIssuesAdapter.setAddressError(IssuesAdapter.ERROR_REQUEST);
                binding.swipeContainer.setRefreshing(false);
            }

            @Override
            public void onJsonError() {
                showSnackbar(R.string.json_error, Snackbar.LENGTH_LONG);
                // Our only type of request in MainActivity is a GET. If it doesn't work, clear the
                // active issues list to avoid showing a stale list.
                mIssuesAdapter.setAddressError(IssuesAdapter.ERROR_REQUEST);
                binding.swipeContainer.setRefreshing(false);
            }

            @Override
            public void onAddressError() {
                showAddressErrorSnackbar();
                mIssuesAdapter.setAddressError(IssuesAdapter.ERROR_ADDRESS);
                binding.swipeContainer.setRefreshing(false);
            }

            @Override
            public void onContactsReceived(String locationName, boolean isLowAccuracy,
                                           List<Contact> contacts) {
                mLocationName = TextUtils.isEmpty(locationName) ?
                        getResources().getString(R.string.unknown_location) : locationName;
                binding.collapsingToolbar.setTitle(String.format(getResources().getString(
                        R.string.title_main), mLocationName));
                mIssuesAdapter.setContacts(contacts, IssuesAdapter.NO_ERROR);
                mIsLowAccuracy = isLowAccuracy;

                hideSnackbars();

                // Check if this is a split district by seeing if there are >2 reps in the house.
                int houseCount = 0;
                for (Contact contact : contacts) {
                    if (TextUtils.equals(contact.area, "US House")) {
                        houseCount++;
                    }
                }
                if (houseCount > 1 || mIsLowAccuracy) {
                    int warning = houseCount > 1 ? R.string.split_district_warning :
                            R.string.low_accuracy_warning;
                    mSnackbar = Snackbar.make(binding.swipeContainer, warning,
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

        mReportListener = new FiveCallsApi.CallRequestListener() {
            @Override
            public void onRequestError() {}

            @Override
            public void onJsonError() {}

            @Override
            public void onReportReceived(int count, boolean donateOn) {
                mDonateIsOn = donateOn;
            }

            @Override
            public void onCallReported() {}
        };

        FiveCallsApi api = AppSingleton.getInstance(getApplicationContext()).getJsonController();
        api.registerIssuesRequestListener(mIssuesRequestListener);
        api.registerContactsRequestListener(mContactsRequestListener);
        api.registerCallRequestListener(mReportListener);
    }

    // Registers a callback that handles back presses. This should be active
    // only when filtering or searching, so that it can do a one-time clear of
    // the active filter or search back to the default main activity state.
    private void registerOnBackPressedCallback() {
        mOnBackPressedCallback = new OnBackPressedCallback(/* enabled= */ false) {
            @Override
            public void handleOnBackPressed() {
                // Clear the filter, if there was one.
                binding.filter.setSelection(0);
                // Clear the search, if there was one.
                onIssueSearchSet("");
                // The calls above will disable this callback, so no need
                // to do it here.
            }
        };
        updateOnBackPressedCallbackEnabled();
        getOnBackPressedDispatcher().addCallback(mOnBackPressedCallback);
    }

    // Should be called whenever filter or search state changes.
    private void updateOnBackPressedCallbackEnabled() {
        boolean isFiltering = !Objects.equals(mFilterText, mFilterAdapter.getItem(0));
        boolean isSearching = !TextUtils.isEmpty(mSearchText);
        mOnBackPressedCallback.setEnabled(isFiltering || isSearching);
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
        binding.filter.setSelection(mFilterAdapter.getPosition(mFilterText));
        // Set this listener after manually setting the selection so it isn't fired right away.
        binding.filter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String newFilter = mFilterAdapter.getItem(i);
                if (TextUtils.equals(newFilter, mFilterText)) {
                    // Already set!
                    return;
                }
                mFilterText = newFilter;
                updateOnBackPressedCallbackEnabled();
                if (binding.swipeContainer.isRefreshing()) {
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
            binding.actionBarSubtitle.setText(String.format(
                    getResources().getString(R.string.your_call_count_summary), callCount));
        }
    }

    private void showStats() {
        Intent intent = new Intent(this, StatsActivity.class);
        startActivity(intent);
    }

    @Override
    public void refreshIssues() {
        FiveCallsApi api = AppSingleton.getInstance(getApplicationContext()).getJsonController();

        if (!mIssuesAdapter.hasContacts()) {
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
        binding.filter.setVisibility(View.GONE);
        binding.searchBar.setVisibility(VISIBLE);
        setSearchText(searchText);
        updateOnBackPressedCallbackEnabled();
    }

    public void onIssueSearchCleared() {
        binding.filter.setVisibility(VISIBLE);
        binding.searchBar.setVisibility(View.GONE);
        setSearchText("");
        updateOnBackPressedCallbackEnabled();
    }

    private void setSearchText(String searchText) {
        binding.searchText.setText(searchText);
        if (TextUtils.equals(mSearchText, searchText)) {
            // Already set, no need to do work.
            return;
        }
        mSearchText = searchText;
        if (binding.swipeContainer.isRefreshing()) {
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

    private void showSnackbar(int message, int length) {
        if (mSnackbar == null) {
            constructSnackbar(message, length);
            mSnackbar.show();
        }
    }

    private void showAddressErrorSnackbar() {
        hideSnackbars();
        constructSnackbar(R.string.error_address_invalid, Snackbar.LENGTH_INDEFINITE);
        mSnackbar.setAction(R.string.update, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchLocationActivity();
            }
        });
        mSnackbar.show();
    }

    private void constructSnackbar(int message, int length) {
        mSnackbar = Snackbar.make(findViewById(R.id.activity_main),
                getResources().getString(message),
                length);
        mSnackbar.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                super.onDismissed(transientBottomBar, event);
                mSnackbar = null;
            }
        });
    }
}
