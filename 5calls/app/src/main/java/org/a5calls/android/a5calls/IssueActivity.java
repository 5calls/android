package org.a5calls.android.a5calls;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

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
