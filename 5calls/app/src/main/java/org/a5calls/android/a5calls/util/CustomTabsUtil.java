package org.a5calls.android.a5calls.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.net.Uri;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

import org.a5calls.android.a5calls.R;

public class CustomTabsUtil {
    /**
     * Launches a Chrome custom tab with the app's toolbar color
     * @param context The Activity or Fragment sending the Intent
     * @param uri The URL to navigate to
     */
    public static void launchUrl(Context context, Uri uri) {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                .setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .build();

        try {
            customTabsIntent.launchUrl(context, uri);
        } catch (ActivityNotFoundException exception) {
            // TODO: Make a WebActivity for the .00001% of users who do not have a browser installed
            Toast.makeText(context, "Cannot open web browser", Toast.LENGTH_SHORT).show();
        }
    }
}
