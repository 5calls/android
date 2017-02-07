package org.a5calls.android.a5calls.controller;

import android.content.Context;
import android.os.Build;
import android.preference.DialogPreference;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;

/**
 * Time picker dialog preference
 */
public class TimePreference extends DialogPreference {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TimePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public TimePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TimePreference(Context context) {
        super(context);
    }

    // TODO: Show a time picker in the dialog, and return the result properly.
}
