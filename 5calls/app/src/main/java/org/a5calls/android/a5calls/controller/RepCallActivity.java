package org.a5calls.android.a5calls.controller;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
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
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.Contact;
import org.a5calls.android.a5calls.model.FiveCallsApi;
import org.a5calls.android.a5calls.model.Issue;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.a5calls.android.a5calls.controller.IssueActivity.KEY_ISSUE;

/**
 * Activity which handles showing a script for a rep and logging calls.
 */
public class RepCallActivity extends AppCompatActivity {
    private static final String TAG = "RepCallActivity";

    public static final String KEY_ADDRESS = "key_address";
    public static final String VOICEMAIL = "vm";
    public static final String CONTACTED = "contacted";
    public static final String UNAVAILABLE = "unavailable";

    public static final String KEY_ACTIVE_CONTACT_INDEX = "active_contact_index";
    private static final String KEY_LOCAL_OFFICES_EXPANDED = "local_offices_expanded";

    private final AccountManager accountManager = AccountManager.Instance;

    private FiveCallsApi.CallRequestListener mStatusListener;
    private Issue mIssue;
    private int mActiveContactIndex;
    private Tracker mTracker = null;

    @BindView(R.id.scroll_view) ScrollView scrollView;

    @BindView(R.id.rep_info) RelativeLayout repInfoLayout;
    @BindView(R.id.rep_image) ImageView repImage;
    @BindView(R.id.call_this_office) TextView callThisOffice;
    @BindView(R.id.contact_name) TextView contactName;
    @BindView(R.id.phone_number) TextView phoneNumber;
    @BindView(R.id.contact_done_img) ImageButton contactChecked;

    @BindView(R.id.buttons_prompt) TextView buttonsPrompt;
    @BindView(R.id.unavailable_btn) Button unavailableButton;
    @BindView(R.id.voicemail_btn) Button voicemailButton;
    @BindView(R.id.made_contact_btn) Button madeContactButton;
    @BindView(R.id.skip_btn) Button skipButton;

    @BindView(R.id.local_office_btn) Button localOfficeButton;
    @BindView(R.id.field_office_section) LinearLayout localOfficeSection;
    @BindView(R.id.field_office_prompt) TextView localOfficePrompt;

    @BindView(R.id.script_section) LinearLayout scriptLayout;
    @BindView(R.id.contact_reason) TextView contactReason;
    @BindView(R.id.call_script) TextView callScript;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String address = getIntent().getStringExtra(KEY_ADDRESS);
        mActiveContactIndex = getIntent().getIntExtra(KEY_ACTIVE_CONTACT_INDEX, 0);
        mIssue = getIntent().getParcelableExtra(KEY_ISSUE);

        setContentView(R.layout.activity_rep_call);
        ButterKnife.bind(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(mIssue.name);
        }

        mStatusListener = new FiveCallsApi.CallRequestListener() {
            @Override
            public void onRequestError() {
                setButtonsEnabled(true);
                showError(R.string.request_error_db_recorded_anyway);
            }

            @Override
            public void onJsonError() {
                setButtonsEnabled(true);
                showError(R.string.json_error_db_recorded_anyway);
            }

            @Override
            public void onCallCount(int count) {
                // unused
            }

            @Override
            public void onCallReported() {
                // Note: Skips are not reported.
                Log.d(TAG, "call reported successfully!");
                returnToIssue();
            }
        };
        FiveCallsApi controller = AppSingleton.getInstance(getApplicationContext())
                .getJsonController();
        controller.registerCallRequestListener(mStatusListener);

        callScript.setText(mIssue.script);

        boolean expandLocalOffices = false;
        if (savedInstanceState != null) {
            expandLocalOffices = savedInstanceState.getBoolean(KEY_LOCAL_OFFICES_EXPANDED,
                    false);
        }
        setupContactUi(mActiveContactIndex, expandLocalOffices);

        madeContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportEvent(RepCallActivity.CONTACTED);
                reportCall(RepCallActivity.CONTACTED, address);
            }
        });

        unavailableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportEvent(RepCallActivity.UNAVAILABLE);
                reportCall(RepCallActivity.UNAVAILABLE, address);
            }
        });

        voicemailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportEvent(RepCallActivity.VOICEMAIL);
                reportCall(RepCallActivity.VOICEMAIL, address);
            }
        });

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
                .unregisterCallRequestListener(mStatusListener);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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
                returnToIssue();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void reportCall(String callType, String address) {
        setButtonsEnabled(false);
        AppSingleton.getInstance(getApplicationContext()).getDatabaseHelper().addCall(mIssue.id,
                mIssue.name, mIssue.contacts[mActiveContactIndex].id,
                mIssue.contacts[mActiveContactIndex].name, callType, address);
        AppSingleton.getInstance(getApplicationContext()).getJsonController().reportCall(
                mIssue.id, mIssue.contacts[mActiveContactIndex].id, callType, address);
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
            Glide.with(getApplicationContext())
                    .load(contact.photoURL)
                    .asBitmap()
                    .centerCrop()
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

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportEvent("skip");
                // Since this is the last contact, just go back to the main menu if they "skip"
                returnToIssue();
            }
        });
    }

    private void showContactChecked(final List<String> previousCalls) {
        contactChecked.setVisibility(View.VISIBLE);
        contactChecked.setImageLevel(1);
        contactChecked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = TextUtils.join(", ", previousCalls)
                        .replace(RepCallActivity.VOICEMAIL, getResources().getString(
                                R.string.voicemail_btn))
                        .replace(RepCallActivity.CONTACTED, getResources().getString(
                                R.string.made_contact_btn))
                        .replace(RepCallActivity.UNAVAILABLE, getResources().getString(
                                R.string.unavailable_btn));
                new AlertDialog.Builder(RepCallActivity.this)
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
            if (!TextUtils.isEmpty(contact.field_offices[i].city)) {
                ((TextView) localOfficeInfo.findViewById(R.id.field_office_city)).setText(
                        "- " + contact.field_offices[i].city);
            }
            localOfficeSection.addView(localOfficeInfo);
        }
    }

    private void showError(int errorStringId) {
        Snackbar.make(skipButton, errorStringId, Snackbar.LENGTH_SHORT).show();
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

    private void returnToIssue() {
        if (isFinishing()) {
            return;
        }
        Intent upIntent = NavUtils.getParentActivityIntent(this);
        upIntent.putExtra(IssueActivity.KEY_ISSUE, mIssue);
        NavUtils.navigateUpTo(this, upIntent);
    }
}
