package org.a5calls.android.a5calls;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * More details about an issue, including links to the phone app to call and buttons to record
 * your calls.
 */
public class IssueActivity extends AppCompatActivity {
    private static final String TAG = "IssueActivity";
    public static final String KEY_ISSUE = "key_issue";
    public static final String KEY_ZIP = "key_zip";
    private static final String KEY_ACTIVE_CONTACT_INDEX = "active_contact_index";

    private JsonController.RequestStatusListener mStatusListener;
    private Issue mIssue;
    private int mActiveContactIndex;

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

        mStatusListener = new JsonController.RequestStatusListener() {
            @Override
            public void onRequestError() {
                Snackbar.make(findViewById(R.id.issue_name),
                        getResources().getString(R.string.request_error),
                        Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onJsonError() {
                Snackbar.make(findViewById(R.id.issue_name),
                        getResources().getString(R.string.json_error),
                        Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onIssuesReceived(List<Issue> issues) {
                // unused
            }

            @Override
            public void onCallCount(int count) {
                // unused
            }

            @Override
            public void onCallReported() {
                Log.d(TAG, "call reported successfully!");
                Snackbar.make(findViewById(R.id.issue_name),
                        getResources().getString(R.string.call_reported),
                        Snackbar.LENGTH_SHORT).show();
                tryLoadingNextContact();
            }
        };
        JsonController controller = AppSingleton.getInstance(getApplicationContext())
                .getJsonController();
        controller.registerStatusListener(mStatusListener);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(mIssue.name);

        ((TextView) findViewById(R.id.issue_name)).setText(mIssue.name);
        ((TextView) findViewById(R.id.issue_description)).setText(mIssue.reason);
        ((TextView) findViewById(R.id.call_script)).setText(mIssue.script);

        if (mIssue.contacts == null || mIssue.contacts.length == 0) {
            findViewById(R.id.buttons_holder).setVisibility(View.GONE);
            findViewById(R.id.buttons_prompt).setVisibility(View.GONE);
            findViewById(R.id.skip_btn).setVisibility(View.GONE);
            findViewById(R.id.call_this_office).setVisibility(View.GONE);
            findViewById(R.id.no_calls_left).setVisibility(View.VISIBLE);
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

            findViewById(R.id.skip_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tryLoadingNextContact();
                }
            });

            findViewById(R.id.made_contact_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportCall("contacted", zip);
                }
            });

            findViewById(R.id.unavailable_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportCall("unavailable", zip);
                }
            });

            findViewById(R.id.voicemail_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportCall("vm", zip);
                }
            });
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
        AppSingleton.getInstance(getApplicationContext()).getDatabaseHelper().addCall(mIssue.id,
                mIssue.contacts[mActiveContactIndex].id, callType, zip);
        AppSingleton.getInstance(getApplicationContext()).getJsonController().reportCall(
                mIssue.id, mIssue.contacts[mActiveContactIndex].id, callType, zip);
    }

    private void setupContactUi(int index) {
        ((TextView) findViewById(R.id.contact_name)).setText(mIssue.contacts[index].name);
        ((TextView) findViewById(R.id.contact_reason)).setText(mIssue.contacts[index].reason);
        if (!TextUtils.isEmpty(mIssue.contacts[index].photoURL)) {
            Glide.with(getApplicationContext())
                    .load(mIssue.contacts[index].photoURL)
                    .into((ImageView) findViewById(R.id.rep_image));
        } else {
            findViewById(R.id.rep_image).setVisibility(View.GONE);
        }
        TextView phoneText = (TextView) findViewById(R.id.phone_number);
        phoneText.setText(mIssue.contacts[index].phone);
        Linkify.addLinks(phoneText, Linkify.PHONE_NUMBERS);
    }

    private void tryLoadingNextContact() {
        if (mActiveContactIndex == mIssue.contacts.length - 1) {
            // Done!
            returnToMain();
        } else {
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
}
