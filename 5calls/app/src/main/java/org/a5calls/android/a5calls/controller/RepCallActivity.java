package org.a5calls.android.a5calls.controller;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.VisibleForTesting;
import androidx.core.app.NavUtils;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.adapter.OutcomeAdapter;
import org.a5calls.android.a5calls.databinding.ActivityRepCallBinding;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.Contact;
import org.a5calls.android.a5calls.model.DatabaseHelper;
import org.a5calls.android.a5calls.model.Issue;
import org.a5calls.android.a5calls.model.Outcome;
import org.a5calls.android.a5calls.net.FiveCallsApi;
import org.a5calls.android.a5calls.util.AnalyticsManager;
import org.a5calls.android.a5calls.util.ScriptReplacements;
import org.a5calls.android.a5calls.util.MarkdownUtil;
import org.a5calls.android.a5calls.view.GridItemDecoration;

import java.util.List;

import static org.a5calls.android.a5calls.controller.IssueActivity.KEY_ISSUE;

/**
 * Activity which handles showing a script for a rep and logging calls.
 */
public class RepCallActivity extends AppCompatActivity {
    private static final String TAG = "RepCallActivity";

    public static final String KEY_ADDRESS = "key_address";
    public static final String KEY_LOCATION_NAME = "key_location_name";

    public static final String KEY_ACTIVE_CONTACT_INDEX = "active_contact_index";
    private static final String KEY_LOCAL_OFFICES_EXPANDED = "local_offices_expanded";

    private FiveCallsApi.CallRequestListener mStatusListener;
    private Issue mIssue;
    private int mActiveContactIndex;
    private OutcomeAdapter outcomeAdapter;

    private ActivityRepCallBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityRepCallBinding.inflate(getLayoutInflater());

        final String address = getIntent().getStringExtra(KEY_ADDRESS);
        mActiveContactIndex = getIntent().getIntExtra(KEY_ACTIVE_CONTACT_INDEX, 0);
        mIssue = getIntent().getParcelableExtra(KEY_ISSUE);
        if (mIssue == null) {
            finish();
            return;
        }

        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(mIssue.name);
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() |
                            WindowInsetsCompat.Type.displayCutout());
            binding.appBarLayout.setPadding(insets.left, insets.top, insets.right, 0);
            binding.scrollView.setPadding(insets.left, 0, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        mStatusListener = new FiveCallsApi.CallRequestListener() {
            @Override
            public void onRequestError() {
                returnToIssueWithServerError();
            }

            @Override
            public void onJsonError() {
                returnToIssueWithServerError();
            }

            @Override
            public void onReportReceived(int count, boolean donateOn) {
                // unused
            }

            @Override
            public void onCallReported() {
                // Note: Skips are not reported.
                returnToIssue();
            }
        };
        FiveCallsApi controller = AppSingleton.getInstance(getApplicationContext())
                .getJsonController();
        controller.registerCallRequestListener(mStatusListener);

        // The markdown view gets focus unless we let the scrollview take it back.
        binding.scrollView.setFocusableInTouchMode(true);
        binding.scrollView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);

        Contact c = mIssue.contacts.get(mActiveContactIndex);
        String baseScript = mIssue.getScriptForContact(c.id);
        String script = ScriptReplacements.replacing(
                this,
                baseScript,
                c,
                getIntent().getStringExtra(KEY_LOCATION_NAME),
                AccountManager.Instance.getUserName(this)
        );
        MarkdownUtil.setUpScript(binding.callScript, script, getApplicationContext());
        binding.callScript.setTextSize(AccountManager.Instance.getScriptTextSize(getApplicationContext()));

        boolean expandLocalOffices = false;
        if (savedInstanceState != null) {
            expandLocalOffices = savedInstanceState.getBoolean(KEY_LOCAL_OFFICES_EXPANDED,
                    false);
        }
        setupContactUi(mActiveContactIndex, expandLocalOffices);

        // If the Issue's Outcome list is somehow empty, use default outcomes
        // See: https://github.com/5calls/android/issues/107
        List<Outcome> issueOutcomes;
        if (mIssue.outcomeModels == null || mIssue.outcomeModels.isEmpty()) {
            issueOutcomes = OutcomeAdapter.DEFAULT_OUTCOMES;
        } else {
            issueOutcomes = mIssue.outcomeModels;
        }

        outcomeAdapter = new OutcomeAdapter(issueOutcomes, new OutcomeAdapter.Callback() {
            @Override
            public void onOutcomeClicked(Outcome outcome) {
                reportEvent(outcome.label);
                reportCall(outcome, address);
            }
        });

        binding.outcomeList.setLayoutManager(
                new GridLayoutManager(this, getSpanCount(RepCallActivity.this)));
        binding.outcomeList.setAdapter(outcomeAdapter);

        int gridPadding = (int) getResources().getDimension(R.dimen.grid_padding);
        binding.outcomeList.addItemDecoration(new GridItemDecoration(gridPadding,
                getSpanCount(RepCallActivity.this)));

        FiveCallsApplication.analyticsManager().trackPageview(String.format("/issue/%s/%s/", mIssue.slug, c.id), this);
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
                binding.fieldOfficeSection.getVisibility() == View.VISIBLE);
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

    private void reportCall(Outcome outcome, String address) {
        outcomeAdapter.setEnabled(false);
        AppSingleton.getInstance(getApplicationContext()).getDatabaseHelper().addCall(mIssue.id,
                mIssue.name, mIssue.contacts.get(mActiveContactIndex).id,
                mIssue.contacts.get(mActiveContactIndex).name, outcome.status.toString(), address);
        AppSingleton.getInstance(getApplicationContext()).getJsonController().reportCall(
                mIssue.id, mIssue.contacts.get(mActiveContactIndex).id, outcome.label, address);
    }

    private void setupContactUi(int index, boolean expandLocalSection) {
        final Contact contact = mIssue.contacts.get(index);
        binding.contactName.setText(contact.name);

        // Set the reason for contacting this rep, using default text if no reason is provided.
        final String contactReasonText = TextUtils.isEmpty(contact.reason)
                ? getResources().getString(R.string.contact_reason_default)
                : contact.reason;
        binding.contactReason.setText(contactReasonText);

        if (!TextUtils.isEmpty(contact.photoURL)) {
            Glide.with(getApplicationContext())
                    .load(contact.photoURL)
                    .centerCrop()
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.baseline_person_52)
                    .into(binding.repImage);
        }

        linkPhoneNumber(binding.phoneNumber, contact.phone);

        if (expandLocalSection) {
            binding.localOfficeButton.setVisibility(View.INVISIBLE);
            expandLocalOfficeSection(contact);
        } else {
            binding.fieldOfficeSection.setVisibility(View.GONE);
            binding.fieldOfficeSection.removeViews(1, binding.fieldOfficeSection.getChildCount() - 1);
            if (contact.field_offices == null || contact.field_offices.length == 0) {
                binding.localOfficeButton.setVisibility(View.GONE);
            } else {
                binding.localOfficeButton.setVisibility(View.VISIBLE);
                binding.localOfficeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        binding.localOfficeButton.setOnClickListener(null);
                        expandLocalOfficeSection(contact);
                    }
                });
            }
        }

        DatabaseHelper dbHelper = AppSingleton.getInstance(this).getDatabaseHelper();
        // Show a bit about whether they've been contacted yet
        final List<String> previousCalls = dbHelper.getCallResults(mIssue.id, contact.id);
        boolean hasCalledToday = dbHelper.hasCalledToday(mIssue.id, contact.id);
        if (hasCalledToday) {
            binding.callToMakeTodayPrompt.setVisibility(View.GONE);
        }
        if (previousCalls.isEmpty()) {
            binding.previousCallStats.setVisibility(View.GONE);
            binding.previousCallDetails.setVisibility(View.GONE);
        } else {
            binding.previousCallStats.setVisibility(View.VISIBLE);
            binding.previousCallDetails.setVisibility(View.VISIBLE);
            if (previousCalls.size() == 1) {
                binding.previousCallStats.setText(getResources().getString(
                        R.string.previous_call_count_one));
            } else {
                binding.previousCallStats.setText(
                        getResources().getString(
                                R.string.previous_call_count_many, previousCalls.size()));
            }
            binding.previousCallDetails.setOnClickListener(v -> {
                showPreviousCallDetails(previousCalls);
            });
        }

        String contactDetails = contact.getDescription(getResources());
        if (TextUtils.isEmpty(contactDetails)) {
            binding.contactDetails.setVisibility(View.GONE);
        } else {
            binding.contactDetails.setText(contactDetails);
        }
    }

    private void showPreviousCallDetails(List<String> previousCalls) {
        new AlertDialog.Builder(RepCallActivity.this)
                .setTitle(R.string.contact_details_dialog_title)
                .setMessage(getReportedActionsMessage(RepCallActivity.this, previousCalls))
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {

                        })
                .show();
    }

    @VisibleForTesting
    static String getReportedActionsMessage(Context context, List<String> previousActions) {
        String result = "";

        if (previousActions != null) {
            int numContacted = 0;
            int numVoicemail = 0;
            int numUnavailable = 0;
            for (String prev : previousActions) {
                if (TextUtils.equals(prev, Outcome.Status.VOICEMAIL.toString())) {
                    numVoicemail++;
                } else if (TextUtils.equals(prev, Outcome.Status.CONTACT.toString())) {
                    numContacted++;
                } else if (TextUtils.equals(prev, Outcome.Status.UNAVAILABLE.toString())) {
                    numUnavailable++;
                }
            }
            if (numContacted > 0) {
                result += numContacted + ": " + Outcome.getDisplayString(context, Outcome.Status.CONTACT) + "\n";
            }
            if (numVoicemail > 0) {
                result += numVoicemail + ": " + Outcome.getDisplayString(context, Outcome.Status.VOICEMAIL) + "\n";
            }
            if (numUnavailable > 0) {
                result += numUnavailable + ": " + Outcome.getDisplayString(context, Outcome.Status.UNAVAILABLE);
            }
        }

        return result;
    }

    private void expandLocalOfficeSection(Contact contact) {
        binding.localOfficeButton.setVisibility(View.INVISIBLE);
        binding.fieldOfficeSection.setVisibility(View.VISIBLE);
        binding.fieldOfficePrompt.setText(String.format(getResources().getString(
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
            linkPhoneNumber(numberView, contact.field_offices[i].phone);
            if (!TextUtils.isEmpty(contact.field_offices[i].city)) {
                ((TextView) localOfficeInfo.findViewById(R.id.field_office_city)).setText(
                        "- " + contact.field_offices[i].city);
            }
            binding.fieldOfficeSection.addView(localOfficeInfo);
        }
    }

    private void showError(int errorStringId) {
        Snackbar.make(binding.scrollView, errorStringId, Snackbar.LENGTH_SHORT).show();
    }

    private void reportEvent(String event) {
        // Could add analytics here.
    }

    private void returnToIssue() {
        if (isFinishing()) {
            return;
        }
        Intent upIntent = NavUtils.getParentActivityIntent(this);
        if (upIntent == null) {
            return;
        }
        upIntent.putExtra(IssueActivity.KEY_ISSUE, mIssue);
        setResult(IssueActivity.RESULT_OK, upIntent);
        finish();
    }

    private void returnToIssueWithServerError() {
        if (isFinishing()) {
            return;
        }
        Intent upIntent = NavUtils.getParentActivityIntent(this);
        if (upIntent == null) {
            return;
        }
        upIntent.putExtra(IssueActivity.KEY_ISSUE, mIssue);
        setResult(IssueActivity.RESULT_SERVER_ERROR, upIntent);
        finish();
    }

    private int getSpanCount(Activity activity) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        double minButtonWidth = activity.getResources().getDimension(R.dimen.min_button_width);

        return (int) (displayMetrics.widthPixels / minButtonWidth);
    }

    private static void linkPhoneNumber(TextView textView, String phoneNumber) {
        textView.setText(phoneNumber);
        Linkify.addLinks(textView, Patterns.PHONE, "tel:",
                Linkify.sPhoneNumberMatchFilter,
                Linkify.sPhoneNumberTransformFilter);
    }
}
