package org.a5calls.android.a5calls;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

/**
 * More details about an isssue.
 */
public class IssueActivity extends AppCompatActivity {

    public static final String KEY_ISSUE = "key_issue";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Issue issue = getIntent().getParcelableExtra(KEY_ISSUE);
        if (issue == null) {
            // TODO handle this state better
            finish();
            return;
        }

        setContentView(R.layout.activity_issue);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(issue.name);

        ((TextView) findViewById(R.id.issue_name)).setText(issue.name);
        ((TextView) findViewById(R.id.issue_description)).setText(issue.reason);

        if (issue.contacts != null && issue.contacts.length > 0) {
            // TODO: Switch between multiple contacts. Remember contact state, show the one
            // not yet called by this user on this issue.
            ((TextView) findViewById(R.id.contact_name)).setText(issue.contacts[0].name);
            Glide.with(getApplicationContext())
                    .load(issue.contacts[0].photoURL)
                    .into((ImageView) findViewById(R.id.rep_image));

            TextView phoneText = (TextView) findViewById(R.id.phone_number);
            phoneText.setText(issue.contacts[0].phone);
            Linkify.addLinks(phoneText, Linkify.PHONE_NUMBERS);
        }

        // TODO: Add all the reset of the info, including a call button.
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
