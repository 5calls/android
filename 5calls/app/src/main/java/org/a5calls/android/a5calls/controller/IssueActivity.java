package org.a5calls.android.a5calls.controller;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.DatabaseHelper;
import org.a5calls.android.a5calls.model.FiveCallsApi;
import org.a5calls.android.a5calls.model.Issue;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * More details about an issue, including links to the phone app to call and buttons to record
 * your calls.
 */
public class IssueActivity extends AppCompatActivity {
    private static final String TAG = "IssueActivity";
    public static final String KEY_ISSUE = "key_issue";
    public static final String KEY_ZIP = "key_zip";
    private static final String KEY_ACTIVE_CONTACT_INDEX = "active_contact_index";

    private final AccountManager accountManager = AccountManager.Instance;

    private FiveCallsApi.RequestStatusListener mStatusListener;
    private Issue mIssue;
    private int mActiveContactIndex;
    private Tracker mTracker = null;

    @BindView(R.id.scroll_view) ScrollView scrollView;

    @BindView(R.id.issue_name) TextView issueName;
    @BindView(R.id.issue_description) TextView issueDescription;
    @BindView(R.id.rep_info) RelativeLayout repInfoLayout;
    @BindView(R.id.rep_image) ImageView repImage;
    @BindView(R.id.call_this_office) TextView callThisOffice;
    @BindView(R.id.contact_name) TextView contactName;
    @BindView(R.id.phone_number) TextView phoneNumber;

    @BindView(R.id.script_section) LinearLayout scriptLayout;
    @BindView(R.id.contact_reason) TextView contactReason;
    @BindView(R.id.call_script) TextView callScript;

    @BindView(R.id.no_calls_left) TextView noCallsLeft;
    @BindView(R.id.buttons_prompt) TextView buttonsPrompt;

    @BindView(R.id.buttons_holder) LinearLayout buttonsLayout;
    @BindView(R.id.unavailable_btn) Button unavailableButton;
    @BindView(R.id.voicemail_btn) Button voicemailButton;
    @BindView(R.id.made_contact_btn) Button madeContactButton;

    @BindView(R.id.skip_btn) Button skipButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String zip = getIntent().getStringExtra(KEY_ZIP);
        if (savedInstanceState == null) {
            mIssue = getIntent().getParcelableExtra(KEY_ISSUE);
        } else {
            mIssue = savedInstanceState.getParcelable(KEY_ISSUE);
        }
        if (mIssue == null) {
            // TODO handle this better? Is it even possible to get here?
            finish();
            return;
        }

        setContentView(R.layout.activity_issue);
        ButterKnife.bind(this);

        mStatusListener = new FiveCallsApi.RequestStatusListener() {
            @Override
            public void onRequestError() {
                Snackbar.make(issueName,
                        getResources().getString(R.string.request_error_db_recorded_anyway),
                        Snackbar.LENGTH_LONG).show();
                setButtonsEnabled(true);
            }

            @Override
            public void onJsonError() {
                Snackbar.make(issueName,
                        getResources().getString(R.string.json_error_db_recorded_anyway),
                        Snackbar.LENGTH_LONG).show();
                setButtonsEnabled(true);
            }

            @Override
            public void onIssuesReceived(String locationName, List<Issue> issues) {
                // unused
            }

            @Override
            public void onCallCount(int count) {
                // unused
            }

            @Override
            public void onCallReported() {
                Log.d(TAG, "call reported successfully!");
                Snackbar.make(issueName,
                        getResources().getString(R.string.call_reported),
                        Snackbar.LENGTH_SHORT).show();
                setButtonsEnabled(true);
                tryLoadingNextContact();
            }
        };
        FiveCallsApi controller = AppSingleton.getInstance(getApplicationContext())
                .getJsonController();
        controller.registerStatusListener(mStatusListener);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(mIssue.name);

        issueName.setText(mIssue.name);
        issueDescription.setText(mIssue.reason);
        callScript.setText(mIssue.script);

        if (mIssue.contacts == null || mIssue.contacts.length == 0) {
            buttonsLayout.setVisibility(View.GONE);
            buttonsPrompt.setVisibility(View.GONE);
            skipButton.setVisibility(View.GONE);
            callThisOffice.setVisibility(View.GONE);
            noCallsLeft.setVisibility(View.VISIBLE);
        } else {
            if (savedInstanceState != null) {
                mActiveContactIndex = savedInstanceState.getInt(KEY_ACTIVE_CONTACT_INDEX, 0);
            } else {
                DatabaseHelper databaseHelper =
                        AppSingleton.getInstance(getApplicationContext()).getDatabaseHelper();

                mActiveContactIndex = 0;
                // Try to start at the next un-contacted representative in the list.
                for (int i = 0; i < mIssue.contacts.length; i++) {
                    boolean contacted = databaseHelper.hasCalled(mIssue.id, mIssue.contacts[i].id);
                    if (!contacted) {
                        mActiveContactIndex = i;
                        break;
                    }
                }
            }
            setupContactUi(mActiveContactIndex);

            skipButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportEvent("skip");
                    tryLoadingNextContact();
                }
            });

            madeContactButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportEvent("contacted");
                    reportCall("contacted", zip);
                }
            });

            unavailableButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportEvent("unavailable");
                    reportCall("unavailable", zip);
                }
            });

            voicemailButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportEvent("vm");
                    reportCall("vm", zip);
                }
            });
        }

        // We allow Analytics opt-out.
        if (accountManager.allowAnalytics(this)) {
            // Obtain the shared Tracker instance.
            FiveCallsApplication application = (FiveCallsApplication) getApplication();
            mTracker = application.getDefaultTracker();
        }
    }

    @Override
    protected void onDestroy() {
        AppSingleton.getInstance(getApplicationContext()).getJsonController()
                .unregisterStatusListener(mStatusListener);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ACTIVE_CONTACT_INDEX, mActiveContactIndex);
        outState.putParcelable(KEY_ISSUE, mIssue);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mTracker != null) {
            mTracker.setScreenName(TAG);
            mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        returnToMain();
    }

    private void reportCall(String callType, String zip) {
        setButtonsEnabled(false);
        AppSingleton.getInstance(getApplicationContext()).getDatabaseHelper().addCall(mIssue.id,
                mIssue.contacts[mActiveContactIndex].id, callType, zip);
        AppSingleton.getInstance(getApplicationContext()).getJsonController().reportCall(
                mIssue.id, mIssue.contacts[mActiveContactIndex].id, callType, zip);
    }

    private void setButtonsEnabled(boolean enabled) {
        skipButton.setEnabled(enabled);
        voicemailButton.setEnabled(enabled);
        madeContactButton.setEnabled(enabled);
        unavailableButton.setEnabled(enabled);
    }

    private void setupContactUi(int index) {
        contactName.setText(mIssue.contacts[index].name);
        contactReason.setText(mIssue.contacts[index].reason);
        if (!TextUtils.isEmpty(mIssue.contacts[index].photoURL)) {
            Glide.with(getApplicationContext())
                    .load(mIssue.contacts[index].photoURL)
                    .into(repImage);
        } else {
            repImage.setVisibility(View.GONE);
        }
        phoneNumber.setText(mIssue.contacts[index].phone);
        Linkify.addLinks(phoneNumber, Linkify.PHONE_NUMBERS);

        // If the ScrollView is below the contact, scroll back up to show it.
        if (scrollView.getScrollY() > repInfoLayout.getTop()) {
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    // TODO: A little more scroll padding might be nice too.
                    scrollView.smoothScrollTo(0, repInfoLayout.getTop());
                }
            });
        }
    }

    private void tryLoadingNextContact() {
        if (mActiveContactIndex == mIssue.contacts.length - 1) {
            // Done!
            returnToMain();
        } else {
            // TODO: Instead of just increasing the index, check to find the next *un-contacted*
            // representative. If there is none, increasing the index is OK.
            mActiveContactIndex++;
            setupContactUi(mActiveContactIndex);
        }
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

    private void reportEvent(String event) {
        if (mTracker != null) {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("CallAction")
                    .setAction(event)
                    .setLabel(mIssue.id + " " + mIssue.contacts[mActiveContactIndex].id)
                    .build());
        }
    }
}
