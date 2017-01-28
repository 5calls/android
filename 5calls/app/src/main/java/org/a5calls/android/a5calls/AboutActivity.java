package org.a5calls.android.a5calls;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.List;

/**
 * The "About" page.
 */
public class AboutActivity extends AppCompatActivity {

    private JsonController mJsonController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        getSupportActionBar().setTitle(getResources().getString(R.string.about_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.about_us_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO
            }
        });

        findViewById(R.id.why_calling_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO
            }
        });

        mJsonController = new JsonController(getApplicationContext(),
                new JsonController.RequestStatusListener() {
                    @Override
                    public void onRequestError() {
                        Snackbar.make(findViewById(R.id.activity_main),
                                getResources().getString(R.string.request_error),
                                Snackbar.LENGTH_LONG).show();
                    }

                    @Override
                    public void onJsonError() {
                        Snackbar.make(findViewById(R.id.activity_main),
                                getResources().getString(R.string.json_error),
                                Snackbar.LENGTH_LONG).show();
                    }

                    @Override
                    public void onIssuesReceived(List<Issue> issues) {
                        // unused
                    }

                    @Override
                    public void onCallCount(int count) {
                        TextView callsToDate = (TextView) findViewById(R.id.calls_to_date);
                        callsToDate.setText(String.format(
                                getResources().getString(R.string.calls_to_date), count));
                    }
                });
        mJsonController.getCallCount();
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
