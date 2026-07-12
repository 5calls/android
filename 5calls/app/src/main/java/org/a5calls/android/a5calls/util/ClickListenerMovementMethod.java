package org.a5calls.android.a5calls.util;

import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * A LinkMovementMethod that calls a callback when the link is clicked,
 * with the link's text.
 */
public class ClickListenerMovementMethod extends LinkMovementMethod {

    public interface OnLinkClickListener {
        void onLinkClick(String url);
    }

    private final OnLinkClickListener mListener;

    public ClickListenerMovementMethod(OnLinkClickListener listener) {
        mListener = listener;
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        mListener.onLinkClick(buffer.toString());
        return super.onTouchEvent(widget, buffer, event);
    }
}
