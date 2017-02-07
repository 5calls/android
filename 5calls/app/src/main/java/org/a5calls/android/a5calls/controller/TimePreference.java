package org.a5calls.android.a5calls.controller;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.preference.DialogPreference;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import org.a5calls.android.a5calls.R;

/**
 * Time picker dialog preference
 */
public class TimePreference extends DialogPreference {

    private TimePicker mTimePicker;

    private int mInitialHourOfDay;
    private int mInitialMinute;
    private boolean mIs24HourView;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TimePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public TimePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TimePreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setPersistent(false);
        setDialogLayoutResource(R.layout.time_preference_dialog);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mTimePicker = (TimePicker) view.findViewById(R.id.time_picker);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setTitle(null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        // TODO save the result somehow
    }

    /**
     * Sets the current time.
     *
     * @param hourOfDay The current hour within the day.
     * @param minuteOfHour The current minute within the hour.
     */
    public void updateTime(int hourOfDay, int minuteOfHour) {
        mTimePicker.setCurrentHour(hourOfDay);
        mTimePicker.setCurrentMinute(minuteOfHour);
    }

    // TODO: Show a time picker in the dialog, and return the result properly.
}
