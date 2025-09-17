package org.a5calls.android.a5calls.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.a5calls.android.a5calls.FakeJSONData;
import org.a5calls.android.a5calls.model.Contact;
import org.a5calls.android.a5calls.model.CustomizedContactScript;
import org.a5calls.android.a5calls.model.Issue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Type;
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

    private Issue testIssue;

    @Before
    public void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        // Use first issue from existing FakeJSONData
        List<Issue> issues = getTestIssues();
        testIssue = issues.get(0); // Use first issue for testing
    }

    private ArrayList<Issue> getTestIssues() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        return gson.fromJson(FakeJSONData.ISSUE_DATA, listType);
    }

    @Test
    public void testIssueReturnsDefaultScriptWhenNoCustomizedScripts() {
        // Given: Issue with no customized scripts (but it might have contacts from JSON data)
        testIssue.customizedScripts = null;

        // When: Getting script for contact (use fake contact ID)
        String script = testIssue.getScriptForContact("fake-contact-id");

        // Then: Returns default script from the JSON test data
        assertEquals(testIssue.script, script);
    }

    @Test
    public void testIssueReturnsCustomizedScriptWhenAvailable() {
        // Given: Issue with customized script for contact
        String testContactId = "test-contact-123";

        CustomizedContactScript customScript = new CustomizedContactScript();
        customScript.id = testContactId;
        customScript.script = "Customized script: Hi [NAME], this is about [ISSUE]";

        testIssue.customizedScripts = new ArrayList<>();
        testIssue.customizedScripts.add(customScript);

        // When: Getting script for contact
        String script = testIssue.getScriptForContact(testContactId);

        // Then: Returns customized script
        assertEquals("Customized script: Hi [NAME], this is about [ISSUE]", script);
    }

    @Test
    public void testIssueReturnsDefaultScriptWhenContactNotFoundInCustomizedScripts() {
        // Given: Issue with customized script for different contact
        String testContactId = "test-contact-123";

        CustomizedContactScript customScript = new CustomizedContactScript();
        customScript.id = "different-contact-id";
        customScript.script = "Different script";

        testIssue.customizedScripts = new ArrayList<>();
        testIssue.customizedScripts.add(customScript);

        // When: Getting script for our test contact
        String script = testIssue.getScriptForContact(testContactId);

        // Then: Returns default script
        assertEquals(testIssue.script, script);
    }

    // Legacy tests below

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
