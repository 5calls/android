package org.a5calls.android.a5calls.controller;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for RepCallActivity.
 */
@RunWith(AndroidJUnit4.class)
public class RepCallActivityUnitTest {

    @Test
    public void TestGetReportedActionsMessage_VoicemailOnly() {
        List<String> previousActions = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            previousActions.add("voicemail");
            String result = RepCallActivity.getReportedActionsMessage(getApplicationContext(),
                    previousActions);
            String expectedNumber = String.valueOf(i);
            assertTrue(result.startsWith(expectedNumber));
            assertEquals(expectedNumber + ": left voicemail\n", result);
        }
    }

    @Test
    public void TestGetReportedActionsMessage_ContactOnly() {
        List<String> previousActions = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            previousActions.add("contact");
            String result = RepCallActivity.getReportedActionsMessage(getApplicationContext(),
                    previousActions);
            String expectedNumber = String.valueOf(i);
            assertTrue(result.startsWith(expectedNumber));
            assertEquals(expectedNumber + ": made contact\n", result);
        }
    }

    @Test
    public void TestGetReportedActionsMessage_UnavailableOnly() {
        List<String> previousActions = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            previousActions.add("unavailable");
            String result = RepCallActivity.getReportedActionsMessage(getApplicationContext(),
                    previousActions);
            String expectedNumber = String.valueOf(i);
            assertTrue(result.startsWith(expectedNumber));
            assertEquals(expectedNumber + ": unavailable", result);
        }
    }

    @Test
    public void TestGetReportedActionsMessage() {
        List<String> previousActions = new ArrayList<>();

        // 3 contact, 2 unavailable, 1 voicemail, random order.
        previousActions.add("contact");
        previousActions.add("unavailable");
        previousActions.add("voicemail");
        previousActions.add("contact");
        previousActions.add("unavailable");
        previousActions.add("contact");

        String result = RepCallActivity.getReportedActionsMessage(getApplicationContext(),
                previousActions);
        assertEquals("3: made contact\n" +
                "1: left voicemail\n" +
                "2: unavailable", result);
    }
}
