package org.a5calls.android.a5calls.controller;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.preference.DialogPreference;
import android.support.annotation.RequiresApi;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Time picker dialog preference
 */
public class TimePreference extends DialogPreference {

    private TimePicker mTimePicker;

    private int mHour;
    private int mMinute;
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
        int storedMinutes = AccountManager.Instance.getNotificationMinutes(getContext());
        mHour = storedMinutes / 60;
        mMinute = storedMinutes % 60;
        mIs24HourView = DateFormat.is24HourFormat(getContext());
        updateSummary();
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mTimePicker = (TimePicker) view.findViewById(R.id.time_picker);
        mTimePicker.setIs24HourView(mIs24HourView);
        mTimePicker.setCurrentHour(mHour);
        mTimePicker.setCurrentMinute(mMinute);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setTitle(null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        mHour = mTimePicker.getCurrentHour();
        mMinute = mTimePicker.getCurrentMinute();

        if (positiveResult) {
            AccountManager.Instance.setNotificationMinutes(getContext(), mHour * 60 + mMinute);
            updateSummary();
        }
    }

    private void updateSummary() {
        Date date = new Date();
        date.setHours(mHour);
        date.setMinutes(mMinute);
        SimpleDateFormat format;
        if (mIs24HourView) {
            format = new SimpleDateFormat("HH:mm", Locale.getDefault());
        } else {
            format = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        }
        setSummary(format.format(date));

    }
}
