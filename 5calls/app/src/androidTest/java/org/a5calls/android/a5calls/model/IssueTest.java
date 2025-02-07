package org.a5calls.android.a5calls.model;

import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.a5calls.android.a5calls.FakeJSONData;
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
            assertEquals(expected.slug, actual.slug);
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
        }
    }

    private ArrayList<Issue> getTestIssues() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        return gson.fromJson(FakeJSONData.ISSUE_DATA, listType);
    }
}