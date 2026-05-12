package org.a5calls.android.a5calls.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.a5calls.android.a5calls.FakeJSONData;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        
        // Add an action to the first issue to test parceling of actions
        Action action = new Action();
        action.type = Action.TYPE_FREEFORM;
        action.title = "Test Action";
        action.body = "Test Body";
        action.buttonText = "Test Button";
        action.buttonURL = "http://test.com";
        issues.get(0).actions = Collections.singletonList(action);

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
            if (expected.actions == null) {
                assertNull(actual.actions);
            } else {
                assertNotNull(actual.actions);
                assertEquals(expected.actions.size(), actual.actions.size());
                for (int j = 0; j < expected.actions.size(); j++) {
                    Action expectedAction = expected.actions.get(j);
                    Action actualAction = actual.actions.get(j);
                    assertEquals(expectedAction.type, actualAction.type);
                    assertEquals(expectedAction.title, actualAction.title);
                    assertEquals(expectedAction.body, actualAction.body);
                    assertEquals(expectedAction.buttonText, actualAction.buttonText);
                    assertEquals(expectedAction.buttonURL, actualAction.buttonURL);
                }
            }
        }
    }

    @Test
    public void testReadActionsFromJSON() {
        String json = "{\"id\":123,\"name\":\"Issue with Action\",\"actions\":[{" +
                "\"type\":\"freeform\"," +
                "\"title\":\"Participate in a survey?\"," +
                "\"body\":\"Would you like to participate in a survey?\"," +
                "\"buttonText\":\"Sign up\"," +
                "\"buttonURL\":\"https://example.com\"" +
                "}]}";
        Gson gson = new GsonBuilder().serializeNulls().create();
        Issue issue = gson.fromJson(json, Issue.class);
        assertNotNull(issue.actions);
        assertEquals(1, issue.actions.size());
        Action action = issue.actions.get(0);
        assertEquals("freeform", action.type);
        assertEquals("Participate in a survey?", action.title);
        assertEquals("Would you like to participate in a survey?", action.body);
        assertEquals("Sign up", action.buttonText);
        assertEquals("https://example.com", action.buttonURL);
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

        assertEquals("Default script for issue", issue.getScriptForContact(""));
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

    @Test
    public void testGetStateNameNoState() {
        Issue issue = TestModelUtils.createIssue("test-issue", "Test Issue", "");
        assertNull(issue.getStateName());
    }

    @Test
    public void testGetStateNameWithNoStateMapping() {
        Issue issue = TestModelUtils.createIssue("test-issue", "Test Issue", "ba");
        assertNull(issue.getStateName());
    }

    @Test
    public void testGetStateNameWithStateSet() {
        Issue issue = TestModelUtils.createIssue("test-issue", "Test Issue", "ca");
        assertEquals("California", issue.getStateName());
    }
}