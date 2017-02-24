package org.a5calls.android.a5calls.controller;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.BuildConfig;
import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.FiveCallsApi;
import org.a5calls.android.a5calls.model.Issue;
import org.a5calls.android.a5calls.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * The "About" page.
 */
public class AboutActivity extends AppCompatActivity {
    private static final String TAG = "AboutActivity";

    private final AccountManager accountManager = AccountManager.Instance;
    private FiveCallsApi.CallRequestListener mStatusListener;

    @BindView(R.id.about_us_btn) Button aboutUsButton;
    @BindView(R.id.rate_us_btn) Button rateUsButton;
    @BindView(R.id.version_info) TextView version;
    @BindView(R.id.calls_to_date) TextView callsToDate;
    @BindView(R.id.license_btn) Button licenseButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);

        getSupportActionBar().setTitle(getResources().getString(R.string.about_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        aboutUsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Add this to the app instead of going to the browser
                // TODO: Meanwhile, get it to open such that the back button returns here.
                Uri uriUrl = Uri.parse("https://5calls.org/#about");
                Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                startActivity(intent);
            }
        });

        rateUsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // From http://stackoverflow.com/questions/11753000/how-to-open-the-google-play-store-directly-from-my-android-application
                final String appPackageName = getPackageName();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                            "market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException ex) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                            "https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
            }
        });

        licenseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOpenSourceLicenses();
            }
        });

        version.setText(String.format(getResources().getString(R.string.version_info),
                BuildConfig.VERSION_NAME));

        mStatusListener = new FiveCallsApi.CallRequestListener() {
            @Override
            public void onRequestError() {
                Snackbar.make(aboutUsButton,
                        getResources().getString(R.string.request_error),
                        Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onJsonError() {
                Snackbar.make(aboutUsButton,
                        getResources().getString(R.string.json_error),
                        Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onCallCount(int count) {
                callsToDate.setText(String.format(
                        getResources().getString(R.string.calls_to_date),
                        NumberFormat.getNumberInstance(Locale.US).format(count)));
            }

            @Override
            public void onCallReported() {
                // unused
            }
        };
        FiveCallsApi controller = AppSingleton.getInstance(getApplicationContext())
                .getJsonController();
        controller.registerCallRequestListener(mStatusListener);
        controller.getCallCount();
    }

    @Override
    protected void onDestroy() {
        AppSingleton.getInstance(getApplicationContext()).getJsonController()
                .unregisterCallRequestListener(mStatusListener);
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
        if (accountManager.allowAnalytics(this)) {
            // Obtain the shared Tracker instance.
            FiveCallsApplication application = (FiveCallsApplication) getApplication();
            Tracker tracker = application.getDefaultTracker();
            tracker.setScreenName(TAG);
            tracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
    }

    // Inspired by https://www.bignerdranch.com/blog/open-source-licenses-and-android/
    private void showOpenSourceLicenses() {
        WebView view = (WebView) LayoutInflater.from(this).inflate(R.layout.licence_view, null);
        view.loadUrl("file:///android_asset/licenses.html");
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.license_btn))
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
