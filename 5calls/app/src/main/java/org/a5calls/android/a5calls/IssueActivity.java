package org.a5calls.android.a5calls;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

/**
 * More details about an isssue.
 */
public class IssueActivity extends AppCompatActivity {

    public static final String KEY_ISSUE = "key_issue";

    private Issue mIssue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIssue = getIntent().getParcelableExtra(KEY_ISSUE);
        if (mIssue == null) {
            // TODO handle this better?
            finish();
            return;
        }

        setContentView(R.layout.activity_issue);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(mIssue.name);

        ((TextView) findViewById(R.id.issue_name)).setText(mIssue.name);
        ((TextView) findViewById(R.id.issue_description)).setText(mIssue.reason);

        if (mIssue.contacts == null || mIssue.contacts.length == 0) {
            findViewById(R.id.buttons_holder).setVisibility(View.GONE);
            findViewById(R.id.buttons_prompt).setVisibility(View.GONE);
            findViewById(R.id.skip_btn).setVisibility(View.GONE);
            findViewById(R.id.call_this_office).setVisibility(View.GONE);
            findViewById(R.id.no_calls_left).setVisibility(View.VISIBLE);
        } else {
            // TODO: Switch between multiple contacts. Remember contact state, show the one
            // not yet called by this user on this issue.

            ((TextView) findViewById(R.id.contact_name)).setText(mIssue.contacts[0].name);
            Glide.with(getApplicationContext())
                    .load(mIssue.contacts[0].photoURL)
                    .into((ImageView) findViewById(R.id.rep_image));
            TextView phoneText = (TextView) findViewById(R.id.phone_number);
            phoneText.setText(mIssue.contacts[0].phone);
            Linkify.addLinks(phoneText, Linkify.PHONE_NUMBERS);

            // TODO: Handlers for the buttons!
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
}
