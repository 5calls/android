package org.a5calls.android.a5calls.model;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

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
}