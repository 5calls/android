package org.a5calls.android.a5calls.controller;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.BuildConfig;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.net.FiveCallsApi;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.util.AnalyticsManager;
import org.a5calls.android.a5calls.util.CustomTabsUtil;

import java.text.NumberFormat;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.VISIBLE;

/**
 * The "About" page.
 */
public class AboutActivity extends AppCompatActivity {
    private static final String TAG = "AboutActivity";
    private static final String EMAIL_CONTENT_TYPE = "message/rfc822";

    private final AccountManager accountManager = AccountManager.Instance;
    private FiveCallsApi.CallRequestListener mStatusListener;

    @BindView(R.id.about_us_btn) Button aboutUsButton;
    @BindView(R.id.contact_us_btn) Button contactUsButton;
    @BindView(R.id.twitter_btn) TextView twitterButton;
    @BindView(R.id.facebook_btn) TextView facebookButton;
    @BindView(R.id.instagram_btn) TextView instagramButton;
    @BindView(R.id.bluesky_btn) TextView blueskyButton;
    @BindView(R.id.rate_us_btn) Button rateUsButton;
    @BindView(R.id.privacy_btn) Button privacyButton;
    @BindView(R.id.version_info) TextView version;
    @BindView(R.id.calls_to_date) TextView callsToDate;
    @BindView(R.id.license_btn) Button licenseButton;
    @BindView(R.id.newsletter_signup_view) View newsletterWrapper;
    @BindView(R.id.newsletter_email) EditText newsletterEmail;
    @BindView(R.id.newsletter_signup_btn) Button newsletterSignupBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);

        getSupportActionBar().setTitle(getString(R.string.about_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        aboutUsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomTabsUtil.launchUrl(AboutActivity.this, Uri.parse(getString(R.string.about_url)));
            }
        });

        setOpenIntentOnClick(
                contactUsButton, getSendEmailIntent(getResources()), getString(R.string.send_email)
        );

        // Testing note: to see the Intent Chooser, install some Twitter apps other than the official Twitter
        // app. The Intent Chooser will show browser apps and third-party Twitter apps. If the official
        // Twitter app is installed, it will be opened directly without asking the user which app to open.
        setOpenIntentOnClick(
                twitterButton,
                getActionIntent(getString(R.string.twitter_url)),
                getString(R.string.open_social_media, getString(R.string.twitter_btn))
        );

        setOpenIntentOnClick(
                facebookButton,
                getActionIntent(getString(R.string.facebook_url)),
                getString(R.string.open_social_media, getString(R.string.facebook_btn))
        );

        setOpenIntentOnClick(
                instagramButton,
                getActionIntent(getString(R.string.instagram_url)),
                getString(R.string.open_social_media, getString(R.string.instagram_btn))
        );

        setOpenIntentOnClick(
                blueskyButton,
                getActionIntent(getString(R.string.bluesky_url)),
                getString(R.string.open_social_media, getString(R.string.bluesky_btn))
        );

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

        privacyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://5calls.org/privacy")));
            }
        });

        licenseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOpenSourceLicenses();
            }
        });

        if (!accountManager.isNewsletterSignUpCompleted(this)) {
            newsletterWrapper.setVisibility(View.VISIBLE);
            newsletterSignupBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String email = newsletterEmail.getText().toString();
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        newsletterEmail.setError(
                                getResources().getString(R.string.error_email_format));
                        return;
                    }
                    newsletterSignupBtn.setEnabled(false);
                    FiveCallsApi api =
                            AppSingleton.getInstance(getApplicationContext()).getJsonController();
                    api.newsletterSubscribe(email, new FiveCallsApi.NewsletterSubscribeCallback() {
                        @Override
                        public void onSuccess() {
                            accountManager.setNewsletterSignUpCompleted(v.getContext(), true);
                            findViewById(R.id.newsletter_card).setVisibility(View.GONE);
                            findViewById(R.id.newsletter_card_result_success).setVisibility(VISIBLE);
                        }

                        @Override
                        public void onError() {
                            newsletterSignupBtn.setEnabled(true);
                            Snackbar.make(findViewById(R.id.activity_about),
                                    getResources().getString(R.string.newsletter_signup_error),
                                    Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }

        underlineButtons();

        version.setText(String.format(getResources().getString(R.string.version_info),
                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

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
            public void onReportReceived(int count, boolean donateOn) {
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
        controller.getReport();

        new AnalyticsManager().trackPageview("/about", this);
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

        if (accountManager.isNewsletterSignUpCompleted(this)) {
            newsletterWrapper.setVisibility(View.GONE);
        }
    }

    private void underlineButtons() {
        underlineText(aboutUsButton);
        underlineText(contactUsButton);
        underlineText(twitterButton);
        underlineText(facebookButton);
        underlineText(instagramButton);
        underlineText(blueskyButton);
        underlineText(rateUsButton);
        underlineText(privacyButton);
        underlineText(licenseButton);
    }

    /**
     * Underlines text in the given {@code TextView}.
     */
    private static void underlineText(final TextView textView) {
        textView.setPaintFlags(textView.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);
    }

    // Inspired by https://www.bignerdranch.com/blog/open-source-licenses-and-android/
    private void showOpenSourceLicenses() {
        @SuppressLint("InflateParams")
        WebView view = (WebView) LayoutInflater.from(this).inflate(R.layout.licence_view, null);
        view.loadUrl("file:///android_asset/licenses.html");
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.license_btn))
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Sets the on click listener of the given {@link View} to launch the given {@link Intent}
     * with a chooser with the given prompt.
     */
    private void setOpenIntentOnClick(final View view,
                                      final Intent intent,
                                      final String prompt) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final Intent intentChooser = Intent.createChooser(
                        intent, prompt
                );
                startActivity(intentChooser);
            }
        });
    }

    /**
     * @return an {@link Intent} that allows the user to send an email to 5 Calls
     */
    private static Intent getSendEmailIntent(final Resources resources) {
        final String[] emailAddress = {resources.getString(R.string.email_address)};
        final String subject = resources.getString(R.string.email_subject);

        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_EMAIL, emailAddress);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.setType(EMAIL_CONTENT_TYPE);

        return intent;
    }

    /**
     * @return an {@link Intent} that opens the given {@code url}
     */
    private static Intent getActionIntent(final String url) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        return intent;
    }
}
