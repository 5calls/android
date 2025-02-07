package org.a5calls.android.a5calls.controller;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ShareActionProvider;
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

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.DatabaseHelper;
import org.a5calls.android.a5calls.model.Outcome;
import org.a5calls.android.a5calls.util.AnalyticsManager;

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
    private ShareActionProvider mShareActionProvider;

    @BindView(R.id.no_calls_message) TextView noCallsMessage;
    @BindView(R.id.stats_holder) LinearLayout statsHolder;
    @BindView(R.id.your_call_count) TextView callCountHeader;
    @BindView(R.id.pie_chart) PieChart pieChart;
    @BindView(R.id.line_chart) GraphView lineChart;


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

        new AnalyticsManager().trackPageview("/stats", this);
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

        createPieChart(contacts, voicemails, unavailables, firstTimestamp);
        createLineGraph(contacts, voicemails, unavailables, firstTimestamp);

        // Show the share button.
        invalidateOptionsMenu();
    }

    private void createPieChart(List<Long> contacts, List<Long> voicemails, List<Long> unavailables, long firstTimestamp) {
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

    private void createLineGraph(List<Long> contacts, List<Long> voicemails,
                                 List<Long> unavailables, long firstTimestamp) {
        LineGraphSeries<DataPoint> contactedSeries = makeSeries(contacts, firstTimestamp,
                R.color.contacted_color);
        contactedSeries.setTitle(getResources().getString(R.string.outcome_contact));
        LineGraphSeries<DataPoint> voicemailSeries = makeSeries(voicemails, firstTimestamp,
                R.color.voicemail_color);
        voicemailSeries.setTitle(getResources().getString(R.string.outcome_voicemail));
        LineGraphSeries<DataPoint> unavailableSeries = makeSeries(unavailables, firstTimestamp,
            R.color.unavailable_color);
        unavailableSeries.setTitle(getResources().getString(R.string.outcome_unavailable));
        lineChart.addSeries(contactedSeries);
        lineChart.addSeries(voicemailSeries);
        lineChart.addSeries(unavailableSeries);

        lineChart.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this));
        lineChart.getGridLabelRenderer().setNumHorizontalLabels(getResources().getInteger(
                R.integer.horizontal_labels_count));
        lineChart.getGridLabelRenderer().setNumVerticalLabels(5);
        lineChart.getGridLabelRenderer().setGridColor(
                getResources().getColor(android.R.color.white));
        lineChart.getGridLabelRenderer().setHumanRounding(false, true);
        lineChart.getViewport().setMinX(firstTimestamp - 10);
        lineChart.getViewport().setMaxX(System.currentTimeMillis() + 10);
        lineChart.getViewport().setXAxisBoundsManual(true);

        // Pad the Y axis so the legend fits.
        int max = Math.max(Math.max(contacts.size(), voicemails.size()), unavailables.size());
        int buffer = (int) Math.ceil(max / 4.0);
        lineChart.getViewport().setMaxY(max + buffer);
        lineChart.getViewport().setMinY(0);
        lineChart.getViewport().setYAxisBoundsManual(true);

        lineChart.getLegendRenderer().setVisible(true);
        lineChart.getLegendRenderer().setBackgroundColor(
                getResources().getColor(android.R.color.transparent));
        lineChart.getLegendRenderer().setFixedPosition(0, 0);

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
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent,
                getResources().getString(R.string.share_chooser_title)));

//        if (mTracker != null) {
//            mTracker.send(new HitBuilders.EventBuilder()
//                    .setCategory("Share")
//                    .setAction("StatsShare")
//                    .setLabel(mCallCount + " calls")
//                    .setValue(1)
//                    .build());
//        }

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
                "org.a5calls.android.a5calls.file-provider", sharedImage);
    }

    private Bitmap generateGraphBitmap() {
        // Show a title for the share.
        lineChart.setTitle("Calls over time");

        // From https://stackoverflow.com/questions/5536066/convert-view-to-bitmap-on-android.
        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(lineChart.getWidth(),
                lineChart.getHeight(),
                Bitmap.Config.ARGB_8888);
        //Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        // Draw a background
        canvas.drawColor(Color.WHITE);
        // draw the view on the canvas
        lineChart.draw(canvas);
        lineChart.getLegendRenderer().draw(canvas);

        // Undo the title.
        lineChart.setTitle("");

        //return the bitmap
        return returnedBitmap;
    }

    private String getStringForCount(int count, int resIdOne, int resIdFormat) {
        return count == 1 ? getResources().getString(resIdOne) :
                String.format(getResources().getString(resIdFormat), count);
    }
}
