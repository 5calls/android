package org.a5calls.android.a5calls.model;

import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.a5calls.android.a5calls.FakeJSONData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class IssueTest {
    @Test
    public void testReadFromJSON() {
        List<Issue> issues = getTestIssues();
        assertNotNull(issues);
        assertEquals(issues.size(), 14);
    }

    @Test
    public void testReadFromJSONWithUnknownFields() {
        List<Issue> issues = getTestIssuesWithUnknownFields();
        assertNotNull(issues);
        assertEquals(issues.size(), 14);
    }

    @Test
    public void testParcelable() {
        ArrayList<Issue> issues = getTestIssues();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("key", issues);
        ArrayList<Issue> reconstructedIssues = bundle.getParcelableArrayList("key");
        assertEquals(issues.size(), reconstructedIssues.size());
        for (int i = 0; i < issues.size(); i++) {
            Issue expected = issues.get(i);
            Issue actual = reconstructedIssues.get(i);
            assertEquals(expected.id, actual.id);
            assertEquals(expected.name, actual.name);
            assertEquals(expected.permalink, actual.permalink);
            assertEquals(expected.reason, actual.reason);
            assertEquals(expected.script, actual.script);
            assertEquals(expected.link, actual.link);
            assertEquals(expected.linkTitle, actual.linkTitle);
            assertEquals(expected.active, actual.active);
            assertEquals(expected.isSplit, actual.isSplit);
            assertEquals(expected.contacts, actual.contacts);
            assertEquals(expected.contactAreas, actual.contactAreas);
            assertEquals(expected.outcomeModels, actual.outcomeModels);
            assertArrayEquals(expected.categories, actual.categories);
            assertEquals(expected.customizedScripts, actual.customizedScripts);
        }
    }

    private ArrayList<Issue> getTestIssues() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        return gson.fromJson(FakeJSONData.ISSUE_DATA, listType);
    }

    // Add some unrecognized stuff to the JSON response to ensure that API
    // changes won't break the logic.
    private ArrayList<Issue> getTestIssuesWithUnknownFields() {
        JSONArray initialIssues = FakeJSONData.GetIssueJSON();
        for (int i = 0; i < initialIssues.length(); i++) {
            try {
                JSONObject issue = initialIssues.getJSONObject(i);
                issue.put("unrecognizedField", "unrecognized value");
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        return gson.fromJson(initialIssues.toString(), listType);
    }

    @Test
    public void testGetScriptForContactWithCustomizedScripts() {
        Issue issue = TestModelUtils.createIssue("test-issue", "Test Issue");
        issue.script = "Default script for issue";

        ArrayList<CustomizedContactScript> customizedScripts = new ArrayList<>();
        customizedScripts.add(new CustomizedContactScript("contact1", "Custom script for contact 1"));
        customizedScripts.add(new CustomizedContactScript("contact2", "Custom script for contact 2"));
        issue.customizedScripts = customizedScripts;

        assertEquals("Custom script for contact 1", issue.getScriptForContact("contact1"));
        assertEquals("Custom script for contact 2", issue.getScriptForContact("contact2"));
    }

    @Test
    public void testGetScriptForContactFallbackToDefault() {
        Issue issue = TestModelUtils.createIssue("test-issue", "Test Issue");
        issue.script = "Default script for issue";

        ArrayList<CustomizedContactScript> customizedScripts = new ArrayList<>();
        customizedScripts.add(new CustomizedContactScript("contact1", "Custom script for contact 1"));
        issue.customizedScripts = customizedScripts;

        assertEquals("Default script for issue", issue.getScriptForContact("nonexistent_contact"));
    }

    @Test
    public void testGetScriptForContactWithNullCustomizedScripts() {
        Issue issue = TestModelUtils.createIssue("test-issue", "Test Issue");
        issue.script = "Default script for issue";
        issue.customizedScripts = null;

        assertEquals("Default script for issue", issue.getScriptForContact("any_contact"));
    }

    @Test
    public void testGetScriptForContactWithEmptyCustomizedScripts() {
        Issue issue = TestModelUtils.createIssue("test-issue", "Test Issue");
        issue.script = "Default script for issue";
        issue.customizedScripts = new ArrayList<>();

        assertEquals("Default script for issue", issue.getScriptForContact("any_contact"));
    }

    @Test
    public void testGetScriptForContactWithNullContactId() {
        Issue issue = TestModelUtils.createIssue("test-issue", "Test Issue");
        issue.script = "Default script for issue";

        ArrayList<CustomizedContactScript> customizedScripts = new ArrayList<>();
        customizedScripts.add(new CustomizedContactScript("contact1", "Custom script for contact 1"));
        issue.customizedScripts = customizedScripts;

        assertEquals("Default script for issue", issue.getScriptForContact(null));
    }

    @Test
    public void testGetScriptForContactWithEmptyContactId() {
        Issue issue = TestModelUtils.createIssue("test-issue", "Test Issue");
        issue.script = "Default script for issue";

        ArrayList<CustomizedContactScript> customizedScripts = new ArrayList<>();
        customizedScripts.add(new CustomizedContactScript("contact1", "Custom script for contact 1"));
        customizedScripts.add(new CustomizedContactScript("", "Custom script for empty id"));
        issue.customizedScripts = customizedScripts;

        assertEquals("Custom script for empty id", issue.getScriptForContact(""));
    }

    @Test
    public void testGetScriptForContactWithNullCustomizedScriptId() {
        Issue issue = TestModelUtils.createIssue("test-issue", "Test Issue");
        issue.script = "Default script for issue";

        ArrayList<CustomizedContactScript> customizedScripts = new ArrayList<>();
        customizedScripts.add(new CustomizedContactScript(null, "Custom script with null id"));
        customizedScripts.add(new CustomizedContactScript("contact1", "Custom script for contact 1"));
        issue.customizedScripts = customizedScripts;

        assertEquals("Custom script for contact 1", issue.getScriptForContact("contact1"));
        assertEquals("Default script for issue", issue.getScriptForContact("nonexistent"));
    }

    @Test
    public void testGetScriptForContactWithMultipleMatchingIds() {
        Issue issue = TestModelUtils.createIssue("test-issue", "Test Issue");
        issue.script = "Default script for issue";

        ArrayList<CustomizedContactScript> customizedScripts = new ArrayList<>();
        customizedScripts.add(new CustomizedContactScript("contact1", "First custom script"));
        customizedScripts.add(new CustomizedContactScript("contact1", "Second custom script"));
        issue.customizedScripts = customizedScripts;

        assertEquals("First custom script", issue.getScriptForContact("contact1"));
    }

    @Test
    public void testGetScriptForContactWithNullDefaultScript() {
        Issue issue = TestModelUtils.createIssue("test-issue", "Test Issue");
        issue.script = null;

        ArrayList<CustomizedContactScript> customizedScripts = new ArrayList<>();
        customizedScripts.add(new CustomizedContactScript("contact1", "Custom script for contact 1"));
        issue.customizedScripts = customizedScripts;

        assertEquals("Custom script for contact 1", issue.getScriptForContact("contact1"));
        assertNull(issue.getScriptForContact("nonexistent_contact"));
    }
}