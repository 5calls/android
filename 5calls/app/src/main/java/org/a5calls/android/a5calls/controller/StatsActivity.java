package org.a5calls.android.a5calls.controller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.DatabaseHelper;
import org.a5calls.android.a5calls.model.Outcome;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Tell the user how great they are!
 */
public class StatsActivity extends AppCompatActivity {
    private static final String TAG = "StatsActivity";
    private static final int NUM_CONTACTS_TO_SHOW = 3;
    private SimpleDateFormat dateFormat;
    private int mCallCount = 0;
    private Tracker mTracker;

    @BindView(R.id.no_calls_message) TextView noCallsMessage;
    @BindView(R.id.stats_holder) LinearLayout statsHolder;
    @BindView(R.id.your_call_count) TextView callCountHeader;
    @BindView(R.id.pie_chart) PieChart pieChart;
    @BindView(R.id.line_chart) LineChart lineChart;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stats);
        ButterKnife.bind(this);
        dateFormat = new SimpleDateFormat(getResources().getString(R.string.date_format), Locale.US);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

        pieChart.setContentDescription(TextUtils.concat(
                getStringForCount(contacts.size(), R.string.impact_contact_one, R.string.impact_contact),
                getStringForCount(voicemails.size(), R.string.impact_vm_one, R.string.impact_vm),
                getStringForCount(unavailables.size(), R.string.impact_unavailable_one,
                        R.string.impact_unavailable)));

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

        createPieChart(contacts, voicemails, unavailables);
        createLineGraph(contacts, voicemails, unavailables);

        // Show the share button.
        invalidateOptionsMenu();
    }

    private void createPieChart(List<Long> contacts, List<Long> voicemails, List<Long> unavailables) {
        // Find first time when user made any call.
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
        Date date = new Date(firstTimestamp);

        ArrayList<Integer> colorsList = new ArrayList<>();
        // Create pie pieChart data entries and add correct colors.
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (contacts.size() > 0) {
            entries.add(new PieEntry(contacts.size(), getResources().getString(R.string.contact_n)));
            colorsList.add(getResources().getColor(R.color.contacted_color));
        }
        if (voicemails.size() > 0) {
            entries.add(new PieEntry(voicemails.size(),  getResources().getString(R.string.voicemail_n)));
            colorsList.add(getResources().getColor(R.color.voicemail_color));
        }
        if (unavailables.size() > 0) {
            entries.add(new PieEntry(unavailables.size(),  getResources().getString(R.string.unavailable_n)));
            colorsList.add(getResources().getColor(R.color.unavailable_color));
        }

        PieDataSet dataSet = new PieDataSet(entries, getResources().getString(R.string.menu_stats));

        // Add colors and set visual properties for pie pieChart.
        dataSet.setColors(colorsList);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setValueLinePart1OffsetPercentage(80.f);
        dataSet.setValueLinePart1Length(.1f);
        dataSet.setValueLinePart2Length(.5f);
        dataSet.setValueLineColor(getResources().getColor(R.color.colorPrimaryDark));
        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(18f);
        data.setValueFormatter(new DefaultValueFormatter(0));
        data.setValueTextColor(Color.WHITE);

        SpannableString insideCircleText = new SpannableString(getResources().getString(R.string.stats_summary_total)
                +"\n"
                + Integer.toString(voicemails.size()+contacts.size()+unavailables.size())
                + "\n"
                + this.dateFormat.format(date)
                + "-"
                + this.dateFormat.format(new Date(System.currentTimeMillis()))
        );
        insideCircleText.setSpan(new RelativeSizeSpan(2f), 0, insideCircleText.length() - 17, 0);

        pieChart.setData(data);
        pieChart.setCenterText(insideCircleText);
        pieChart.setCenterTextColor(getResources().getColor(R.color.colorPrimaryDark));
        pieChart.setCenterTextSize(11f);
        pieChart.setHoleRadius(70);
        pieChart.setEntryLabelColor(getResources().getColor(R.color.colorPrimaryDark));
        pieChart.getLegend().setEnabled(false);
        pieChart.setDescription(new Description());
        pieChart.getDescription().setText("");
        pieChart.invalidate();
    }

    private void createLineGraph(List<Long> contacts, List<Long> voicemails, List<Long> unavailables) {
        int maxCallsPerCategory = 0;
        if (contacts.size() > maxCallsPerCategory) {
            maxCallsPerCategory = contacts.size();
        }
        if (voicemails.size() > maxCallsPerCategory) {
            maxCallsPerCategory = voicemails.size();
        }
        if (unavailables.size() > maxCallsPerCategory) {
            maxCallsPerCategory = unavailables.size();
        }
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragDecelerationFrictionCoef(0.9f);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setHighlightPerDragEnabled(false);
        lineChart.setBackgroundColor(Color.WHITE);
        lineChart.setViewPortOffsets(0f, 0f, 0f, 0f);

        setLineChartData(contacts, voicemails, unavailables);
        lineChart.invalidate();

        Legend l = lineChart.getLegend();
        l.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setCenterAxisLabels(true);
        // One day granularity.
        xAxis.setGranularity(24f);
        xAxis.setAxisMaximum(System.currentTimeMillis());
        xAxis.setGridColor(getResources().getColor(R.color.colorPrimaryDark));
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return dateFormat.format(new Date((long) value));
            }
        });
        YAxis axisLeft = lineChart.getAxisLeft();
        axisLeft.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        axisLeft.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        axisLeft.setDrawGridLines(true);
        axisLeft.setGranularityEnabled(false);
        axisLeft.setYOffset(-5f);
        axisLeft.setAxisMinimum(0f);
        axisLeft.setAxisMaximum(maxCallsPerCategory + 1);
        axisLeft.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        axisLeft.setGridColor(getResources().getColor(R.color.colorPrimaryDark));
        axisLeft.setDrawAxisLine(false);
        YAxis axisRight = lineChart.getAxisRight();
        axisRight.setEnabled(false);
    }

    private void setLineChartData(List<Long> contacts, List<Long> voicemails, List<Long> unavailables) {

        LineDataSet contactsSet = createLineData(contacts,
                getResources().getString(R.string.outcome_contact),
                R.color.contacted_color);
        // Set fill color under contacts line.
        contactsSet.setDrawFilled(true);
        contactsSet.setFillColor(getResources().getColor(R.color.contacted_color));
        contactsSet.setFillAlpha(150);
        contactsSet.setFillFormatter(new IFillFormatter() {
            @Override
            public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
                return lineChart.getAxisLeft().getAxisMinimum();
            }
        });

        LineDataSet voicemailsSet = createLineData(voicemails,
                getResources().getString(R.string.outcome_voicemail),
                R.color.voicemail_color);

        LineDataSet unavailablesSet = createLineData(unavailables,
                getResources().getString(R.string.outcome_unavailable),
                R.color.unavailable_color);

        LineData data = new LineData(contactsSet, voicemailsSet, unavailablesSet);
        data.setDrawValues(false);

        lineChart.setData(data);
    }

    private LineDataSet createLineData(List<Long> callsList, String label, int lineColor) {
        ArrayList<Entry> linePoints = new ArrayList<>();
        for (int i = 0; i < callsList.size(); i++) {
            linePoints.add(new Entry(callsList.get(i), (i + 1)));
        }
        // Add last point for current time.
        linePoints.add(new Entry(System.currentTimeMillis(), callsList.size()));
        LineDataSet lineDataSet = new LineDataSet(linePoints, label);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setColor(getResources().getColor(lineColor));
        lineDataSet.setCircleColor(getResources().getColor(lineColor));
        lineDataSet.setLineWidth(1.5f);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        lineDataSet.setValueFormatter(new DefaultValueFormatter(0));
        return lineDataSet;
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
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
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
        return FileProvider.getUriForFile(this,
                "org.a5calls.android.a5calls.fileprovider", sharedImage);
    }

    private Bitmap generateGraphBitmap() {
        return lineChart.getChartBitmap();
    }

    private String getStringForCount(int count, int resIdOne, int resIdFormat) {
        return count == 1 ? getResources().getString(resIdOne) :
                String.format(getResources().getString(resIdFormat), count);
    }
}
