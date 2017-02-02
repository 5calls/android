package org.a5calls.android.a5calls.controller;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.BuildConfig;
import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.model.Issue;
import org.a5calls.android.a5calls.model.JsonController;
import org.a5calls.android.a5calls.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * The "About" page.
 */
public class AboutActivity extends AppCompatActivity {
    private static final String TAG = "AboutActivity";

    private JsonController.RequestStatusListener mStatusListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        getSupportActionBar().setTitle(getResources().getString(R.string.about_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.about_us_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Add this to the app instead of going to the browser
                // TODO: Meanwhile, get it to open such that the back button returns here.
                Uri uriUrl = Uri.parse("https://5calls.org/#about");
                Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                startActivity(intent);
            }
        });

        TextView version = (TextView) findViewById(R.id.version_info);
        version.setText(String.format(getResources().getString(R.string.version_info),
                BuildConfig.VERSION_NAME));

        // TODO: Find the correct URL for Why Calling Works
        /*findViewById(R.id.why_calling_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Add this to the app instead of going to the browser
                Uri uriUrl = Uri.parse("https://5calls.org/");
                Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                startActivity(intent);
            }
        });*/

        mStatusListener = new JsonController.RequestStatusListener() {
            @Override
            public void onRequestError() {
                Snackbar.make(findViewById(R.id.about_us_btn),
                        getResources().getString(R.string.request_error),
                        Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onJsonError() {
                Snackbar.make(findViewById(R.id.about_us_btn),
                        getResources().getString(R.string.json_error),
                        Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onIssuesReceived(String locationName, List<Issue> issues) {
                // unused
            }

            @Override
            public void onCallCount(int count) {
                TextView callsToDate = (TextView) findViewById(R.id.calls_to_date);
                callsToDate.setText(String.format(
                        getResources().getString(R.string.calls_to_date),
                        NumberFormat.getNumberInstance(Locale.US).format(count)));
            }

            @Override
            public void onCallReported() {
                // unused
            }
        };
        JsonController controller = AppSingleton.getInstance(getApplicationContext())
                .getJsonController();
        controller.registerStatusListener(mStatusListener);
        controller.getCallCount();
    }

    @Override
    protected void onDestroy() {
        AppSingleton.getInstance(getApplicationContext()).getJsonController()
                .unregisterStatusListener(mStatusListener);
        super.onDestroy();
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
    protected void onResume() {
        super.onResume();
        // We allow Analytics opt-out.
        SharedPreferences pref = getSharedPreferences(MainActivity.PREFS_FILE, MODE_PRIVATE);
        if (pref.getBoolean(MainActivity.KEY_ALLOW_ANALYTICS, true)) {
            // Obtain the shared Tracker instance.
            FiveCallsApplication application = (FiveCallsApplication) getApplication();
            Tracker tracker = application.getDefaultTracker();
            tracker.setScreenName(TAG);
            tracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
    }
}
