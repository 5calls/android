package org.a5calls.android.a5calls.model;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A server-controlled message shown above the caller's reps, e.g. explaining that new members
 * of Congress don't take office until January. The API only sends a notice while it's live.
 */
public class RepNotice {
    // RFC 3339 as Go writes it: optional fractional seconds, then Z or an offset like -08:00.
    private static final Pattern TIMESTAMP = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(?:\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})$");

    public final int id;
    public final String text;
    @Nullable public final Date expiresAt;

    public RepNotice(int id, String text, @Nullable Date expiresAt) {
        this.id = id;
        this.text = text;
        this.expiresAt = expiresAt;
    }

    /**
     * Reads the optional "notice" object from a reps response. Returns null when there's no
     * notice or it has no text.
     */
    @Nullable
    public static RepNotice fromRepsResponse(@Nullable JSONObject response) {
        if (response == null) {
            return null;
        }
        JSONObject notice = response.optJSONObject("notice");
        if (notice == null) {
            return null;
        }
        String text = notice.optString("text", "").trim();
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        String expiresAt = notice.isNull("expiresAt") ? null : notice.optString("expiresAt", null);
        return new RepNotice(notice.optInt("id"), text, parseTimestamp(expiresAt));
    }

    /**
     * The API stops sending a notice once it expires, but a screen can stay open longer than
     * that, so check again before showing it.
     */
    public boolean isActive(Date now) {
        return expiresAt == null || now.before(expiresAt);
    }

    @Nullable
    static Date parseTimestamp(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        Matcher matcher = TIMESTAMP.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        // Parse the wall time as UTC and apply the offset by hand: SimpleDateFormat's ISO
        // offset pattern ("XXX") needs API 24 and we support 23.
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date wallTime = format.parse(matcher.group(1));
            if (wallTime == null) {
                return null;
            }
            String offset = matcher.group(2);
            long offsetMillis = 0;
            if (!"Z".equals(offset)) {
                int sign = offset.charAt(0) == '-' ? -1 : 1;
                int hours = Integer.parseInt(offset.substring(1, 3));
                int minutes = Integer.parseInt(offset.substring(4, 6));
                offsetMillis = sign * (hours * 60L + minutes) * 60_000L;
            }
            return new Date(wallTime.getTime() - offsetMillis);
        } catch (ParseException e) {
            return null;
        }
    }
}
