package org.a5calls.android.a5calls.adapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.a5calls.android.a5calls.FakeJSONData;
import org.a5calls.android.a5calls.model.Issue;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for IssuesAdapter.
 */
@RunWith(AndroidJUnit4.class)
public class IssuesAdapterTest {
    @Test
    public void testFilterIssuesBySearchText_noMatches() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(FakeJSONData.ISSUE_DATA, listType);
        List<Issue> filtered = IssuesAdapter.filterIssuesBySearchText("Pizza", issues);
        assertEquals(0, filtered.size());
    }

    @Test
    public void testFilterIssuesBySearchText_matchesCategory() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(FakeJSONData.ISSUE_DATA, listType);
        List<Issue> filtered = IssuesAdapter.filterIssuesBySearchText("Healthcare", issues);
        assertEquals(4, filtered.size());
    }

    @Test
    public void testFilterIssuesBySearchText_matchesCategory_ignoresCapitalization() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(FakeJSONData.ISSUE_DATA, listType);
        List<Issue> filtered = IssuesAdapter.filterIssuesBySearchText("hEaLtHcArE", issues);
        assertEquals(4, filtered.size());
    }

    @Test
    public void testFilterIssuesBySearchText_matchesTitle() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(FakeJSONData.ISSUE_DATA, listType);
        List<Issue> filtered = IssuesAdapter.filterIssuesBySearchText("stop h.r. 722", issues);
        assertEquals(1, filtered.size());
    }

    @Test
    public void testFilterIssuesBySearchText_matchesReason() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(FakeJSONData.ISSUE_DATA, listType);
        List<Issue> filtered = IssuesAdapter.filterIssuesBySearchText("displacement", issues);
        assertEquals(1, filtered.size());
    }

    @Test
    public void testFilterIssuesBySearchText_matchesReason_twoWords() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(FakeJSONData.ISSUE_DATA, listType);
        List<Issue> filtered = IssuesAdapter.filterIssuesBySearchText("Trump admin", issues);
        assertEquals(4, filtered.size());
    }

    @Test
    public void testFilterIssuesBySearchText_matchesReason_doesNotMatchIfNotStartOfWord() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(FakeJSONData.ISSUE_DATA, listType);
        List<Issue> filtered = IssuesAdapter.filterIssuesBySearchText("isplacement", issues);
        assertEquals(0, filtered.size());
    }
}