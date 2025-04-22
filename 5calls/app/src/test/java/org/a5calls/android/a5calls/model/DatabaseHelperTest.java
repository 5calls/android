package org.a5calls.android.a5calls.model;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import androidx.core.util.Pair;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.*;

/**
 * Unit tests for DatabaseHelper.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseHelperTest {
    private DatabaseHelper mDatabase;
    private Calendar mCalendar;

    @Before
    public void setUp() {
        getApplicationContext().deleteDatabase(DatabaseHelper.CALLS_TABLE_NAME);
        getApplicationContext().deleteDatabase(DatabaseHelper.ISSUES_TABLE_NAME);
        getApplicationContext().deleteDatabase(DatabaseHelper.CONTACTS_TABLE_NAME);
        getApplicationContext().deleteDatabase(DatabaseHelper.STARRED_ISSUES_TABLE_NAME);

        // Create a fake TimeProvider so we can control timestamps.
        mCalendar = new Calendar.Builder()
                .setDate(1973, Calendar.JANUARY, 22)
                .setTimeOfDay(20, 0, 0) // 8 pm
                .build();
        DatabaseHelper.TimeProvider mTimeProvider = new DatabaseHelper.TimeProvider() {
            @Override
            public long currentTimeMillis() {
                return mCalendar.getTimeInMillis();
            }

            @Override
            public Calendar getCalendar() {
                return mCalendar;
            }
        };
        mDatabase = new DatabaseHelper(getApplicationContext(), mTimeProvider);
    }

    @After
    public void tearDown() throws Exception {
        mDatabase.close();
    }

    @Test
    public void sanitizesStrings() throws Exception {
        assertEquals("TX-BetoO''Rourke", DatabaseHelper.sanitizeContactId("TX-BetoO'Rourke"));
        assertEquals("cats", DatabaseHelper.sanitizeContactId("cats"));
        assertEquals("cat''s", DatabaseHelper.sanitizeContactId("cat's"));
        assertEquals("cat''s cats cats''", DatabaseHelper.sanitizeContactId("cat's cats cats'"));

        // Don't sanitize double quotes
        assertEquals("cat''s", DatabaseHelper.sanitizeContactId("cat''s"));
    }

    @Test
    public void callsInitiallyEmpty() {
        assertEquals(0, mDatabase.getCallsCount());
    }

    @Test
    public void addCall_readCall() {
        mDatabase.addCall("myIssue", "Issue name", "myContact", "Contact name",
                "myFirstResult", "myLocation");
        assertEquals(1, mDatabase.getCallsCount());
        List<String> callResults = mDatabase.getCallResults("myIssue", "myContact");
        assertEquals(1, callResults.size());
        assertEquals("myFirstResult", callResults.get(0));

        mDatabase.addCall("myIssue", "Issue name", "myContact", "Contact name",
                "mySecondResult", "myLocation");
        assertEquals(2, mDatabase.getCallsCount());
        callResults = mDatabase.getCallResults("myIssue", "myContact");
        assertEquals(2, callResults.size());
        assertEquals("mySecondResult", callResults.get(1));
    }

    @Test
    public void addCall_AddsContactOnce() {
        // Contact not in DB yet.
        assertEquals("", mDatabase.getContactName("myContact"));

        // Gets added when call is logged.
        mDatabase.addCall("myIssue", "Issue name", "myContact", "Contact name",
                "myFirstResult", "myLocation");
        assertEquals("Contact name", mDatabase.getContactName("myContact"));

        // Logging another call with the same contact ID doesn't cause any problems.
        mDatabase.addCall("anotherIssue", "Issue name 2", "myContact", "Contact name",
                "mySecondResult", "myLocation");
        assertEquals("Contact name", mDatabase.getContactName("myContact"));

        // If the contact name changes for some reason, there's no conflict.
        // Not sure if this is the "correct" behavior, but we can test the app doesn't
        // have any issues in this case.
        mDatabase.addCall("anotherIssue", "Issue name 2", "myContact", "New contact name",
                "myThirdResult", "myLocation");
        assertEquals("Contact name", mDatabase.getContactName("myContact"));
    }

    @Test
    public void addCall_AddsIssueOnce() {
        // Issue not in DB yet.
        assertEquals("", mDatabase.getIssueName("myIssue"));

        // Gets added when call is logged.
        mDatabase.addCall("myIssue", "Issue name", "myContact", "Contact name",
                "myFirstResult", "myLocation");
        assertEquals("Issue name", mDatabase.getIssueName("myIssue"));

        // Logging another call with the same issue ID doesn't cause any problems.
        mDatabase.addCall("myIssue", "Issue name", "myContact2", "Contact name 2",
                "mySecondResult", "myLocation");
        assertEquals("Issue name", mDatabase.getIssueName("myIssue"));

        // Does not change the issue name in the DB if it was already there.
        // Issue names may change over time, for example issues writers may put
        // major updates in an issue name. We only keep the first title of the
        // issue in the DB.
        mDatabase.addCall("myIssue", "New issue name", "myContact2", "Contact name 2",
                "myThirdResult", "myLocation");
        assertEquals("Issue name", mDatabase.getIssueName("myIssue"));
    }

    @Test
    public void testGetTotalCallsForIssueAndContacts() {
        IssuesAndContacts issuesAndContacts = initializeDbWithIssuesAndContacts();
        List<Issue> issues = issuesAndContacts.issues;
        List<Contact> contacts = issuesAndContacts.contacts;

        int totalCallsForIssue1 = mDatabase.getTotalCallsForIssueAndContacts(
                issues.get(0).id, contacts);
        assertEquals(3, totalCallsForIssue1);

        int totalCallsForIssue2 = mDatabase.getTotalCallsForIssueAndContacts(
                issues.get(1).id, contacts);
        assertEquals(2, totalCallsForIssue2);

        int totalCallsForIssue3 = mDatabase.getTotalCallsForIssueAndContacts(
                issues.get(2).id, contacts);
        assertEquals(6, totalCallsForIssue3);

        int totalCallsForIssue4 = mDatabase.getTotalCallsForIssueAndContacts(
                issues.get(3).id, contacts);
        assertEquals(0, totalCallsForIssue4);
    }

    @Test
    public void testHasCalled() {
        IssuesAndContacts issuesAndContacts = initializeDbWithIssuesAndContacts();
        List<Issue> issues = issuesAndContacts.issues;
        List<Contact> contacts = issuesAndContacts.contacts;

        // The first issue has calls from each of the first 3 contacts, and no call from
        // the fourth contact.
        for (int i = 0; i < 3; i++) {
            assertTrue(mDatabase.hasCalled(issues.get(0).id, contacts.get(i).id));
        }
        assertFalse(mDatabase.hasCalled(issues.get(0).id, contacts.get(3).id));

        // The second issue has calls only from the second contact.
        for (int i = 0; i < 4; i++) {
            if (i == 1) {
                assertTrue(mDatabase.hasCalled(issues.get(1).id, contacts.get(i).id));
            } else {
                assertFalse(mDatabase.hasCalled(issues.get(1).id, contacts.get(i).id));
            }
        }

        // The third issue has calls from the first and third contacts.
        assertTrue(mDatabase.hasCalled(issues.get(2).id, contacts.get(0).id));
        assertTrue(mDatabase.hasCalled(issues.get(2).id, contacts.get(2).id));
        assertFalse(mDatabase.hasCalled(issues.get(2).id, contacts.get(1).id));
        assertFalse(mDatabase.hasCalled(issues.get(2).id, contacts.get(3).id));

        // The forth issue has no calls.
        for (Contact contact : contacts) {
            assertFalse(mDatabase.hasCalled(issues.get(3).id, contact.id));
        }
    }

    @Test
    public void testGetCallResults() {
        IssuesAndContacts issuesAndContacts = initializeDbWithIssuesAndContacts();
        List<Issue> issues = issuesAndContacts.issues;
        List<Contact> contacts = issuesAndContacts.contacts;

        // The first three contacts have "unavailable" as their call result
        // on the first issue.
        for (int i = 0; i < 3; i++) {
            List<String> callResults = mDatabase.getCallResults(issues.get(0).id,
                    contacts.get(i).id);
            assertEquals(1, callResults.size());
            assertEquals("unavailable", callResults.getFirst());
        }

        // The second issue has calls only from the second contact.
        for (int i = 0; i < 4; i++) {
            List<String> callResults = mDatabase.getCallResults(
                    issues.get(1).id, contacts.get(i).id);
            if (i == 1) {
                assertEquals(2, callResults.size());
                assertEquals("voicemail", callResults.getFirst());
            } else {
                assertEquals(0, callResults.size());
            }
        }

        // The third issue has several unique call results from the first
        // and third contacts.
        for (int i = 0; i < 4; i++) {
            List<String> callResults = mDatabase.getCallResults(
                    issues.get(2).id, contacts.get(i).id);
            if (i == 0 || i == 2) {
                assertEquals(3, callResults.size());
                for (int j = 0; j < 3; j++) {
                    assertTrue(callResults.contains(String.valueOf(Outcome.Status.values()[j])));
                }
            } else {
                assertEquals(0, callResults.size());
            }
        }

        // The fourth issue has no calls from any contacts.
        for (Contact contact : contacts) {
            List<String> callResults = mDatabase.getCallResults(
                    issues.get(3).id, contact.id);
            assertEquals(0, callResults.size());
        }
    }

    @Test
    public void testGetCallCountsByContact() {
        IssuesAndContacts issuesAndContacts = initializeDbWithIssuesAndContacts();
        List<Contact> contacts = issuesAndContacts.contacts;

        List<Pair<String, Integer>> callCountsByContact =
                mDatabase.getCallCountsByContact();

        // Three contacts had calls.
        assertEquals(3, callCountsByContact.size());

        // This is the expected number of calls for each of those three contacts.
        Pair<String, Integer> expectedContact1 = new Pair<>(contacts.get(0).id, 4);
        Pair<String, Integer> expectedContact2 = new Pair<>(contacts.get(1).id, 3);
        Pair<String, Integer> expectedContact3 = new Pair<>(contacts.get(2).id, 4);

        assertTrue(callCountsByContact.contains(expectedContact1));
        assertTrue(callCountsByContact.contains(expectedContact2));
        assertTrue(callCountsByContact.contains(expectedContact3));
    }

    @Test
    public void testGetCalLCountsByIssue() {
        IssuesAndContacts issuesAndContacts = initializeDbWithIssuesAndContacts();
        List<Issue> issues = issuesAndContacts.issues;

        List<Pair<String, Integer>> callCountsByIssue =
                mDatabase.getCallCountsByIssue();

        // Three issues had calls.
        assertEquals(3, callCountsByIssue.size());

        // This is the expected number of calls for each of those three issues.
        Pair<String, Integer> expectedIssue1 = new Pair<>(issues.get(0).id, 3);
        Pair<String, Integer> expectedIssue2 = new Pair<>(issues.get(1).id, 2);
        Pair<String, Integer> expectedIssue3 = new Pair<>(issues.get(2).id, 6);

        assertTrue(callCountsByIssue.contains(expectedIssue1));
        assertTrue(callCountsByIssue.contains(expectedIssue2));
        assertTrue(callCountsByIssue.contains(expectedIssue3));
    }

    @Test
    public void testGetCallTimestampsForType() {
        initializeDbWithIssuesAndContacts();
        List<Long> unavailable = mDatabase.getCallTimestampsForType(Outcome.Status.UNAVAILABLE);
        List<Long> contact = mDatabase.getCallTimestampsForType(Outcome.Status.CONTACT);
        List<Long> voicemail = mDatabase.getCallTimestampsForType(Outcome.Status.VOICEMAIL);

        assertEquals(5, unavailable.size());
        assertEquals(2, contact.size());
        assertEquals(4, voicemail.size());
    }

    @Test
    public void testHasCalledToday() {
        IssuesAndContacts issuesAndContacts = initializeDbWithIssuesAndContacts();
        List<Issue> issues = issuesAndContacts.issues;
        List<Contact> contacts = issuesAndContacts.contacts;

        // The first issue was not called today for any contacts, as the current calendar
        // started at 8 pm and progressed by 11 hours.
        for (Contact contact : contacts) {
            assertFalse(mDatabase.hasCalledToday(issues.get(0).id, contact.id));
        }

        // The second issue was called once today by contact 2.
        for (int i = 0; i < 4; i++) {
            if (i == 1) {
                assertTrue(mDatabase.hasCalledToday(issues.get(1).id, contacts.get(i).id));
            } else {
                assertFalse(mDatabase.hasCalledToday(issues.get(1).id, contacts.get(i).id));
            }
        }

        // The third issue was called today by contacts 1 and 3.
        for (int i = 0; i < 4; i++) {
            if (i == 0 || i == 2) {
                assertTrue(mDatabase.hasCalledToday(issues.get(2).id, contacts.get(i).id));
            } else {
                assertFalse(mDatabase.hasCalledToday(issues.get(2).id, contacts.get(i).id));
            }
        }

        // The fourth issue was not called today.
        for (Contact contact : contacts) {
            assertFalse(mDatabase.hasCalledToday(issues.get(3).id, contact.id));
        }

        // Move time forward to "tomorrow".
        long twentyFourHoursInMs = 24 * 60 * 60 * 1000;
        mCalendar.setTimeInMillis(mCalendar.getTimeInMillis() + twentyFourHoursInMs);

        // No calls now.
        for (Contact contact : contacts) {
            for (Issue issue : issues) {
                assertFalse(mDatabase.hasCalledToday(issue.id, contact.id));
            }
        }
    }

    @Test
    public void starredIssues_AddIssue() {
        mDatabase.addStarredIssue("test-issue");
        assertEquals(1, mDatabase.getStarredIssues().size());
        assertEquals("test-issue", mDatabase.getStarredIssues().getFirst());
    }

    @Test
    public void starredIssues_DeletesIssue() {
        String issue = "to-be-deleted";
        mDatabase.addStarredIssue(issue);
        assertTrue(mDatabase.getStarredIssues().contains(issue));
        mDatabase.removeStarredIssue(issue);
        assertFalse(mDatabase.getStarredIssues().contains(issue));
    }

    @Test
    public void starredIssues_TrimsIssues() {
        mDatabase.addStarredIssue("test-issue2");
        mDatabase.addStarredIssue("test-issue3");
        mDatabase.addStarredIssue("keep");
        mDatabase.trimStarredIssues(List.of("keep"));
        assertEquals(1, mDatabase.getStarredIssues().size());
        assertEquals("keep", mDatabase.getStarredIssues().getFirst());
    }

    @Test
    public void starredIssue_TrimIssues() {

    }

    // Wrapper class for holding a pair of issues and contacts.
    private static class IssuesAndContacts {
        List<Contact> contacts;
        List<Issue> issues;
    }

    private IssuesAndContacts initializeDbWithIssuesAndContacts() {
        IssuesAndContacts result = new IssuesAndContacts();
        result.contacts = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Contact contact = TestModelUtils.createContact("contact_id" + i, "Contact Name " + i);
            result.contacts.add(contact);
        }
        result.issues = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Issue issue = TestModelUtils.createIssue("issue_id" + i, "Issue Name " + i);
            result.issues.add(issue);
        }
        long oneHourInMs = 60 * 60 * 1000;
        // For the first issue, log a single call for the first 3 contacts,
        // one hour apart.
        for (int i = 0; i < 3; i++) {
            mDatabase.addCall(result.issues.get(0).id, result.issues.get(0).name,
                    result.contacts.get(i).id, result.contacts.get(i).name,
                    "unavailable", "location");
            mCalendar.setTimeInMillis(mCalendar.getTimeInMillis() + oneHourInMs);
        }
        // For the second issue, log two identical calls for only the second contact,
        // one hour apart.
        for (int i = 0; i < 2; i++) {
            mDatabase.addCall(result.issues.get(1).id, result.issues.get(1).name,
                    result.contacts.get(1).id, result.contacts.get(1).name,
                    "voicemail", "location");
            mCalendar.setTimeInMillis(mCalendar.getTimeInMillis() + oneHourInMs);
        }
        // For the third issue, log three calls from the first and third contact,
        // each with a different result.
        for (int i = 0; i < 3; i++) {
            mDatabase.addCall(result.issues.get(2).id, result.issues.get(2).name,
                    result.contacts.get(0).id, result.contacts.get(0).name,
                    String.valueOf(Outcome.Status.values()[i]), "location");
            mCalendar.setTimeInMillis(mCalendar.getTimeInMillis() + oneHourInMs);
            mDatabase.addCall(result.issues.get(2).id, result.issues.get(2).name,
                    result.contacts.get(2).id, result.contacts.get(2).name,
                    String.valueOf(Outcome.Status.values()[i]), "location");
            mCalendar.setTimeInMillis(mCalendar.getTimeInMillis() + oneHourInMs);
        }
        // The fourth issue and contact are unused.

        assertEquals(11, mDatabase.getCallsCount());
        return result;
    }
}