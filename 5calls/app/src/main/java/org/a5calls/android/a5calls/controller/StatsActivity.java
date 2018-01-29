package org.a5calls.android.a5calls.controller;

import android.app.Dialog;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.authentication.storage.CredentialsManagerException;
import com.auth0.android.callback.BaseCallback;
import com.auth0.android.management.ManagementException;
import com.auth0.android.provider.AuthCallback;
import com.auth0.android.result.Credentials;
import com.auth0.android.result.UserProfile;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.AuthenticationManager;
import org.a5calls.android.a5calls.model.DatabaseHelper;
import org.a5calls.android.a5calls.model.Outcome;
import org.a5calls.android.a5calls.model.User;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Tell the user how great they are!
 */
public class StatsActivity extends LoginActivity {
    private static final String TAG = "StatsActivity";
    private static final int NUM_CONTACTS_TO_SHOW = 3;

    private int mCallCount = 0;
    private ShareActionProvider mShareActionProvider;
    private Tracker mTracker;

    @BindView(R.id.no_calls_message) TextView noCallsMessage;
    @BindView(R.id.stats_holder) LinearLayout statsHolder;
    @BindView(R.id.your_call_count) TextView callCountHeader;
    @BindView(R.id.stats_summary) TextView statsSummary;
    @BindView(R.id.graph) GraphView graph;
    @BindView(R.id.btn_sign_in) Button signInButton;
    @BindView(R.id.sign_in_section) ViewGroup signInSection;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stats);
        ButterKnife.bind(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        User user = AppSingleton.getInstance(this).getAuthenticationManager()
                .getCachedUserProfile(getApplicationContext());
        if (user != null) {
            signInSection.setVisibility(View.GONE);
        } else {
            signInButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    login();
                }
            });
        }

        DatabaseHelper db = AppSingleton.getInstance(this).getDatabaseHelper();
        initializeUI(db);
    }

    private void initializeUI(DatabaseHelper db) {
        mCallCount = db.getCallsCount();
        if (mCallCount == 0) {
            // Show a "no impact yet!" message.
            noCallsMessage.setVisibility(View.VISIBLE);
            statsHolder.setVisibility(View.GONE);
            return;
        }

        noCallsMessage.setVisibility(View.GONE);
        statsHolder.setVisibility(View.VISIBLE);
        callCountHeader.setText(getStringForCount(
                mCallCount, R.string.your_call_count_one, R.string.your_call_count));

        List<Long> contacts = db.getCallTimestampsForType(Outcome.Status.CONTACT);
        List<Long> voicemails = db.getCallTimestampsForType(Outcome.Status.VOICEMAIL);
        List<Long> unavailables = db.getCallTimestampsForType(Outcome.Status.UNAVAILABLE);
        
        Spannable contactString = getTextForCount(contacts.size(),
                R.string.impact_contact_one, R.string.impact_contact, R.color.contacted_color);
        Spannable vmString = getTextForCount(voicemails.size(),
                R.string.impact_vm_one, R.string.impact_vm, R.color.voicemail_color);
        Spannable unavailableString = getTextForCount(unavailables.size(),
                R.string.impact_unavailable_one, R.string.impact_unavailable,
                R.color.unavailable_color);
        statsSummary.setText(TextUtils.concat(contactString, vmString, unavailableString));

        // There's probably not that many contacts because mostly the user just calls their own
        // reps. However, it'd be good to move this to a RecyclerView or ListView with an adapter
        // in the future.
        // TODO optimize this list creation
        List<Pair<String, Integer>> contactStats = db.getCallCountsByContact();
        LayoutInflater inflater = LayoutInflater.from(this);
        String callFormatString = getResources().getString(R.string.contact_call_stat);
        String callFormatStringOne = getResources().getString(R.string.contact_call_stat_one);
        int numToShow = Math.min(NUM_CONTACTS_TO_SHOW, contactStats.size());
        for (int i = 0; i < numToShow; i++) {
            String name = db.getContactName(contactStats.get(i).first);
            if (TextUtils.isEmpty(name)) {
                name = getResources().getString(R.string.unknown_contact);
            }
            TextView contactStat = (TextView) inflater.inflate(R.layout.contact_stat_textview,
                    null);
            statsHolder.addView(contactStat);
            contactStat.setText(contactStats.get(i).second == 1 ?
                    String.format(callFormatStringOne, name) :
                    String.format(callFormatString, contactStats.get(i).second, name));
        }

        /*
        // Listing issues called doesn't seem so useful since most people will make 1-3 calls per
        // issue to the same set of contacts.
        // Can add this in later if there is need.
        List<Pair<String, Integer>> issueStats = db.getCallCountsByIssue();
        for (int i = 0; i < issueStats.size(); i++) {
            String name = db.getIssueName(issueStats.get(i).first);
            if (TextUtils.isEmpty(name)) {
                name = getResources().getString(R.string.unknown_issue);
            }
            Log.d(TAG, name + " had " + issueStats.get(i).second + " calls");
        }
        */

        createGraph(contacts, voicemails, unavailables);

        // Show the share button.
        invalidateOptionsMenu();
    }

    private void createGraph(List<Long> contacts, List<Long> voicemails, List<Long> unavailables) {
        long firstTimestamp = Long.MAX_VALUE;
        if (contacts.size() > 0) {
            firstTimestamp = Math.min(firstTimestamp, contacts.get(0));
        }
        if (voicemails.size() > 0) {
            firstTimestamp = Math.min(firstTimestamp, voicemails.get(0));
        }
        if (unavailables.size() > 0) {
            firstTimestamp = Math.min(firstTimestamp, unavailables.get(0));
        }
        LineGraphSeries<DataPoint> contactedSeries = makeSeries(contacts, firstTimestamp,
                R.color.contacted_color);
        contactedSeries.setTitle(getResources().getString(R.string.outcome_contact));
        LineGraphSeries<DataPoint> voicemailSeries = makeSeries(voicemails, firstTimestamp,
                R.color.voicemail_color);
        voicemailSeries.setTitle(getResources().getString(R.string.outcome_voicemail));
        LineGraphSeries<DataPoint> unavailableSeries = makeSeries(unavailables, firstTimestamp,
                R.color.unavailable_color);
        unavailableSeries.setTitle(getResources().getString(R.string.outcome_unavailable));
        graph.addSeries(contactedSeries);
        graph.addSeries(voicemailSeries);
        graph.addSeries(unavailableSeries);

        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this));
        graph.getGridLabelRenderer().setNumHorizontalLabels(getResources().getInteger(
                R.integer.horizontal_labels_count));
        graph.getGridLabelRenderer().setNumVerticalLabels(5);
        graph.getGridLabelRenderer().setGridColor(getResources().getColor(android.R.color.white));
        graph.getGridLabelRenderer().setHumanRounding(false, true);
        graph.getViewport().setMinX(firstTimestamp - 10);
        graph.getViewport().setMaxX(System.currentTimeMillis() + 10);
        graph.getViewport().setXAxisBoundsManual(true);

        /*
        // Allow manual zoom. Need to make sure the user can't zoom in too much...
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);
        */
    }

    private LineGraphSeries<DataPoint> makeSeries(List<Long> timestamps, long firstTimestamp,
                                                  int colorId) {
        DataPoint[] points = new DataPoint[timestamps.size() + 2];
        // Add a first timestamp so the graphs all start at (0, 0)
        points[0] = new DataPoint(firstTimestamp, 0);
        int count = 0;
        for (int i = 0; i < timestamps.size(); i++) {
            points[i + 1] = new DataPoint(timestamps.get(i), ++count);
        }
        // Add right now to the timestamps, to scale the graph as expected.
        points[timestamps.size() + 1] = new DataPoint(System.currentTimeMillis(), count);

        // Styling
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(points);
        series.setColor(getResources().getColor(colorId));
        series.setThickness(getResources().getDimensionPixelSize(R.dimen.graph_line_width));
        return series;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // We allow Analytics opt-out.
        if (AccountManager.Instance.allowAnalytics(this)) {
            // Obtain the shared Tracker instance.
            FiveCallsApplication application = (FiveCallsApplication) getApplication();
            mTracker = application.getDefaultTracker();
            mTracker.setScreenName(TAG);
            mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mCallCount == 0) {
            return super.onCreateOptionsMenu(menu);
        }
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_stats, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_share:
                sendShare();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendShare() {
        Uri imageUri = saveGraphImage();
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(
                R.string.share_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                String.format(getResources().getString(R.string.share_content), mCallCount));
        shareIntent.setDataAndType(imageUri, "image/png");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // Needed to avoid security exception on KitKat.
            shareIntent.setClipData(ClipData.newRawUri(null, imageUri));
        }
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent,
                getResources().getString(R.string.share_chooser_title)));

        if (mTracker != null) {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Share")
                    .setAction("StatsShare")
                    .setLabel(mCallCount + " calls")
                    .setValue(1)
                    .build());
        }

        startActivity(Intent.createChooser(shareIntent, getResources().getString(
                R.string.share_chooser_title)));
    }

    private Uri saveGraphImage() {
        Bitmap bitmap = generateGraphBitmap();
        File shareFolder = new File(getFilesDir(), "pictures");
        if (!shareFolder.exists()) {
            shareFolder.mkdirs();
        }
        File sharedImage = new File(shareFolder, "5calls_stats.png");
        try {
            FileOutputStream stream = new FileOutputStream(sharedImage);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Uri contentUri = FileProvider.getUriForFile(this,
                "org.a5calls.android.a5calls.fileprovider", sharedImage);
        return contentUri;
    }

    private Bitmap generateGraphBitmap() {
        // Show a title and legend for the share
        graph.setTitle("Calls over time");
        graph.getLegendRenderer().setVisible(true);

        // From https://stackoverflow.com/questions/5536066/convert-view-to-bitmap-on-android.
        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(graph.getWidth(), graph.getHeight(),
                Bitmap.Config.ARGB_8888);
        //Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        // Draw a background
        canvas.drawColor(Color.WHITE);
        // draw the view on the canvas
        graph.draw(canvas);

        // Undo the legend and title.
        graph.setTitle("");
        graph.getLegendRenderer().setVisible(false);

        //return the bitmap
        return returnedBitmap;
    }

    private Spannable getTextForCount(int count, int resIdOne, int resIdFormat, int resIdColor) {
        String string = getStringForCount(count, resIdOne, resIdFormat);
        Spannable result = new SpannableString(string);
        result.setSpan(new ForegroundColorSpan(getResources().getColor(resIdColor)), 0,
                string.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return result;
    }

    private String getStringForCount(int count, int resIdOne, int resIdFormat) {
        return count == 1 ? getResources().getString(resIdOne) :
                String.format(getResources().getString(resIdFormat), count);
    }

    @Override
    public void onLoginSuccess(Credentials credentials, User user) {
        signInSection.setVisibility(View.GONE);
    }

    @Override
    public View getSnackbarView() {
        return findViewById(R.id.stats_activity);
    }
}
