package org.a5calls.android.a5calls.model;

import android.os.Bundle;
import android.os.Parcel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
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

    @Before
    public void setUp() {
        getApplicationContext().deleteDatabase(DatabaseHelper.CALLS_TABLE_NAME);
        getApplicationContext().deleteDatabase(DatabaseHelper.ISSUES_TABLE_NAME);
        getApplicationContext().deleteDatabase(DatabaseHelper.CONTACTS_TABLE_NAME);
        mDatabase = new DatabaseHelper(getApplicationContext());
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
        IssuesAndContacts issuesAndContacts = initializeDbWithThreeIssuesAndContacts();
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
        IssuesAndContacts issuesAndContacts = initializeDbWithThreeIssuesAndContacts();
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
        IssuesAndContacts issuesAndContacts = initializeDbWithThreeIssuesAndContacts();
        List<Issue> issues = issuesAndContacts.issues;
        List<Contact> contacts = issuesAndContacts.contacts;

        // The first three contacts have "first_result" as their call result
        // on the first issue.
        for (int i = 0; i < 3; i++) {
            List<String> callResults = mDatabase.getCallResults(issues.get(0).id,
                    contacts.get(i).id);
            assertEquals(1, callResults.size());
            assertEquals("first_result", callResults.getFirst());
        }

        // The second issue has calls only from the second contact.
        // Both calls have the same result, so only one item is returned.
        for (int i = 0; i < 4; i++) {
            List<String> callResults = mDatabase.getCallResults(
                    issues.get(1).id, contacts.get(i).id);
            if (i == 1) {
                assertEquals(1, callResults.size());
                assertEquals("second_result", callResults.getFirst());
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
                    assertTrue(callResults.contains("third_result" + j));
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
        IssuesAndContacts issuesAndContacts = initializeDbWithThreeIssuesAndContacts();
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
        IssuesAndContacts issuesAndContacts = initializeDbWithThreeIssuesAndContacts();
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

    // TODO: Add tests for DB calls in which timestamps matter.

    // Wrapper class for holding a pair of issues and contacts.
    private static class IssuesAndContacts {
        List<Contact> contacts;
        List<Issue> issues;
    }

    private IssuesAndContacts initializeDbWithThreeIssuesAndContacts() {
        IssuesAndContacts result = new IssuesAndContacts();
        result.contacts = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Contact contact = createContact("contact_id" + i, "Contact Name " + i);
            result.contacts.add(contact);
        }
        result.issues = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Issue issue = createIssue("issue_id" + i, "Issue Name " + i);
            result.issues.add(issue);
        }
        // For the first issue, log a single call for the first 3 contacts.
        for (int i = 0; i < 3; i++) {
            mDatabase.addCall(result.issues.get(0).id, result.issues.get(0).name,
                    result.contacts.get(i).id, result.contacts.get(i).name,
                    "first_result", "location");
        }
        // For the second issue, log two identical calls for only the second contact.
        for (int i = 0; i < 2; i++) {
            mDatabase.addCall(result.issues.get(1).id, result.issues.get(1).name,
                    result.contacts.get(1).id, result.contacts.get(1).name,
                    "second_result", "location");
        }
        // For the third issue, log three calls from the first and third contact,
        // each with a different result.
        for (int i = 0; i < 3; i++) {
            mDatabase.addCall(result.issues.get(2).id, result.issues.get(2).name,
                    result.contacts.get(0).id, result.contacts.get(0).name,
                    "third_result" + i, "location");
            mDatabase.addCall(result.issues.get(2).id, result.issues.get(2).name,
                    result.contacts.get(2).id, result.contacts.get(2).name,
                    "third_result" + i, "location");
        }
        // The fourth issue and contact are unused.

        assertEquals(11, mDatabase.getCallsCount());
        return result;
    }

    private Contact createContact(String id, String name) {
        Contact contact = Contact.CREATOR.createFromParcel(Parcel.obtain());
        contact.id = id;
        contact.name = name;
        return contact;
    }

    private Issue createIssue(String id, String name) {
        Issue issue = Issue.CREATOR.createFromParcel(Parcel.obtain());
        issue.id = id;
        issue.name = name;
        return issue;
    }
}