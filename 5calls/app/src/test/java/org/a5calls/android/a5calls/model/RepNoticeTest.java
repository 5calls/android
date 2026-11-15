package org.a5calls.android.a5calls.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(AndroidJUnit4.class)
public class RepNoticeTest {
    // 2027-01-04T00:00:00Z
    private static final long JAN_4_2027 = 1799020800000L;

    @Test
    public void testReadsNoticeFromRepsResponse() throws JSONException {
        RepNotice notice = RepNotice.fromRepsResponse(new JSONObject(
                "{\"location\":\"San Francisco\",\"representatives\":[],"
                        + "\"notice\":{\"id\":4,\"text\":\" New members take office in January. \","
                        + "\"expiresAt\":\"2027-01-04T00:00:00Z\"}}"));

        assertNotNull(notice);
        assertEquals(4, notice.id);
        assertEquals("New members take office in January.", notice.text);
        assertEquals(new Date(JAN_4_2027), notice.expiresAt);
    }

    @Test
    public void testNoNotice() throws JSONException {
        assertNull(RepNotice.fromRepsResponse(new JSONObject("{\"representatives\":[]}")));
        assertNull(RepNotice.fromRepsResponse(new JSONObject("{\"notice\":null}")));
        assertNull(RepNotice.fromRepsResponse(new JSONObject("{\"notice\":{\"id\":1,\"text\":\"  \"}}")));
        assertNull(RepNotice.fromRepsResponse(null));
    }

    @Test
    public void testNoExpiryIsAlwaysActive() throws JSONException {
        RepNotice notice = RepNotice.fromRepsResponse(new JSONObject(
                "{\"notice\":{\"id\":1,\"text\":\"Hello\",\"expiresAt\":null}}"));

        assertNotNull(notice);
        assertNull(notice.expiresAt);
        assertTrue(notice.isActive(new Date(Long.MAX_VALUE)));
    }

    @Test
    public void testExpiresAtItsExpiry() {
        RepNotice notice = new RepNotice(1, "Hello", new Date(JAN_4_2027));

        assertTrue(notice.isActive(new Date(JAN_4_2027 - 1)));
        // expiry is exclusive, matching the API
        assertFalse(notice.isActive(new Date(JAN_4_2027)));
    }

    @Test
    public void testParsesTimestampsTheApiCanSend() {
        assertEquals(new Date(JAN_4_2027), RepNotice.parseTimestamp("2027-01-04T00:00:00Z"));
        assertEquals(new Date(JAN_4_2027), RepNotice.parseTimestamp("2027-01-04T00:00:00.123456Z"));
        assertEquals(new Date(JAN_4_2027), RepNotice.parseTimestamp("2027-01-03T16:00:00-08:00"));
        assertEquals(new Date(JAN_4_2027), RepNotice.parseTimestamp("2027-01-04T05:30:00+05:30"));
    }

    @Test
    public void testUnreadableTimestampMeansNoExpiry() {
        assertNull(RepNotice.parseTimestamp(null));
        assertNull(RepNotice.parseTimestamp(""));
        assertNull(RepNotice.parseTimestamp("January 2027"));
    }
}
