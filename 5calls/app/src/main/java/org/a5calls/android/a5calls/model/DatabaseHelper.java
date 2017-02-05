package org.a5calls.android.a5calls.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;

import org.a5calls.android.a5calls.AppSingleton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Local database helper. I believe this is already "thread-safe" and such because SQLiteOpenHelper
 * handles all of that for us. As long as we just use one SQLiteOpenHelper from AppSingleton
 * we should be safe!
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";

    private static final int DATABASE_VERSION = 2;
    private static final String CALLS_TABLE_NAME = "UserCallsDatabase";
    private static final String ISSUES_TABLE_NAME = "UserIssuesTable";
    private static final String CONTACTS_TABLE_NAME = "UserContactsTable";


    private static class CallsColumns {
        public static String TIMESTAMP = "timestamp";
        public static String CONTACT_ID = "contactid";
        public static String ISSUE_ID = "issueid";
        public static String LOCATION = "location";
        public static String RESULT = "result";
    }

    private static final String CALLS_TABLE_CREATE =
            "CREATE TABLE " + CALLS_TABLE_NAME + " (" +
                CallsColumns.TIMESTAMP + " INTEGER, " + CallsColumns.CONTACT_ID + " STRING, " +
                    CallsColumns.ISSUE_ID + " STRING, " + CallsColumns.LOCATION + " STRING, " +
                    CallsColumns.RESULT + " STRING);";

    private static class IssuesColumns {
        public static String ISSUE_ID = "issueid";
        public static String ISSUE_NAME = "issuename";
    }

    private static final String ISSUES_TABLE_CREATE =
            "CREATE TABLE " + ISSUES_TABLE_NAME + " (" + IssuesColumns.ISSUE_ID + " STRING, " +
                    IssuesColumns.ISSUE_NAME + " STRING);";

    public static class ContactColumns {
        public static String CONTACT_ID = "contactid";
        public static String CONTACT_NAME = "contactname";
    }

    private static final String CONTACTS_TABLE_CREATE =
            "CREATE TABLE " + CONTACTS_TABLE_NAME + " (" + ContactColumns.CONTACT_ID + " STRING, " +
                    ContactColumns.CONTACT_NAME + " STRING);";

    public DatabaseHelper(Context context) {
        super(context, CALLS_TABLE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CALLS_TABLE_CREATE);
        db.execSQL(ISSUES_TABLE_CREATE);
        db.execSQL(CONTACTS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion >= 2) {
            db.execSQL(ISSUES_TABLE_CREATE);
            db.execSQL(CONTACTS_TABLE_CREATE);
        }
    }

    /**
     * Adds a successful call to the user's local database
     */
    public void addCall(String issueId, String issueName, String contactId, String contactName,
                        String result, String location) {
        addCall(issueId, contactId, result, location);
        addIssue(issueId, issueName);
        addContact(contactId, contactName);
    }

    private void addCall(String issueId, String contactId, String result, String location) {
        ContentValues values = new ContentValues();
        values.put(CallsColumns.TIMESTAMP, System.currentTimeMillis());
        values.put(CallsColumns.CONTACT_ID, contactId);
        values.put(CallsColumns.ISSUE_ID, issueId);
        values.put(CallsColumns.LOCATION, location);
        values.put(CallsColumns.RESULT, result);
        getWritableDatabase().insert(CALLS_TABLE_NAME, null, values);
    }

    public void addIssue(String issueId, String issueName) {
        ContentValues values = new ContentValues();
        values.put(IssuesColumns.ISSUE_ID, issueId);
        values.put(IssuesColumns.ISSUE_NAME, issueName);
        // Insert it into the database if it isn't there yet.
        getWritableDatabase().insertWithOnConflict(ISSUES_TABLE_NAME, null, values,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void addContact(String contactId, String contactName) {
        ContentValues values = new ContentValues();
        values.put(ContactColumns.CONTACT_ID, contactId);
        values.put(ContactColumns.CONTACT_NAME, contactName);
        // Insert it into the database if it isn't there yet.
        getWritableDatabase().insertWithOnConflict(CONTACTS_TABLE_NAME, null, values,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    public String getIssueName(String issueId) {
        Cursor c = getReadableDatabase().rawQuery("SELECT " + IssuesColumns.ISSUE_NAME + " FROM " +
                ISSUES_TABLE_NAME + " WHERE " + IssuesColumns.ISSUE_ID + " = \"" + issueId + "\"",
                null);
        String result = "";
        if (c.moveToNext()) {
            result = c.getString(0);
        }
        c.close();
        return result;
    }

    public String getContactName(String contactId) {
        Cursor c = getReadableDatabase().rawQuery("SELECT " + ContactColumns.CONTACT_NAME +
                " FROM " + CONTACTS_TABLE_NAME + " WHERE " + ContactColumns.CONTACT_ID + " = \"" +
                contactId + "\"", null);
        String result = "";
        if (c.moveToNext()) {
            result = c.getString(0);
        }
        c.close();
        return result;
    }

    /**
     * Gets the calls in the database for a particular issue.
     * @param issueId
     * @return A list of the contact IDs contacted for this issue.
     */
    public List<String> getCallsForIssue(String issueId) {
        Cursor c = getReadableDatabase().rawQuery("SELECT " + CallsColumns.CONTACT_ID + " FROM " +
                CALLS_TABLE_NAME + " WHERE " + CallsColumns.ISSUE_ID + " = ? GROUP BY " +
                CallsColumns.CONTACT_ID, new String[] {issueId});
        List<String> result = new ArrayList<>();
        while (c.moveToNext()) {
            result.add(c.getString(0));
        }
        c.close();
        return result;
    }

    /**
     * Gets the calls in the database for a particular issue and list of contacts.
     * @param issueId
     * @param contacts
     * @return A list of the contact IDs contacted for this issue.
     */
    public List<String> getCallsForIssueAndContacts(String issueId, Contact[] contacts) {
        String[] contactIdList = new String[contacts.length];
        for (int i = 0; i < contacts.length; i++) {
            contactIdList[i] = "'" + contacts[i].id + "'";
        }
        String contactIds = "(" + TextUtils.join(",", contactIdList) + ")";
        String query = "SELECT " + CallsColumns.CONTACT_ID + " FROM " +
                CALLS_TABLE_NAME + " WHERE " + CallsColumns.ISSUE_ID + " = ? AND " +
                CallsColumns.CONTACT_ID + " IN " + contactIds + " GROUP BY " +
                CallsColumns.CONTACT_ID;
        Cursor c = getReadableDatabase().rawQuery(query, new String[] {issueId});
        List<String> result = new ArrayList<>();
        while (c.moveToNext()) {
            result.add(c.getString(0));
        }
        c.close();
        return result;
    }

    /**
     * Gets the calls in the database for a particular contact.
     * @param contactId
     * @return a list of the issues IDs that were called for a particular contact.
     */
    public List<String> getCallsForContact(String contactId) {
        // TODO do we want to return issue IDs, or more detailed info about the call? What about
        // whether contact was made?
        // Probably need to make a "Call" class to store this in.
        return null;
    }

    /**
     * Whether a contact has been called for a particular issue and contact.
     * @param issueId
     * @param contactId
     */
    public boolean hasCalled(String issueId, String contactId) {
        Cursor c = getReadableDatabase().rawQuery("SELECT " + CallsColumns.TIMESTAMP + " FROM " +
                CALLS_TABLE_NAME + " WHERE " + CallsColumns.ISSUE_ID + " = ? AND " +
                CallsColumns.CONTACT_ID + " = ?", new String[] {issueId, contactId});
        boolean result = c.getCount() > 0;
        c.close();
        return result;
    }

    /**
     * The types of calls made for a particular issue and contact.
     * @param issueId
     * @param contactId
     * @return A list of the call results for this issue and contact.
     */
    public List<String> getCallResults(String issueId, String contactId) {
        Cursor c = getReadableDatabase().rawQuery("SELECT " + CallsColumns.RESULT + " FROM " +
                CALLS_TABLE_NAME + " WHERE " + CallsColumns.ISSUE_ID + " = ? AND " +
                CallsColumns.CONTACT_ID + " = ? GROUP BY " + CallsColumns.RESULT,
                new String[] {issueId, contactId});
        List<String> result = new ArrayList<>();
        while (c.moveToNext()) {
            result.add(c.getString(0));
        }
        c.close();
        return result;
    }

    /**
     * Gets the total number of calls this user has made
     */
    public int getCallsCount() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT " + CallsColumns.TIMESTAMP + " FROM " + CALLS_TABLE_NAME, null);
        int count = c.getCount();
        c.close();
        return count;
    }

    /**
     * Gets the total number of calls of a particular type (voicemail, unavailable, contacted) that
     * this user has made
     */
    public int getCallsCountForType(String type) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT " + CallsColumns.TIMESTAMP + " FROM " + CALLS_TABLE_NAME + " WHERE " +
                CallsColumns.RESULT + " = \"" + type + "\"", null);
        int count = c.getCount();
        c.close();
        return count;
    }

    /**
     * Gets a list of pairs of the contacts called and call count for that contact.
     */
    public List<Pair<String, Integer>> getCallCountsByContact() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT " + CallsColumns.CONTACT_ID + ", COUNT(*) as COUNT FROM " +
                        CALLS_TABLE_NAME + " GROUP BY " + CallsColumns.CONTACT_ID +
                        " ORDER BY count desc", null);
        List<Pair<String, Integer>> result = new ArrayList<>();
        while (c.moveToNext()) {
            Pair<String, Integer> next = new Pair(c.getString(0), c.getInt(1));
            result.add(next);
        }
        c.close();
        return result;
    }

    /**
     * Gets a list of pairs of the issues called and call count for that issue.
     */
    public List<Pair<String, Integer>> getCallCountsByIssue() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT " + CallsColumns.ISSUE_ID + ", COUNT(*) as COUNT FROM " +
                        CALLS_TABLE_NAME + " GROUP BY " + CallsColumns.ISSUE_ID +
                        " ORDER BY count desc", null);
        List<Pair<String, Integer>> result = new ArrayList<>();
        while (c.moveToNext()) {
            Pair<String, Integer> next = new Pair(c.getString(0), c.getInt(1));
            result.add(next);
        }
        c.close();
        return result;
    }

    public void saveIssuesToDatabaseForUpgrade(List<Issue> issues) {
        Set<String> addedContacts = new HashSet<>();
        for (Issue issue : issues) {
            addIssue(issue.id, issue.name);
            for (int i = 0; i < issue.contacts.length; i++) {
                // Do a little less DB work by keeping added contacts in a set. Most contacts in
                // the issues list are repeats anyway.
                if (!addedContacts.contains(issue.contacts[i].id)) {
                    addContact(issue.contacts[i].id, issue.contacts[i].name);
                    addedContacts.add(issue.contacts[i].id);
                }
            }
        }
    }
}
