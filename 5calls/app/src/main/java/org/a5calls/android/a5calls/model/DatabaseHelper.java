package org.a5calls.android.a5calls.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Local database helper. I believe this is already "thread-safe" and such because SQLiteOpenHelper
 * handles all of that for us. As long as we just use one SQLiteOpenHelper from AppSingleton
 * we should be safe!
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";

    private static final int DATABASE_VERSION = 1;
    private static final String CALLS_TABLE_NAME = "UserCallsDatabase";

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


    DatabaseHelper(Context context) {
        super(context, CALLS_TABLE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CALLS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /**
     * Adds a successful call to the user's local database
     * @param issueId
     * @param contactId
     * @param location
     * @param result
     */
    public void addCall(String issueId, String contactId, String result, String location) {
        ContentValues values = new ContentValues();
        values.put(CallsColumns.TIMESTAMP, System.currentTimeMillis());
        values.put(CallsColumns.CONTACT_ID, contactId);
        values.put(CallsColumns.ISSUE_ID, issueId);
        values.put(CallsColumns.LOCATION, location);
        values.put(CallsColumns.RESULT, result);
        getWritableDatabase().insert(CALLS_TABLE_NAME, null, values);
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
     * Gets the calls in the database for a particular issue.
     * @param issueId
     * @param zip
     * @return A list of the contact IDs contacted for this issue.
     */
    public List<String> getCallsForIssueAndZip(String issueId, String zip) {
        String query = "SELECT " + CallsColumns.CONTACT_ID + " FROM " +
                CALLS_TABLE_NAME + " WHERE " + CallsColumns.ISSUE_ID + " = ? AND " +
                CallsColumns.LOCATION + " = ? GROUP BY " + CallsColumns.CONTACT_ID;
        Cursor c = getReadableDatabase().rawQuery(query, new String[] {issueId, zip});
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
     * Whether a contact has been called for a particular issue.
     * @param issueId
     * @param contactId
     */
    public boolean hasCalled(String issueId, String contactId) {
        Cursor c = getReadableDatabase().rawQuery("SELECT " + CallsColumns.TIMESTAMP + " FROM " +
                CALLS_TABLE_NAME + " WHERE " + CallsColumns.ISSUE_ID + " = ? AND " +
                CallsColumns.CONTACT_ID + " = ?", new String[] {issueId, contactId});
        return c.getCount() > 0;
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
}
