package org.a5calls.android.a5calls.adapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.a5calls.android.a5calls.FakeJSONData;
import org.a5calls.android.a5calls.model.Contact;
import org.a5calls.android.a5calls.model.Issue;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
            "permalink": "/issue/trump-us-gaza-palestinian-occupation",
            "active": true,
            "hidden": false,
            "meta": ""
        }
    ]""";

    /**
     * 2025-07-19 XXX: this test is a companion to
     * {@link #testFilterIssuesBySearchText_matchesReason_doesNotMatchIfNotStartOfWord()}
     * that demonstrates that different criteria are used for matching
     * searchText against titles and reasons.
     */
    @Test
    public void testFilterIssuesBySearchText_matchesTitle_doesMatchEndOfWord() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(REGEX_TEST_ISSUE_DATA, listType);
        List<Issue> filtered = IssuesAdapter.filterIssuesBySearchText("over", issues);
        assertEquals(1, filtered.size());
    }

    @Test
    public void testFilterIssuesBySearchText_matchesReason_matchesStartOfFirstWord() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(REGEX_TEST_ISSUE_DATA, listType);
        List<Issue> filtered = IssuesAdapter.filterIssuesBySearchText("During", issues);
        assertEquals(1, filtered.size());
    }

    @Test
    public void testFilterIssuesBySearchText_matchesReason_matchesWordPrefixes() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(REGEX_TEST_ISSUE_DATA, listType);
        List<Issue> filtered = IssuesAdapter.filterIssuesBySearchText("Press Conf", issues);
        assertEquals(1, filtered.size());
    }

    @Test
    public void testFilterIssuesBySearchText_matchesReason_noCrashIfSearchTextIsInvalidRegex() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(REGEX_TEST_ISSUE_DATA, listType);
        List<Issue> filtered = IssuesAdapter.filterIssuesBySearchText("[", issues);
        assertEquals(0, filtered.size());
        String regexQuotePattern = "\\E[";
        List<Issue> secondFilterAttempt = IssuesAdapter.filterIssuesBySearchText(
            regexQuotePattern, issues
        );
        assertEquals(0, secondFilterAttempt.size());
    }

    @Test
    public void testFilterIssuesBySearchText_matchesReason_searchTextNotInterpretedAsRegexPattern() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(REGEX_TEST_ISSUE_DATA, listType);
        List<Issue> filtered = IssuesAdapter.filterIssuesBySearchText(".", issues);
        assertEquals(0, filtered.size());
    }

    // Test data for issue ordering tests
    private static final String ORDERING_TEST_ISSUE_DATA = """
    [
        {
            "id": "100",
            "name": "Regular Issue A",
            "reason": "Regular issue without meta",
            "script": "Test script",
            "categories": [{"name": "Test"}],
            "contactType": "REPS",
            "contactAreas": ["US House"],
            "outcomeModels": [],
            "stats": { "calls": 0 },
            "active": true,
            "meta": "",
            "sort": 300
        },
        {
            "id": "200",
            "name": "State Issue B",
            "reason": "Issue with CA meta",
            "script": "Test script",
            "categories": [{"name": "Test"}],
            "contactType": "REPS",
            "contactAreas": ["US House"],
            "outcomeModels": [],
            "stats": { "calls": 0 },
            "active": true,
            "meta": "CA",
            "sort": 100
        },
        {
            "id": "300",
            "name": "Regular Issue C",
            "reason": "Another regular issue",
            "script": "Test script",
            "categories": [{"name": "Test"}],
            "contactType": "REPS",
            "contactAreas": ["US House"],
            "outcomeModels": [],
            "stats": { "calls": 0 },
            "active": true,
            "meta": "",
            "sort": 400
        },
        {
            "id": "400",
            "name": "State Issue D",
            "reason": "Issue with NY meta",
            "script": "Test script",
            "categories": [{"name": "Test"}],
            "contactType": "REPS",
            "contactAreas": ["US House"],
            "outcomeModels": [],
            "stats": { "calls": 0 },
            "active": true,
            "meta": "NY",
            "sort": 200
        }
    ]""";

    @Test
    public void testSortIssuesWithMetaPriority_emptyList() {
        IssuesAdapter adapter = new IssuesAdapter(null, null);
        List<Issue> emptyList = new ArrayList<>();
        ArrayList<Issue> result = adapter.sortIssuesWithMetaPriority(emptyList);
        assertEquals(0, result.size());
    }

    @Test
    public void testSortIssuesWithMetaPriority_allWithoutMeta() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(ORDERING_TEST_ISSUE_DATA, listType);
        
        // Remove meta from all issues
        for (Issue issue : issues) {
            issue.meta = "";
        }
        
        IssuesAdapter adapter = new IssuesAdapter(null, null);
        ArrayList<Issue> result = adapter.sortIssuesWithMetaPriority(issues);
        
        assertEquals(4, result.size());
        // Should be sorted by sort field: 100(sort=300), 200(sort=100), 300(sort=400), 400(sort=200)
        // Expected order by sort: 200, 400, 100, 300
        assertEquals("200", result.getFirst().id);
        assertEquals("400", result.get(1).id);
        assertEquals("100", result.get(2).id);
        assertEquals("300", result.get(3).id);
    }

    @Test
    public void testSortIssuesWithMetaPriority_allWithMeta() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(ORDERING_TEST_ISSUE_DATA, listType);
        
        // Add meta to all issues
        issues.get(0).meta = "TX";
        issues.get(2).meta = "FL";
        
        IssuesAdapter adapter = new IssuesAdapter(null, null);
        ArrayList<Issue> result = adapter.sortIssuesWithMetaPriority(issues);
        
        assertEquals(4, result.size());
        // All should have meta, sorted by sort field: 200(100), 400(200), 100(300), 300(400)
        assertEquals("200", result.getFirst().id);
        assertEquals("CA", result.getFirst().meta);
        assertEquals("400", result.get(1).id);
        assertEquals("NY", result.get(1).meta);
        assertEquals("100", result.get(2).id);
        assertEquals("TX", result.get(2).meta);
        assertEquals("300", result.get(3).id);
        assertEquals("FL", result.get(3).meta);
    }

    @Test
    public void testSortIssuesWithMetaPriority_mixedMetaValues() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(ORDERING_TEST_ISSUE_DATA, listType);
        
        IssuesAdapter adapter = new IssuesAdapter(null, null);
        ArrayList<Issue> result = adapter.sortIssuesWithMetaPriority(issues);
        
        assertEquals(4, result.size());
        
        // First two should have meta values (sorted by sort field: 200(100), 400(200))
        assertEquals("200", result.getFirst().id);
        assertEquals("CA", result.get(0).meta);
        assertEquals(100, result.get(0).sort);
        assertEquals("400", result.get(1).id);
        assertEquals("NY", result.get(1).meta);
        assertEquals(200, result.get(1).sort);
        
        // Last two should not have meta values (sorted by sort field: 100(300), 300(400))
        assertEquals("100", result.get(2).id);
        assertEquals("", result.get(2).meta);
        assertEquals(300, result.get(2).sort);
        assertEquals("300", result.get(3).id);
        assertEquals("", result.get(3).meta);
        assertEquals(400, result.get(3).sort);
    }

    @Test
    public void testSortIssuesWithMetaPriority_nullMetaHandling() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(ORDERING_TEST_ISSUE_DATA, listType);
        
        // Set some meta to null
        issues.get(0).meta = null;
        issues.get(2).meta = null;
        
        IssuesAdapter adapter = new IssuesAdapter(null, null);
        ArrayList<Issue> result = adapter.sortIssuesWithMetaPriority(issues);
        
        assertEquals(4, result.size());
        
        // Issues with non-empty meta come first (sorted by sort: 200(100), 400(200))
        assertEquals("200", result.getFirst().id);
        assertEquals("CA", result.getFirst().meta);
        assertEquals("400", result.get(1).id);
        assertEquals("NY", result.get(1).meta);

        // Issues with null/empty meta come last (sorted by sort: 100(300), 300(400))
        assertEquals("100", result.get(2).id);
        assertEquals("300", result.get(3).id);
    }

    // Test data for populateIssueContacts tests
    private static final String CONTACTS_TEST_ISSUE_DATA = """
    [
        {
            "id": "1000",
            "name": "Test Issue with House and Senate",
            "reason": "Test reason",
            "script": "Test script",
            "categories": [{"name": "Test"}],
            "contactType": "REPS",
            "contactAreas": ["US House", "US Senate"],
            "outcomeModels": [],
            "stats": { "calls": 0 },
            "active": true,
            "meta": ""
        },
        {
            "id": "2000",
            "name": "Test Issue with only Senate",
            "reason": "Test reason",
            "script": "Test script",
            "categories": [{"name": "Test"}],
            "contactType": "REPS",
            "contactAreas": ["US Senate"],
            "outcomeModels": [],
            "stats": { "calls": 0 },
            "active": true,
            "meta": ""
        },
        {
            "id": "3000",
            "name": "Test Issue with no contact areas",
            "reason": "Test reason",
            "script": "Test script",
            "categories": [{"name": "Test"}],
            "contactType": "REPS",
            "contactAreas": [],
            "outcomeModels": [],
            "stats": { "calls": 0 },
            "active": true,
            "meta": ""
        }
    ]""";

    @Test
    public void testPopulateIssueContacts_basicAssignment() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(CONTACTS_TEST_ISSUE_DATA, listType);
        Issue issue = issues.getFirst();

        List<Contact> contacts = new ArrayList<>();
        Contact senatorA = Contact.createPlaceholder("sen1", "Senator A", "", "US Senate");
        senatorA.isPlaceholder = false;
        contacts.add(senatorA);

        Contact senatorB = Contact.createPlaceholder("sen2", "Senator B", "", "US Senate");
        senatorB.isPlaceholder = false;
        contacts.add(senatorB);

        Contact rep = Contact.createPlaceholder("rep1", "Representative", "", "US House");
        rep.isPlaceholder = false;
        contacts.add(rep);

        IssuesAdapter adapter = new IssuesAdapter(null, null);
        adapter.setContacts(contacts, false, IssuesAdapter.NO_ERROR);
        adapter.populateIssueContacts(issue);

        assertEquals(3, issue.contacts.size());
        assertEquals("Representative", issue.contacts.get(0).name);
        assertEquals("Senator A", issue.contacts.get(1).name);
        assertEquals("Senator B", issue.contacts.get(2).name);
    }

    @Test
    public void testPopulateIssueContacts_splitDistrict() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(CONTACTS_TEST_ISSUE_DATA, listType);
        Issue issue = issues.getFirst();

        List<Contact> contacts = new ArrayList<>();
        Contact rep = Contact.createPlaceholder("rep1", "Representative", "", "US House");
        rep.isPlaceholder = false;
        contacts.add(rep);

        IssuesAdapter adapter = new IssuesAdapter(null, null);
        adapter.setContacts(contacts, true, IssuesAdapter.NO_ERROR);
        adapter.populateIssueContacts(issue);

        assertTrue(issue.isSplit);
    }

    @Test
    public void testPopulateIssueContacts_emptyContactAreas() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(CONTACTS_TEST_ISSUE_DATA, listType);
        Issue issue = issues.get(2);

        List<Contact> contacts = new ArrayList<>();
        Contact senator = Contact.createPlaceholder("sen1", "Senator", "", "US Senate");
        senator.isPlaceholder = false;
        contacts.add(senator);

        IssuesAdapter adapter = new IssuesAdapter(null, null);
        adapter.setContacts(contacts, false, IssuesAdapter.NO_ERROR);
        adapter.populateIssueContacts(issue);

        assertNull(issue.contacts);
    }

    @Test
    public void testPopulateIssueContacts_noMatchingContacts() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(CONTACTS_TEST_ISSUE_DATA, listType);
        Issue issue = issues.getFirst();

        List<Contact> contacts = new ArrayList<>();
        Contact stateRep = Contact.createPlaceholder("state1", "State Representative", "", "State Lower");
        stateRep.isPlaceholder = false;
        contacts.add(stateRep);

        IssuesAdapter adapter = new IssuesAdapter(null, null);
        adapter.setContacts(contacts, false, IssuesAdapter.NO_ERROR);
        adapter.populateIssueContacts(issue);

        assertEquals(0, issue.contacts.size());
    }

    @Test
    public void testPopulateIssueContacts_noDuplicates() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
        List<Issue> issues = gson.fromJson(CONTACTS_TEST_ISSUE_DATA, listType);
        Issue issue = issues.get(1);

        List<Contact> contacts = new ArrayList<>();
        Contact senator = Contact.createPlaceholder("sen1", "Senator A", "", "US Senate");
        senator.isPlaceholder = false;
        contacts.add(senator);
        contacts.add(senator);

        IssuesAdapter adapter = new IssuesAdapter(null, null);
        adapter.setContacts(contacts, false, IssuesAdapter.NO_ERROR);
        adapter.populateIssueContacts(issue);

        assertEquals(1, issue.contacts.size());
        assertEquals("Senator A", issue.contacts.getFirst().name);
    }
}