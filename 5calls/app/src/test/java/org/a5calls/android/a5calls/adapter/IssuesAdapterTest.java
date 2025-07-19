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
        List<Issue> filtered = IssuesAdapter.filterIssuesBySearchText(
                "Government Oversight", issues);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterIssuesBySearchText_matchesCategory_ignoresCapitalization() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(FakeJSONData.ISSUE_DATA, listType);
        List<Issue> filtered = IssuesAdapter.filterIssuesBySearchText(
                "gOvErNmEnT oVeRsIgHt", issues);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterIssuesBySearchText_matchesCategoryAndReason() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(FakeJSONData.ISSUE_DATA, listType);
        List<Issue> filtered = IssuesAdapter.filterIssuesBySearchText("healthcare", issues);
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

    private static final String REGEX_TEST_ISSUE_DATA = """
    [
        {
            "id": 35700,
            "createdAt": "2025-02-05T18:01:44Z",
            "name": "Condemn a US Takeover of Gaza",
            "reason": "During a press conference alongside,",
            "script": "Hi, my name is **[NAME]** and ... from [].\\n\\n",
            "categories": [{"name": "Foreign Affairs"}],
            "contactType": "REPS",
            "contacts": null,
            "contactAreas": ["US House","US Senate"],
            "outcomeModels": [
                {"label": "unavailable","status": "unavailable"},
                {"label": "voicemail","status": "voicemail"},
                {"label": "contact","status": "contact"},
                {"label": "skip","status": "skip"}
            ],
            "stats": { "calls": 0 },
            "slug": "trump-us-gaza-palestinian-occupation",
            "active": true,
            "hidden": false,
            "meta": ""
        }
    ]""";

    @Test
    public void testFilterIssuesBySearchText_matchesReason_matchesStartOfFirstWord() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(REGEX_TEST_ISSUE_DATA, listType);
        List<Issue> filtered = IssuesAdapter.filterIssuesBySearchText("During", issues);
        assertEquals(1, filtered.size());
    }

    @Test
    public void testFilterIssuesBySearchText_matchesReason_noCrashIfSearchTextIsInvalidRegex() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(REGEX_TEST_ISSUE_DATA, listType);
        List<Issue> filtered = IssuesAdapter.filterIssuesBySearchText("[", issues);
        assertEquals(0, filtered.size());
    }

    @Test
    public void testFilterIssuesBySearchText_matchesReason_searchTextNotInterpretedAsRegexPattern() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(REGEX_TEST_ISSUE_DATA, listType);
        List<Issue> filtered = IssuesAdapter.filterIssuesBySearchText(".", issues);
        assertEquals(0, filtered.size());
    }
}