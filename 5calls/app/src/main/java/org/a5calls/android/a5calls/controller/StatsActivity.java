package org.a5calls.android.a5calls.controller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.databinding.ActivityStatsBinding;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Tell the user how great they are!
 */
public class StatsActivity extends AppCompatActivity {
    private static final String TAG = "StatsActivity";
    private static final int NUM_CONTACTS_TO_SHOW = 3;
    private static final int NUM_ISSUES_TO_SHOW = 5;
    private static final long MIN_DAYS_FOR_LINE_GRAPH = 3;
    private SimpleDateFormat dateFormat;
    private int mCallCount = 0;
    private ActivityStatsBinding binding;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityStatsBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        dateFormat = new SimpleDateFormat(getResources().getString(R.string.date_format), Locale.US);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() |
                            WindowInsetsCompat.Type.displayCutout());
            binding.appbar.setPadding(insets.left, insets.top, insets.right, 0);
            binding.scrollView.setPadding(insets.left, 0, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        DatabaseHelper db = AppSingleton.getInstance(this).getDatabaseHelper();
        initializeUI(db);

        FiveCallsApplication.analyticsManager().trackPageview("/stats", this);
    }

    private void initializeUI(DatabaseHelper db) {
        mCallCount = db.getCallsCount();
        if (mCallCount == 0) {
            // Show a "no impact yet!" message.
            binding.noCallsMessage.setVisibility(View.VISIBLE);
            binding.statsHolder.setVisibility(View.GONE);
            binding.lineChart.setVisibility(View.GONE);
            return;
        }

        binding.noCallsMessage.setVisibility(View.GONE);
        binding.statsHolder.setVisibility(View.VISIBLE);
        binding.yourCallCount.setText(getStringForCount(
                mCallCount, R.string.your_call_count_one, R.string.your_call_count));

        List<Long> contacts = db.getCallTimestampsForType(Outcome.Status.CONTACT);
        List<Long> voicemails = db.getCallTimestampsForType(Outcome.Status.VOICEMAIL);
        List<Long> unavailables = db.getCallTimestampsForType(Outcome.Status.UNAVAILABLE);

        binding.pieChart.setContentDescription(TextUtils.concat(
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
        int numContactsShown = 0;
        for (Pair<String, Integer> contactStat : contactStats) {
            String name = db.getContactName(contactStat.first);
            if (TextUtils.isEmpty(name)) {
                name = getResources().getString(R.string.unknown_contact);
            }
            TextView contactStatView = (TextView) inflater.inflate(R.layout.stat_textview,
                    null);
            binding.repStats.addView(contactStatView);
            contactStatView.setText(contactStat.second == 1 ?
                    String.format(callFormatStringOne, name) :
                    String.format(callFormatString, contactStat.second, name));
            numContactsShown++;
            if (numContactsShown >= NUM_CONTACTS_TO_SHOW) {
                break;
            }
        }

        List<Pair<String, Integer>> issueStats = db.getCallCountsByIssue();
        int numIssuesShown = 0;
        for (Pair<String, Integer> issueStat : issueStats) {
            String name = db.getIssueName(issueStat.first);
            if (TextUtils.isEmpty(name)) {
                name = getResources().getString(R.string.unknown_issue);
            }
            TextView issueStatView = (TextView) inflater.inflate(R.layout.stat_textview,
                    null);
            binding.callStats.addView(issueStatView);
            issueStatView.setText(issueStat.second == 1 ?
                    String.format(callFormatStringOne, name) :
                    String.format(callFormatString, issueStat.second, name));
            numIssuesShown++;
            if (numIssuesShown >= NUM_ISSUES_TO_SHOW) {
                break;
            }
        }

        // Find first time when user made any call.
        long firstTimestamp = Long.MAX_VALUE;
        long lastTimestamp = Long.MIN_VALUE;
        if (!contacts.isEmpty()) {
            firstTimestamp = Math.min(firstTimestamp, contacts.get(0));
            lastTimestamp = Math.max(lastTimestamp, contacts.get(contacts.size() - 1));
        }
        if (!voicemails.isEmpty()) {
            firstTimestamp = Math.min(firstTimestamp, voicemails.get(0));
            lastTimestamp = Math.max(lastTimestamp, voicemails.get(voicemails.size() - 1));
        }
        if (!unavailables.isEmpty()) {
            firstTimestamp = Math.min(firstTimestamp, unavailables.get(0));
            lastTimestamp = Math.max(lastTimestamp, unavailables.get(unavailables.size() - 1));
        }

        createPieChart(contacts, voicemails, unavailables, firstTimestamp);

        if (lastTimestamp - firstTimestamp > MIN_DAYS_FOR_LINE_GRAPH * 24 * 60 * 60 * 1000) {
            createLineGraph(contacts, voicemails, unavailables, firstTimestamp);
        }

        // Show the share button.
        invalidateOptionsMenu();
    }

    private void createPieChart(List<Long> contacts, List<Long> voicemails, List<Long> unavailables, long firstTimestamp) {
        Date date = new Date(firstTimestamp);

        ArrayList<Integer> colorsList = new ArrayList<>();
        // Create pie pieChart data entries and add correct colors.
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (!contacts.isEmpty()) {
            entries.add(new PieEntry(contacts.size(), getResources().getString(R.string.contact_n)));
            colorsList.add(getResources().getColor(R.color.contacted_color));
        }
        if (!voicemails.isEmpty()) {
            entries.add(new PieEntry(voicemails.size(), getResources().getString(R.string.voicemail_n)));
            colorsList.add(getResources().getColor(R.color.voicemail_color));
        }
        if (!unavailables.isEmpty()) {
            entries.add(new PieEntry(unavailables.size(), getResources().getString(R.string.unavailable_n)));
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
                + "\n"
                + Integer.toString(voicemails.size() + contacts.size() + unavailables.size())
                + "\n"
                + this.dateFormat.format(date)
                + "-"
                + this.dateFormat.format(new Date(System.currentTimeMillis()))
        );
        insideCircleText.setSpan(new RelativeSizeSpan(2f), 0, insideCircleText.length() - 17, 0);

        binding.pieChart.setData(data);
        binding.pieChart.setCenterText(insideCircleText);
        binding.pieChart.setCenterTextColor(getResources().getColor(R.color.colorPrimaryDark));
        binding.pieChart.setCenterTextSize(11f);
        binding.pieChart.setHoleRadius(70);
        binding.pieChart.setEntryLabelColor(getResources().getColor(R.color.colorPrimaryDark));
        binding.pieChart.getLegend().setEnabled(false);
        binding.pieChart.setDescription(new Description());
        binding.pieChart.getDescription().setText("");
        binding.pieChart.invalidate();
    }

    private void createLineGraph(List<Long> contacts, List<Long> voicemails,
                                 List<Long> unavailables, long firstTimestamp) {
        binding.lineChart.setVisibility(View.VISIBLE);
        binding.lineChartTitle.setVisibility(View.VISIBLE);
        LineGraphSeries<DataPoint> contactedSeries = makeSeries(contacts, firstTimestamp,
                R.color.contacted_color);
        contactedSeries.setTitle(getResources().getString(R.string.outcome_contact));
        LineGraphSeries<DataPoint> voicemailSeries = makeSeries(voicemails, firstTimestamp,
                R.color.voicemail_color);
        voicemailSeries.setTitle(getResources().getString(R.string.outcome_voicemail));
        LineGraphSeries<DataPoint> unavailableSeries = makeSeries(unavailables, firstTimestamp,
                R.color.unavailable_color);
        unavailableSeries.setTitle(getResources().getString(R.string.outcome_unavailable));
        binding.lineChart.addSeries(contactedSeries);
        binding.lineChart.addSeries(voicemailSeries);
        binding.lineChart.addSeries(unavailableSeries);

        binding.lineChart.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this));
        binding.lineChart.getGridLabelRenderer().setNumHorizontalLabels(getResources().getInteger(
                R.integer.horizontal_labels_count));
        binding.lineChart.getGridLabelRenderer().setNumVerticalLabels(5);
        binding.lineChart.getGridLabelRenderer().setGridColor(
                getResources().getColor(android.R.color.white));
        binding.lineChart.getGridLabelRenderer().setHumanRounding(false, true);
        binding.lineChart.getViewport().setMinX(firstTimestamp - 10);
        binding.lineChart.getViewport().setMaxX(System.currentTimeMillis() + 10);
        binding.lineChart.getViewport().setXAxisBoundsManual(true);

        // Pad the Y axis so the legend fits.
        int max = Math.max(Math.max(contacts.size(), voicemails.size()), unavailables.size());
        int buffer = (int) Math.ceil(max / 4.0);
        binding.lineChart.getViewport().setMaxY(max + buffer);
        binding.lineChart.getViewport().setMinY(0);
        binding.lineChart.getViewport().setYAxisBoundsManual(true);

        binding.lineChart.getLegendRenderer().setVisible(true);
        binding.lineChart.getLegendRenderer().setBackgroundColor(
                getResources().getColor(android.R.color.transparent));
        binding.lineChart.getLegendRenderer().setFixedPosition(0, 0);

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
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.menu_share) {
            sendShare();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendShare() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(
                R.string.share_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                String.format(getResources().getString(R.string.share_content), mCallCount));

        if (binding.lineChart.getVisibility() == View.VISIBLE) {
            Uri imageUri = saveGraphImage();
            shareIntent.setDataAndType(imageUri, "image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            shareIntent.setType("text/plain");
        }
        startActivity(Intent.createChooser(shareIntent,
                getResources().getString(R.string.share_chooser_title)));

        // Could send analytics here.
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
        binding.lineChart.setTitle(getString(R.string.impact_calls_over_time));

        // From https://stackoverflow.com/questions/5536066/convert-view-to-bitmap-on-android.
        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(binding.lineChart.getWidth(),
                binding.lineChart.getHeight(),
                Bitmap.Config.ARGB_8888);
        //Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        // Draw a background
        canvas.drawColor(Color.WHITE);
        // draw the view on the canvas
        binding.lineChart.draw(canvas);
        binding.lineChart.getLegendRenderer().draw(canvas);

        // Undo the title.
        binding.lineChart.setTitle("");

        //return the bitmap
        return returnedBitmap;
    }

    private String getStringForCount(int count, int resIdOne, int resIdFormat) {
        return count == 1 ? getResources().getString(resIdOne) :
                String.format(getResources().getString(resIdFormat), count);
    }
}
