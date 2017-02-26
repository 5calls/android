package org.a5calls.android.a5calls.controller;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.DatabaseHelper;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Tell the user how great they are!
 */
public class StatsActivity extends AppCompatActivity {
    private static final String TAG = "StatsActivity";
    private static final int NUM_CONTACTS_TO_SHOW = 3;

    private int mCallCount = 0;
    private int mContactCount = 0;
    private int mVmCount = 0;
    private ShareActionProvider mShareActionProvider;

    @BindView(R.id.no_calls_message) TextView noCallsMessage;
    @BindView(R.id.stats_holder) LinearLayout statsHolder;
    @BindView(R.id.your_call_count) TextView callCountHeader;
    @BindView(R.id.stats_contacted) TextView statsContacted;
    @BindView(R.id.stats_vm) TextView statsVoicemail;
    @BindView(R.id.stats_unavailable) TextView statsUnavailable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stats);
        ButterKnife.bind(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
        callCountHeader.setText(getTextForCount(
                mCallCount, R.string.your_call_count_one, R.string.your_call_count));

        mContactCount = db.getCallsCountForType(IssueActivity.CONTACTED);
        mVmCount = db.getCallsCountForType(IssueActivity.VOICEMAIL);
        int unavailableCount = db.getCallsCountForType(IssueActivity.UNAVAILABLE);
        statsContacted.setText(getTextForCount(
                mContactCount, R.string.impact_contact_one, R.string.impact_contact));
        statsVoicemail.setText(getTextForCount(
                mVmCount, R.string.impact_vm_one, R.string.impact_vm));
        statsUnavailable.setText(getTextForCount(
                unavailableCount, R.string.impact_unavailable_one, R.string.impact_unavailable));

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

        // Show the share button.
        invalidateOptionsMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // We allow Analytics opt-out.
        if (AccountManager.Instance.allowAnalytics(this)) {
            // Obtain the shared Tracker instance.
            FiveCallsApplication application = (FiveCallsApplication) getApplication();
            Tracker tracker = application.getDefaultTracker();
            tracker.setScreenName(TAG);
            tracker.send(new HitBuilders.ScreenViewBuilder().build());
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
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, R.string.share_subject);
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                String.format(getResources().getString(R.string.share_content),
                        mCallCount, mContactCount, mVmCount));
        shareIntent.setType("text/plain");

        startActivity(Intent.createChooser(shareIntent, getResources().getString(
                R.string.share_chooser_title)));
    }

    private String getTextForCount(int count, int resIdOne, int resIdFormat) {
        return count == 1 ? getResources().getString(resIdOne) :
                String.format(getResources().getString(resIdFormat), count);
    }
}
