package org.a5calls.android.a5calls.view;

import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import org.a5calls.android.a5calls.R;

import java.util.ArrayList;
import java.util.List;


public class OutcomeView extends LinearLayout {

    public static final String VM = "vm";
    public static final String VOICEMAIL = "voicemail";
    public static final String CONTACT = "contact";
    public static final String CONTACTED = "contacted";
    public static final String UNAVAILABLE = "unavailable";

    private List<Button> buttons;

    public OutcomeView(Context context, String[] outcomes, final Callback callback) {
        super(context);

        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setOrientation(VERTICAL);

        if (outcomes != null && outcomes.length > 0) {
            int orientation = context.getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                setOrientation(VERTICAL);
            } else {
                setOrientation(HORIZONTAL);
            }

            buttons = new ArrayList<>();
            for (final String outcome : outcomes) {
                Button button = new AppCompatButton(context);
                button.setMinHeight(dpsToPixels(context, 48));

                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    button.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                } else {
                    button.setLayoutParams(new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                }
                button.setText(getDisplayString(context, outcome));
                button.setTextColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
                button.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        callback.onOutcomeClick(outcome);
                    }
                });

                buttons.add(button);
                addView(button);
            }
        }
    }

    public static String getDisplayString(Context context, String outcome) {
        String result;

        switch (outcome) {
            case VOICEMAIL:
            case VM:
                result = context.getResources().getString(R.string.voicemail_btn);
                break;
            case UNAVAILABLE:
                result = context.getResources().getString(R.string.unavailable_btn);
                break;
            case CONTACT:
            case CONTACTED:
                result = context.getResources().getString(R.string.made_contact_btn);
                break;
            default:
                result = outcome;
        }

        return result;
    }

    public void setButtonsEnabled(boolean enabled) {
        if (buttons != null) {
            for (Button button : buttons) {
                button.setEnabled(enabled);
            }
        }
    }

    private int dpsToPixels(Context context, int dps) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return  (int) (dps * scale + 0.5f);
    }

    public interface Callback {
        void onOutcomeClick(String outcome);
    }
}
