package org.a5calls.android.a5calls.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.BuildConfig;
import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.databinding.ActivityIssueBinding;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.Contact;
import org.a5calls.android.a5calls.model.CustomizedContactScript;
import org.a5calls.android.a5calls.model.DatabaseHelper;
import org.a5calls.android.a5calls.model.Issue;
import org.a5calls.android.a5calls.net.FiveCallsApi;
import org.a5calls.android.a5calls.util.MarkdownUtil;
import org.a5calls.android.a5calls.util.StateMapping;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * More details about an issue, including links to the phone app to call and buttons to record
 * your calls.
 */
public class IssueActivity extends AppCompatActivity implements FiveCallsApi.ScriptsRequestListener {
    private static final String TAG = "IssueActivity";
    public static final String KEY_ISSUE = "key_issue";
    public static final String KEY_IS_DISTRICT_SPLIT = "key_is_district_split";
    public static final String KEY_IS_LOW_ACCURACY = "key_is_low_accuracy";
    public static final String KEY_DONATE_IS_ON = "key_donate_is_on";

    public static final int RESULT_OK = 1;
    public static final int RESULT_SERVER_ERROR = 2;

    private static final String DONATE_URL = "https://secure.actblue.com/donate/5calls-donate?refcode=android&refcode2=";

    private static final int MIN_CALLS_TO_SHOW_CALL_STATS = 10;

    private boolean mShowServerError = false;

    private Issue mIssue;
    // indicates that the zip entered intersects with multiple congressional districts
    private boolean mIsDistrictSplit = false;
    // low accuracy locations are zip codes or city names, we warn on state reps if you are using one
    private boolean mIsLowAccuracy = false;
    private boolean mDonateIsOn = false;
    private boolean mIsAnimating = false;

    private ActivityIssueBinding binding;
    private ActivityResultLauncher<Intent> mRepCallLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityIssueBinding.inflate(getLayoutInflater());

        // Register activity result launcher for RepCallActivity
        mRepCallLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_SERVER_ERROR) {
                        mShowServerError = true;
                    }
                });

        mIssue = getIntent().getParcelableExtra(KEY_ISSUE);
        if (mIssue == null) {
            // TODO handle this better? Is it even possible to get here?
            finish();
            return;
        }
        mIsDistrictSplit = getIntent().getBooleanExtra(KEY_IS_DISTRICT_SPLIT, false);
        mIsLowAccuracy = getIntent().getBooleanExtra(KEY_IS_LOW_ACCURACY, false);
        mDonateIsOn = getIntent().getBooleanExtra(KEY_DONATE_IS_ON, false);

        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(mIssue.name);
        }

        final BottomSheetBehavior<NestedScrollView> behavior =
                BottomSheetBehavior.from(binding.bottomSheet);
        final int targetPeakHeight = behavior.getPeekHeight();
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() |
                            WindowInsetsCompat.Type.displayCutout());
            binding.appBarLayout.setPadding(insets.left, insets.top, insets.right, 0);
            binding.scrollView.setPadding(insets.left, 0, insets.right, insets.bottom);
            binding.bottomSheet.setPadding(0, 0, 0, insets.bottom);
            final int activityPadding = getResources().getDimensionPixelSize(
                    R.dimen.activity_horizontal_padding);
            binding.repPrompt.setPadding(activityPadding + insets.left, 0,
                    activityPadding + insets.right, 0);
            int numChildren = binding.repList.getChildCount();
            final int repItemHorizontalPadding = getResources().getDimensionPixelSize(
                    R.dimen.rep_list_dimens_left_right);
            final int repItemVerticalPadding = getResources().getDimensionPixelSize(
                    R.dimen.rep_list_dimens);
            for (int i = 0; i < numChildren; i++) {
                View child = binding.repList.getChildAt(i);
                child.setPadding(repItemHorizontalPadding + insets.left, repItemVerticalPadding,
                        repItemHorizontalPadding + insets.right, repItemVerticalPadding);
            }
            behavior.setPeekHeight(targetPeakHeight + insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        if (!TextUtils.isEmpty(mIssue.meta)) {
            String stateName = StateMapping.getStateName(mIssue.meta);
            if (!TextUtils.isEmpty(stateName)) {
                binding.stateIndicator.setText(stateName);
                binding.stateIndicator.setVisibility(View.VISIBLE);
            }
        }

        binding.issueName.setText(mIssue.name);
        MarkdownUtil.setUpScript(binding.issueDescription, mIssue.reason, getApplicationContext());
        if (!TextUtils.isEmpty(mIssue.link)) {
            binding.link.setVisibility(View.VISIBLE);
            binding.link.setMovementMethod(LinkMovementMethod.getInstance());
            if ((TextUtils.isEmpty(mIssue.linkTitle))) {
                binding.link.setText(mIssue.link);
            } else {
                binding.link.setText(Html.fromHtml(
                        String.format("<a href=\"%s\">%s</a>", mIssue.link, mIssue.linkTitle)));
            }
        } else {
            binding.link.setVisibility(View.GONE);
        }

        binding.repPrompt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED ||
                        behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    behavior.setState(behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED ?
                            BottomSheetBehavior.STATE_EXPANDED :
                            BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });
        final int collapsedSize =
                getResources().getDimensionPixelSize(R.dimen.accessibility_min_size);
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            private boolean wasAtBottom = false;

            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_SETTLING ||
                        newState == BottomSheetBehavior.STATE_DRAGGING) {
                    wasAtBottom = binding.scrollView.getHeight() +
                            binding.scrollView.getScrollY() >=
                            binding.issueSection.getMeasuredHeight();
                    mIsAnimating = true;
                } else {
                    mIsAnimating = false;
                    binding.expandContactsIcon.setRotation(newState == BottomSheetBehavior.STATE_EXPANDED ?
                            0 : 180);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (behavior.getState() != BottomSheetBehavior.STATE_DRAGGING &&
                        behavior.getState() != BottomSheetBehavior.STATE_SETTLING) {
                    return;
                }
                binding.expandContactsIcon.setRotation(180 - slideOffset * 180);
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)
                        binding.scrollView.getLayoutParams();
                params.bottomMargin = collapsedSize + (int) ((bottomSheet.getMeasuredHeight() -
                        collapsedSize) * slideOffset);
                binding.scrollView.setLayoutParams(params);
                // Only auto-scroll up if we are already scrolled to the bottom.
                if (wasAtBottom) {
                    binding.scrollView.fullScroll(View.FOCUS_DOWN);
                }
            }
        });

        binding.scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY,
                                       int oldScrollX, int oldScrollY) {
                // If we are fully scrolled and it isn't open, open it.
                if (mIsAnimating) {
                    return;
                }
                if (binding.scrollView.getHeight() + binding.scrollView.getScrollY() >=
                        binding.issueSection.getMeasuredHeight() + binding.bottomSheet.getPaddingBottom()) {
                    if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                }
            }
        });

        FiveCallsApplication.analyticsManager().trackPageview(mIssue.permalink, this);

        // Register scripts request listener once
        FiveCallsApi api = AppSingleton.getInstance(this).getJsonController();
        api.registerScriptsRequestListener(this);

        // Fetch customized scripts once on create
        fetchCustomizedScripts();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                returnToMain();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_ISSUE, mIssue);
        outState.putBoolean(KEY_IS_DISTRICT_SPLIT, mIsDistrictSplit);
        outState.putBoolean(KEY_IS_LOW_ACCURACY, mIsLowAccuracy);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mShowServerError) {
            Snackbar.make(getWindow().getDecorView(),
                    getResources().getString(R.string.call_error_db_recorded_anyway),
                    Snackbar.LENGTH_LONG).show();
            mShowServerError = false;
        }
        if (mIssue.contactAreas.isEmpty()) {
            binding.issueDone.getRoot().setVisibility(View.GONE);
            binding.noContactAreas.setVisibility(View.VISIBLE);
            return;
        }
        if (mIssue.contacts == null) {
            if (AccountManager.Instance.hasLocation(this)) {
                // An address error.
                binding.noCallsLeft.setVisibility(View.VISIBLE);
                binding.updateLocationButton.setOnClickListener(view -> {
                    Intent intent = new Intent(IssueActivity.this, LocationActivity.class);
                    intent.putExtra(LocationActivity.ALLOW_HOME_UP_KEY, true);
                    startActivity(intent);
                });
            } else {
                // Hasn't set an address yet.
                binding.noAddressSet.setVisibility(View.VISIBLE);
                binding.setLocationButton.setOnClickListener(view -> {
                    Intent intent = new Intent(IssueActivity.this, LocationActivity.class);
                    intent.putExtra(LocationActivity.ALLOW_HOME_UP_KEY, true);
                    startActivity(intent);
                });
            }
            binding.issueDone.getRoot().setVisibility(View.GONE);
            return;
        }

        // Check for vacancies:
        // If there is only 1 contact in the senate, or no contact in the house, then
        // that is a vacancy. Add a placeholder contact.
        if (mIssue.contactAreas.contains(Contact.AREA_SENATE)) {
            int senateCount = 0;
            for (Contact contact : mIssue.contacts) {
                if (TextUtils.equals(contact.area, Contact.AREA_SENATE)) {
                    senateCount++;
                }
            }
            if (senateCount <= 1) {
                mIssue.contacts.add(Contact.createPlaceholder(
                        "placeholderSenate",
                        getResources().getString(R.string.vacant_seat_rep_name),
                        getResources().getString(R.string.vacant_seat_rep_reason, Contact.AREA_SENATE),
                        Contact.AREA_SENATE));
            }
        }

        if (mIssue.contactAreas.contains(Contact.AREA_HOUSE)) {
            int houseCount = 0;
            for (Contact contact : mIssue.contacts) {
                if (TextUtils.equals(contact.area, Contact.AREA_HOUSE)) {
                    houseCount++;
                }
            }
            if (houseCount == 0) {
                mIssue.contacts.add(Contact.createPlaceholder("placeholderHouse",
                        getResources().getString(R.string.vacant_seat_rep_name),
                        getResources().getString(R.string.vacant_seat_rep_reason, Contact.AREA_HOUSE),
                        Contact.AREA_HOUSE));
            }
        }

        // Still no reps after populating vacancies.
        // TODO: Determine if this is ever due to invalid address or if it is always because
        // of vacancies or districts without the type of rep (e.g. senate-only calls in house-only
        // districts like DC).
        if (mIssue.contacts.isEmpty()) {
            binding.noCallsLeft.setVisibility(View.VISIBLE);
            binding.issueDone.getRoot().setVisibility(View.GONE);
            binding.updateLocationButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(IssueActivity.this, LocationActivity.class);
                    intent.putExtra(LocationActivity.ALLOW_HOME_UP_KEY, true);
                    startActivity(intent);
                }
            });
            return;
        }

        // Maybe show the notification dialog if everyone in this issue has been called.
        boolean allCalled = loadRepList();
        int callCount = AppSingleton.getInstance(this).getDatabaseHelper()
                .getCallsCount();
        Fragment dialog = getSupportFragmentManager()
                .findFragmentByTag(NotificationSettingsDialog.TAG);
        if (allCalled && !AccountManager.Instance.isNotificationDialogShown(this)) {
            if (dialog == null) {
                NotificationSettingsDialog fragment = NotificationSettingsDialog.newInstance();
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(fragment, NotificationSettingsDialog.TAG)
                        .commit();
            }
        } else if (dialog != null) {
            ((NotificationSettingsDialog) dialog).dismiss();
        }

        // When we're not showing the dialog and have a few calls, prompt to leave a review.
        if (callCount >= 4 && !BuildConfig.DEBUG &&
                !AccountManager.Instance.hasReviewDialogBeenShown(this)) {
            ReviewManager reviewManager = ReviewManagerFactory.create(this);
            Task<ReviewInfo> request = reviewManager.requestReviewFlow();
            request.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // We can get the ReviewInfo object
                    ReviewInfo reviewInfo = task.getResult();
                    Task<Void> flow = reviewManager.launchReviewFlow(this, reviewInfo);
                    AccountManager.Instance.setReviewDialogShown(this, true);
                }
            });
        }

        maybeShowIssueDone();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_issue, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            returnToMain();
            return true;
        }
        if (item.getItemId() == R.id.menu_share) {
            sendShare(true);
            return true;
        }
        if (item.getItemId() == R.id.menu_details) {
            showIssueDetails();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void returnToMain() {
        Intent intent = new Intent();
        intent.putExtra(KEY_ISSUE, mIssue);
        if (getParent() == null) {
            setResult(Activity.RESULT_OK, intent);
        } else {
            getParent().setResult(Activity.RESULT_OK, intent);
        }
        finish();
    }

    private void sendShare(boolean urlOnly) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(
                R.string.issue_share_subject));
        if (urlOnly) {
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    String.format(getResources().getString(R.string.issue_share_url), mIssue.permalink));
        } else {
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    String.format(getResources().getString(R.string.issue_share_content), mIssue.name,
                            mIssue.permalink));
        }
        shareIntent.setType("text/plain");

        // Could send analytics on share event.

        startActivity(Intent.createChooser(shareIntent, getResources().getString(
                R.string.share_chooser_title)));
    }

    private void launchDonate() {
        // Could send analytics on donate event.

        String donateUrl = DONATE_URL + AccountManager.Instance.getCallerID(this);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(donateUrl)));
    }

    /**
     * Loads the representatives-to-call list at the bottom.
     * @return true if each rep has been called at least once.
     */
    private boolean loadRepList() {
        binding.repList.removeAllViews();
        boolean allCalled = true;
        DatabaseHelper dbHelper = AppSingleton.getInstance(this).getDatabaseHelper();

        for (int i = 0; i < mIssue.contacts.size(); i++) {
            Contact contact = mIssue.contacts.get(i);
            View repView = LayoutInflater.from(this).inflate(R.layout.rep_list_view, null);
            boolean hasCalledToday = dbHelper.hasCalledToday(mIssue.id, contact.id);
            populateRepView(repView, contact, i, hasCalledToday);
            binding.repList.addView(repView);
            if (!hasCalledToday && !contact.isPlaceholder) {
                allCalled = false;
            }
        }
        return allCalled && !mIssue.contacts.isEmpty();
    }

    private void populateRepView(View repView, Contact contact, final int index,
                                 boolean hasCalledToday) {
        final TextView contactName = repView.findViewById(R.id.contact_name);
        final ImageView repImage = repView.findViewById(R.id.rep_image);
        final ImageView contactChecked = repView.findViewById(R.id.contact_done_img);
        final TextView contactReason = repView.findViewById(R.id.contact_reason);
        final TextView contactWarning = repView.findViewById(R.id.contact_warning);

        String displayName;
        if (!TextUtils.isEmpty(contact.party)) {
            displayName = String.format("%s (%s-%s)", contact.name, contact.party.charAt(0), contact.state);
        } else {
            displayName = contact.name;
        }
        contactName.setText(displayName);
        contactWarning.setVisibility(View.GONE);
        if (!TextUtils.isEmpty(contact.reason)) {
            contactReason.setText(contact.reason);
            if (TextUtils.equals(contact.area, Contact.AREA_HOUSE) && mIsDistrictSplit) {
                contactWarning.setVisibility(View.VISIBLE);
                contactWarning.setText(R.string.split_district_warning);
            } else if ((TextUtils.equals(contact.area, Contact.AREA_STATE_LOWER) ||
                       TextUtils.equals(contact.area, Contact.AREA_STATE_UPPER)) && mIsLowAccuracy) {
                contactWarning.setVisibility(View.VISIBLE);
                contactWarning.setText(R.string.low_accuracy_state_rep_warning);
            }
            contactReason.setVisibility(View.VISIBLE);
        } else {
            contactReason.setVisibility(View.GONE);
        }
        if (!contact.isPlaceholder) {
            if (!TextUtils.isEmpty(contact.photoURL)) {
            Glide.with(getApplicationContext())
                    .load(contact.photoURL)
                    .centerCrop()
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.baseline_person_52)
                    .into(repImage);
            }
            // Show a bit about whether they've been contacted yet today.
            if (hasCalledToday) {
                contactChecked.setImageLevel(1);
                contactChecked.setContentDescription(getResources().getString(
                        R.string.contact_done_img_description));
            } else {
                contactChecked.setImageLevel(0);
                contactChecked.setContentDescription(null);
            }

            repView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getApplicationContext(), RepCallActivity.class);
                    intent.putExtra(KEY_ISSUE, mIssue);
                    intent.putExtra(RepCallActivity.KEY_ADDRESS,
                            getIntent().getStringExtra(RepCallActivity.KEY_ADDRESS));
                    intent.putExtra(RepCallActivity.KEY_LOCATION_NAME,
                            getIntent().getStringExtra(RepCallActivity.KEY_LOCATION_NAME));
                    intent.putExtra(RepCallActivity.KEY_ACTIVE_CONTACT_INDEX, index);
                    mRepCallLauncher.launch(intent);
                }
            });
        } else {
            // Placeholder.
            contactChecked.setVisibility(View.GONE);
            contactName.setTextColor(getColor(R.color.textColorDarkGrey));
            repImage.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.grey_circle));
        }
    }

    private void maybeShowIssueDone() {
        if (mIssue.contacts.isEmpty()) {
            // Couldn't find any contacts.
            binding.issueDone.getRoot().setVisibility(View.GONE);
            return;
        }
        final DatabaseHelper dbHelper = AppSingleton.getInstance(this).getDatabaseHelper();
        int numPlaceholder = 0;
        for (Contact contact : mIssue.contacts) {
            if (contact.isPlaceholder) {
                numPlaceholder++;
                continue;
            }
            if (!dbHelper.hasCalledToday(mIssue.id, contact.id)) {
                binding.issueDone.getRoot().setVisibility(View.GONE);
                return;
            }
        }
        if (numPlaceholder == mIssue.contacts.size()) {
            // All contacts are placeholders.
            binding.issueDone.getRoot().setVisibility(View.GONE);
            return;
        }
        // At this point, all the contacts have been contacted today.
        binding.issueDone.getRoot().setVisibility(View.VISIBLE);
        binding.scrollView.scrollTo(0, 0);

        // Format call stats like a nice number with commas.
        ((TextView) findViewById(R.id.issue_call_count)).setText(
                String.format(Locale.getDefault(), "%,d", mIssue.stats.calls));

        findViewById(R.id.share_btn).setOnClickListener(v -> sendShare(false));

        if (mDonateIsOn) {
            findViewById(R.id.donate_section).setVisibility(View.VISIBLE);
            findViewById(R.id.donate_btn).setOnClickListener(v -> launchDonate());
        } else {
            findViewById(R.id.donate_section).setVisibility(View.GONE);
        }
    }

    private void showIssueDetails() {
        new AlertDialog.Builder(IssueActivity.this)
                .setTitle(R.string.details_btn)
                .setMessage(getIssueDetailsMessage(IssueActivity.this, mIssue))
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {

                        })
                .show();
    }

    @VisibleForTesting
    static String getIssueDetailsMessage(Context context, Issue issue) {
        StringBuilder result = new StringBuilder();
        if (issue.categories.length > 0) {
            if (issue.categories.length == 1) {
                result.append(context.getResources().getString(R.string.issue_category_one));
            } else {
                result.append(context.getResources().getString(R.string.issue_category_many));
            }
            for (int i = 0; i < issue.categories.length; i++) {
                if (i > 0) {
                    result.append(",");
                }
                result.append(" ").append(issue.categories[i].name);
            }
            result.append("\n\n");
        }
        if (issue.stats.calls >= MIN_CALLS_TO_SHOW_CALL_STATS) {
            String callCount = String.format(Locale.getDefault(), "%,d", issue.stats.calls);
            result.append(context.getResources().getString(R.string.done_issue_stats));
            result.append(" ").append(callCount).append("\n\n");
        }
        // Define input format (ISO 8601)
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            // Parse the date string into a Date object
            Date date = isoFormat.parse(issue.createdAt);
            if (date == null) {
                return result.toString();
            }

            // Define output format
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM d, yyyy",
                    Locale.getDefault());

            // Format and print the date
            String formattedDate = outputFormat.format(date);
            result.append(context.getResources().getString(R.string.issue_date_created,
                    formattedDate));
        } catch (ParseException e) {
            Log.d(TAG, "Unable to parse created date: " + issue.createdAt);
        }
        return result.toString();
    }

    private void fetchCustomizedScripts() {
        if (mIssue == null || mIssue.contacts == null || mIssue.contacts.isEmpty()) {
            return;
        }

        String address = getIntent().getStringExtra(RepCallActivity.KEY_ADDRESS);
        String locationName = getIntent().getStringExtra(RepCallActivity.KEY_LOCATION_NAME);

        if (TextUtils.isEmpty(address) && TextUtils.isEmpty(locationName)) {
            return;
        }


        List<String> contactIds = new ArrayList<>();
        for (Contact contact : mIssue.contacts) {
            contactIds.add(contact.id);
        }

        FiveCallsApi api = AppSingleton.getInstance(this).getJsonController();
        String userName = AccountManager.Instance.getUserName(this);
        api.getCustomizedScripts(mIssue.id, contactIds, locationName != null ? locationName : address, userName);
    }

    @Override
    public void onRequestError(String issueId) {
    }

    @Override
    public void onJsonError(String issueId) {
    }

    @Override
    public void onScriptsReceived(String issueId, List<CustomizedContactScript> scripts) {
        // Only process scripts for the current issue to prevent race conditions
        if (TextUtils.equals(mIssue.id, issueId)) {
            // Apply the scripts to the current issue
            mIssue.customizedScripts = scripts;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FiveCallsApi api = AppSingleton.getInstance(this).getJsonController();
        api.unregisterScriptsRequestListener(this);
    }
}
