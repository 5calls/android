package org.a5calls.android.a5calls.controller;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.BuildConfig;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.Contact;
import org.a5calls.android.a5calls.model.Issue;
import org.a5calls.android.a5calls.util.AnalyticsManager;
import org.a5calls.android.a5calls.util.MarkdownUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.noties.markwon.Markwon;

/**
 * More details about an issue, including links to the phone app to call and buttons to record
 * your calls.
 */
public class IssueActivity extends AppCompatActivity {
    private static final String TAG = "IssueActivity";
    public static final String KEY_ISSUE = "key_issue";

    public static final int RESULT_OK = 1;
    public static final int RESULT_SERVER_ERROR = 2;

    private static final int REP_CALL_REQUEST_CODE = 1;
    private boolean mShowServerError = false;

    private final AccountManager accountManager = AccountManager.Instance;

    private Issue mIssue;
//    private Tracker mTracker = null;
    private boolean mIsAnimating = false;

    @BindView(R.id.scroll_view) NestedScrollView scrollView;
    @BindView(R.id.issue_name) TextView issueName;
    @BindView(R.id.issue_description) TextView issueDescription;
    @BindView(R.id.no_calls_left) ViewGroup noCallsLeft;
    @BindView(R.id.update_location_btn) Button updateLocationBtn;
    @BindView(R.id.rep_prompt) ViewGroup repPrompt;
    @BindView(R.id.rep_list) LinearLayout repList;
    @BindView(R.id.bottom_sheet) NestedScrollView bottomSheet;
    @BindView(R.id.main_layout) ViewGroup issueTextSection;
    @BindView(R.id.expand_contacts_icon) ImageView expandContactsIcon;
    @BindView(R.id.link) TextView linkText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIssue = getIntent().getParcelableExtra(KEY_ISSUE);
        if (mIssue == null) {
            // TODO handle this better? Is it even possible to get here?
            finish();
            return;
        }

        setContentView(R.layout.activity_issue);
        ButterKnife.bind(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(mIssue.name);
        }

        issueName.setText(mIssue.name);
        MarkdownUtil.setUpScript(issueDescription, mIssue.reason, getApplicationContext());
        if (!TextUtils.isEmpty(mIssue.link)) {
            linkText.setVisibility(View.VISIBLE);
            linkText.setMovementMethod(LinkMovementMethod.getInstance());
            if ((TextUtils.isEmpty(mIssue.linkTitle))) {
                linkText.setText(mIssue.link);
            } else {
                linkText.setText(Html.fromHtml(
                        String.format("<a href=\"%s\">%s</a>", mIssue.link, mIssue.linkTitle)));
            }
        } else {
            linkText.setVisibility(View.GONE);
        }

        final BottomSheetBehavior<NestedScrollView> behavior =
                BottomSheetBehavior.from(bottomSheet);
        repPrompt.setOnClickListener(new View.OnClickListener() {
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
        final int collapsedSize = getResources().getDimensionPixelSize(R.dimen.accessibility_min_size);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            private boolean wasAtBottom = false;

            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_SETTLING ||
                        newState == BottomSheetBehavior.STATE_DRAGGING) {
                    wasAtBottom = scrollView.getHeight() + scrollView.getScrollY() >=
                            issueTextSection.getMeasuredHeight();
                    mIsAnimating = true;
                } else {
                    mIsAnimating = false;
                    expandContactsIcon.setRotation(newState == BottomSheetBehavior.STATE_EXPANDED ?
                            0 : 180);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (behavior.getState() != BottomSheetBehavior.STATE_DRAGGING &&
                        behavior.getState() != BottomSheetBehavior.STATE_SETTLING) {
                    return;
                }
                expandContactsIcon.setRotation(180 - slideOffset * 180);
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)
                        scrollView.getLayoutParams();
                params.bottomMargin = collapsedSize + (int) ((bottomSheet.getMeasuredHeight() -
                        collapsedSize) * slideOffset);
                scrollView.setLayoutParams(params);
                // Only auto-scroll up if we are already scrolled to the bottom.
                if (wasAtBottom) {
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
            }
        });

        scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY,
                                       int oldScrollX, int oldScrollY) {
                // If we are fully scrolled and it isn't open, open it.
                if (mIsAnimating) {
                    return;
                }
                if (scrollView.getHeight() + scrollView.getScrollY() >=
                        issueTextSection.getMeasuredHeight()) {
                    if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                }

            }
        });

        new AnalyticsManager().trackPageview(String.format("/issue/%s/", mIssue.slug));

        // We allow Analytics opt-out.
        if (accountManager.allowAnalytics(this)) {
            // Obtain the shared Tracker instance.
//            FiveCallsApplication application = (FiveCallsApplication) getApplication();
//            mTracker = application.getDefaultTracker();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_ISSUE, mIssue);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (mTracker != null) {
//            mTracker.setScreenName(TAG);
//            mTracker.send(new HitBuilders.ScreenViewBuilder().build());
//        }
        if (mShowServerError) {
            Snackbar.make(getWindow().getDecorView(),
                    getResources().getString(R.string.call_error_db_recorded_anyway),
                    Snackbar.LENGTH_LONG).show();
            mShowServerError = false;
        }
        if (mIssue.contacts == null || mIssue.contacts.isEmpty()) {
            noCallsLeft.setVisibility(View.VISIBLE);
            updateLocationBtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(IssueActivity.this, LocationActivity.class);
                    startActivity(intent);
                }
            });
        } else {
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

            // when we're not showing the dialog and have a few calls, potentially leave a review
            if (callCount >= 4 && !BuildConfig.DEBUG && !AccountManager.Instance.hasReviewDialogBeenShown(this)) {
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
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_issue, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_share:
                sendShare();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        returnToMain();
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

    private void sendShare() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(
                R.string.issue_share_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                String.format(getResources().getString(R.string.issue_share_content), mIssue.name,
                        mIssue.slug));
        shareIntent.setType("text/plain");

//        if (mTracker != null) {
//            mTracker.send(new HitBuilders.EventBuilder()
//                    .setCategory("Share")
//                    .setAction("IssueShare")
//                    .setLabel(mIssue.id)
//                    .setValue(1)
//                    .build());
//        }

        startActivity(Intent.createChooser(shareIntent, getResources().getString(
                R.string.share_chooser_title)));
    }

    /**
     * Loads the representatives-to-call list at the bottom.
     * @return true least once.if each rep has been called at
     */
    private boolean loadRepList() {
        repList.removeAllViews();
        boolean allCalled = true;
        for (int i = 0; i < mIssue.contacts.size(); i++) {
            View repView = LayoutInflater.from(this).inflate(R.layout.rep_list_view, null);
            List<String> previousCalls = AppSingleton.getInstance(this).getDatabaseHelper()
                    .getCallResults(mIssue.id, mIssue.contacts.get(i).id);
            populateRepView(repView, mIssue.contacts.get(i), i, previousCalls);
            repList.addView(repView);
            if (previousCalls.size() == 0) {
                allCalled = false;
            }
        }
        return allCalled && mIssue.contacts.size() > 0;
    }

    private void populateRepView(View repView, Contact contact, final int index,
                                 List<String> previousCalls) {
        TextView contactName = repView.findViewById(R.id.contact_name);
        final ImageView repImage = repView.findViewById(R.id.rep_image);
        ImageView contactChecked = repView.findViewById(R.id.contact_done_img);
        TextView contactReason = repView.findViewById(R.id.contact_reason);
        TextView contactWarning = repView.findViewById(R.id.contact_warning);
        contactName.setText(contact.name);
        contactWarning.setVisibility(View.GONE);
        if (!TextUtils.isEmpty(contact.area)) {
            contactReason.setText(contact.area);
            if (TextUtils.equals(contact.area, "US House") && mIssue.isSplit) {
                contactWarning.setVisibility(View.VISIBLE);
                contactWarning.setText(R.string.split_district_warning);
            }
            contactReason.setVisibility(View.VISIBLE);
        } else {
            contactReason.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(contact.photoURL)) {
            Glide.with(getApplicationContext())
                    .load(contact.photoURL)
                    .asBitmap()
                    .centerCrop()
                    .placeholder(R.drawable.baseline_person_52)
                    .into(new BitmapImageViewTarget(repImage) {
                        @Override
                        protected void setResource(Bitmap resource) {
                            RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(
                                    repImage.getContext().getResources(), resource);
                            drawable.setCircular(true);
                            drawable.setGravity(Gravity.TOP);
                            repImage.setImageDrawable(drawable);
                        }
                    });
        }
        // Show a bit about whether they've been contacted yet
        if (previousCalls.size() > 0) {
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
                intent.putExtra(RepCallActivity.KEY_ACTIVE_CONTACT_INDEX, index);
                startActivityForResult(intent, REP_CALL_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_SERVER_ERROR) {
            mShowServerError = true;
        }
    }
}
