package org.a5calls.android.a5calls.controller;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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
import org.a5calls.android.a5calls.model.Contact;
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
    public static final String VOICEMAIL = "vm";
    public static final String CONTACTED = "contacted";
    public static final String UNAVAILABLE = "unavailable";

    private static final String KEY_ACTIVE_CONTACT_INDEX = "active_contact_index";
    private static final String KEY_LOCAL_OFFICES_EXPANDED = "local_offices_expanded";

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
    @BindView(R.id.contact_done_img) ImageButton contactChecked;

    @BindView(R.id.script_section) LinearLayout scriptLayout;
    @BindView(R.id.contact_reason) TextView contactReason;
    @BindView(R.id.call_script) TextView callScript;

    @BindView(R.id.no_calls_left) TextView noCallsLeft;
    @BindView(R.id.buttons_prompt) TextView buttonsPrompt;

    @BindView(R.id.unavailable_btn) Button unavailableButton;
    @BindView(R.id.voicemail_btn) Button voicemailButton;
    @BindView(R.id.made_contact_btn) Button madeContactButton;
    @BindView(R.id.skip_btn) Button skipButton;

    @BindView(R.id.local_office_btn) Button localOfficeButton;
    @BindView(R.id.field_office_section) LinearLayout localOfficeSection;
    @BindView(R.id.field_office_prompt) TextView localOfficePrompt;

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
                setButtonsEnabled(true);
                tryLoadingNextContact(getResources().getString(
                        R.string.request_error_db_recorded_anyway));
            }

            @Override
            public void onJsonError() {
                setButtonsEnabled(true);
                tryLoadingNextContact(getResources().getString(
                        R.string.json_error_db_recorded_anyway));
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
                // Note: Skips are not reported.
                Log.d(TAG, "call reported successfully!");
                setButtonsEnabled(true);
                if (mActiveContactIndex == mIssue.contacts.length - 1) {
                    // Show the check if this was the last one
                    final List<String> previousCalls =
                            AppSingleton.getInstance(IssueActivity.this).getDatabaseHelper()
                                    .getCallResults(mIssue.id,
                                            mIssue.contacts[mActiveContactIndex].id);
                    showContactChecked(previousCalls);
                }
                tryLoadingNextContact(getResources().getString(R.string.call_reported));
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
            // Hide everything if there are no contacts at all.
            skipButton.setVisibility(View.GONE);
            unavailableButton.setVisibility(View.GONE);
            voicemailButton.setVisibility(View.GONE);
            madeContactButton.setVisibility(View.GONE);
            buttonsPrompt.setVisibility(View.GONE);
            callThisOffice.setVisibility(View.GONE);
            localOfficeButton.setVisibility(View.GONE);
            scriptLayout.setVisibility(View.GONE);
            repInfoLayout.setVisibility(View.GONE);
            noCallsLeft.setVisibility(View.VISIBLE);
        } else {
            boolean expandLocalOffices = false;
            if (savedInstanceState != null) {
                mActiveContactIndex = savedInstanceState.getInt(KEY_ACTIVE_CONTACT_INDEX, 0);
                expandLocalOffices = savedInstanceState.getBoolean(KEY_LOCAL_OFFICES_EXPANDED,
                        false);
            } else {
                mActiveContactIndex = 0;
            }
            setupContactUi(mActiveContactIndex, expandLocalOffices);

            madeContactButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportEvent(CONTACTED);
                    reportCall(CONTACTED, zip);
                }
            });

            unavailableButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportEvent(UNAVAILABLE);
                    reportCall(UNAVAILABLE, zip);
                }
            });

            voicemailButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportEvent(VOICEMAIL);
                    reportCall(VOICEMAIL, zip);
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
        outState.putBoolean(KEY_LOCAL_OFFICES_EXPANDED,
                localOfficeSection.getVisibility() == View.VISIBLE);
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
                mIssue.name, mIssue.contacts[mActiveContactIndex].id,
                mIssue.contacts[mActiveContactIndex].name, callType, zip);
        AppSingleton.getInstance(getApplicationContext()).getJsonController().reportCall(
                mIssue.id, mIssue.contacts[mActiveContactIndex].id, callType, zip);
    }

    private void setButtonsEnabled(boolean enabled) {
        skipButton.setEnabled(enabled);
        voicemailButton.setEnabled(enabled);
        madeContactButton.setEnabled(enabled);
        unavailableButton.setEnabled(enabled);
    }

    private void setupContactUi(int index, boolean expandLocalSection) {
        final Contact contact = mIssue.contacts[index];
        contactName.setText(contact.name);
        contactReason.setText(contact.reason);
        if (!TextUtils.isEmpty(contact.photoURL)) {
            repImage.setVisibility(View.VISIBLE);
            Glide.with(getApplicationContext()).load(contact.photoURL).into(repImage);
        } else {
            repImage.setVisibility(View.GONE);
        }
        phoneNumber.setText(contact.phone);
        Linkify.addLinks(phoneNumber, Linkify.PHONE_NUMBERS);

        if (expandLocalSection) {
            localOfficeButton.setVisibility(View.INVISIBLE);
            expandLocalOfficeSection(contact);
        } else {
            localOfficeSection.setVisibility(View.GONE);
            localOfficeSection.removeViews(1, localOfficeSection.getChildCount() - 1);
            if (contact.field_offices == null || contact.field_offices.length == 0) {
                localOfficeButton.setVisibility(View.GONE);
            } else {
                localOfficeButton.setVisibility(View.VISIBLE);
                localOfficeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        localOfficeButton.setOnClickListener(null);
                        expandLocalOfficeSection(contact);
                    }
                });
            }
        }

        // Show a bit about whether they've been contacted yet
        final List<String> previousCalls = AppSingleton.getInstance(this).getDatabaseHelper()
                .getCallResults(mIssue.id, contact.id);
        if (previousCalls.size() > 0) {
            showContactChecked(previousCalls);
        } else {
            contactChecked.setVisibility(View.GONE);
            contactChecked.setOnClickListener(null);
        }

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
        if (index == mIssue.contacts.length - 1) {
            skipButton.setText(getResources().getString(R.string.done));
            skipButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportEvent("skip");
                    // Since this is the last contact, just go back to the main menu if they "skip"
                    returnToMain();
                }
            });
        } else {
            skipButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportEvent("skip");
                    tryLoadingNextContact(getResources().getString(R.string.skip_snackbar_message));
                }
            });
        }
    }

    private void showContactChecked(final List<String> previousCalls) {
        contactChecked.setVisibility(View.VISIBLE);
        contactChecked.setImageLevel(1);
        contactChecked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = TextUtils.join(", ", previousCalls)
                        .replace(IssueActivity.VOICEMAIL, getResources().getString(
                                R.string.voicemail_btn))
                        .replace(IssueActivity.CONTACTED, getResources().getString(
                                R.string.made_contact_btn))
                        .replace(IssueActivity.UNAVAILABLE, getResources().getString(
                                R.string.unavailable_btn));
                new AlertDialog.Builder(IssueActivity.this)
                        .setTitle(R.string.contact_details_dialog_title)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                        .show();
            }
        });
    }

    private void expandLocalOfficeSection(Contact contact) {
        localOfficeButton.setVisibility(View.INVISIBLE);
        localOfficeSection.setVisibility(View.VISIBLE);
        localOfficePrompt.setText(String.format(getResources().getString(
                R.string.field_office_prompt), contact.name));
        // TODO: Use an adapter or ListView or something. There aren't expected to be
        // so many local offices so this is OK for now.
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < contact.field_offices.length; i++) {
            ViewGroup localOfficeInfo = (ViewGroup) inflater.inflate(
                    R.layout.field_office_list_item, null);
            TextView numberView = (TextView) localOfficeInfo.findViewById(
                    R.id.field_office_number);
            numberView.setText(contact.field_offices[i].phone);
            Linkify.addLinks(numberView, Linkify.PHONE_NUMBERS);
            ((TextView) localOfficeInfo.findViewById(R.id.field_office_city)).setText(
                    "- " + contact.field_offices[i].city);
            localOfficeSection.addView(localOfficeInfo);
        }
    }

    private void tryLoadingNextContact(String snackbarMessage) {
        if (mActiveContactIndex + 1 >= mIssue.contacts.length) {
            Snackbar.make(issueName, snackbarMessage + " " +
                    getResources().getString(R.string.no_calls_left), Snackbar.LENGTH_LONG)
                    .setAction(getResources().getString(R.string.back), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            returnToMain();
                        }
                    })
                    .show();
        } else {
            mActiveContactIndex++;
            Snackbar.make(issueName, snackbarMessage, Snackbar.LENGTH_LONG).show();
            setupContactUi(mActiveContactIndex, /* don't expand local offices by default */ false);
        }
    }

    // This isn't used right now but might be helpful later.
    private int getNextUncontactedIndex() {
        DatabaseHelper db = AppSingleton.getInstance(this).getDatabaseHelper();
        for (int i = 0; i < mIssue.contacts.length; i++) {
            int index = (mActiveContactIndex + i) % mIssue.contacts.length;
            if (!db.hasCalled(mIssue.id, mIssue.contacts[index].id)) {
                return index;
            }
        }
        return -1;
    }

    private void returnToMain() {
        Intent intent = new Intent();
        intent.putExtra(KEY_ISSUE, mIssue);
        if (getParent() == null) {
            setResult(Activity.RESULT_OK, intent);
        } else {
            getParent().setResult(Activity.RESULT_OK, intent);
        }
        supportFinishAfterTransition();
    }

    private void reportEvent(String event) {
        if (mTracker != null) {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("CallAction")
                    .setAction(event)
                    .setLabel(mIssue.id + " " + mIssue.contacts[mActiveContactIndex].id)
                    .setValue(1)
                    .build());
        }
    }
}
