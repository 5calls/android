package org.a5calls.android.a5calls.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Pair;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Local database helper. I believe this is already "thread-safe" and such because SQLiteOpenHelper
 * handles all of that for us. As long as we just use one SQLiteOpenHelper from AppSingleton
 * we should be safe!
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";

    private static final int DATABASE_VERSION = 3;
    @VisibleForTesting
    protected static final String CALLS_TABLE_NAME = "UserCallsDatabase";
    @VisibleForTesting
    protected static final String ISSUES_TABLE_NAME = "UserIssuesTable";
    @VisibleForTesting
    protected static final String CONTACTS_TABLE_NAME = "UserContactsTable";

    // Can be used to control time in tests.
    private TimeProvider mTimeProvider;
    public interface TimeProvider {
        public long currentTimeMillis();
        public Calendar getCalendar();
    }

    private static class DefaultTimeProvider implements TimeProvider {

        @Override
        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        @Override
        public Calendar getCalendar() {
            return Calendar.getInstance();
        }
    }

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
        this(context, new DefaultTimeProvider());
    }

    public DatabaseHelper(Context context, TimeProvider timeProvider) {
        super(context, CALLS_TABLE_NAME, null, DATABASE_VERSION);
        mTimeProvider = timeProvider;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CALLS_TABLE_CREATE);
        db.execSQL(ISSUES_TABLE_CREATE);
        db.execSQL(CONTACTS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int currentDbVersion = oldVersion;

        if (oldVersion < 2 && currentDbVersion < newVersion) {
            db.execSQL(ISSUES_TABLE_CREATE);
            db.execSQL(CONTACTS_TABLE_CREATE);
            currentDbVersion = 2;
        }

        if (oldVersion < 3 && currentDbVersion < newVersion) {
            ContentValues contactContentValues = new ContentValues();
            contactContentValues.put(CallsColumns.RESULT, Outcome.Status.CONTACT.toString());
            db.update(CALLS_TABLE_NAME,
                    contactContentValues,
                    CallsColumns.RESULT + " = ?",
                    new String[]{Outcome.Status.CONTACTED.toString()});

            ContentValues voicemailContentValues = new ContentValues();
            voicemailContentValues.put(CallsColumns.RESULT, Outcome.Status.VOICEMAIL.toString());
            db.update(CALLS_TABLE_NAME,
                    voicemailContentValues,
                    CallsColumns.RESULT + " = ?",
                    new String[]{Outcome.Status.VM.toString()});
            currentDbVersion = 3;
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
        values.put(CallsColumns.TIMESTAMP, mTimeProvider.currentTimeMillis());
        values.put(CallsColumns.CONTACT_ID, contactId);
        values.put(CallsColumns.ISSUE_ID, issueId);
        values.put(CallsColumns.LOCATION, location);
        values.put(CallsColumns.RESULT, result);
        getWritableDatabase().insert(CALLS_TABLE_NAME, null, values);
    }

    private void addIssue(String issueId, String issueName) {
        ContentValues values = new ContentValues();
        values.put(IssuesColumns.ISSUE_ID, issueId);
        values.put(IssuesColumns.ISSUE_NAME, issueName);
        // Insert it into the database if it isn't there yet.
        getWritableDatabase().insertWithOnConflict(ISSUES_TABLE_NAME, null, values,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void addContact(String contactId, String contactName) {
        ContentValues values = new ContentValues();
        values.put(ContactColumns.CONTACT_ID, contactId);
        values.put(ContactColumns.CONTACT_NAME, contactName);
        // Insert it into the database if it isn't there yet.
        getWritableDatabase().insertWithOnConflict(CONTACTS_TABLE_NAME, null, values,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    public String getIssueName(String issueId) {
        Cursor c = getReadableDatabase().rawQuery("SELECT " + IssuesColumns.ISSUE_NAME + " FROM " +
                ISSUES_TABLE_NAME + " WHERE " + IssuesColumns.ISSUE_ID + " = '" + issueId + "'",
                null);
        String result = "";
        if (c.moveToNext()) {
            result = c.getString(0);
        }
        c.close();
        return result;
    }

    public String getContactName(String contactId) {
        contactId = sanitizeContactId(contactId);
        Cursor c = getReadableDatabase().rawQuery("SELECT " + ContactColumns.CONTACT_NAME +
                " FROM " + CONTACTS_TABLE_NAME + " WHERE " + ContactColumns.CONTACT_ID + " = '" +
                contactId + "'", null);
        String result = "";
        if (c.moveToNext()) {
            result = c.getString(0);
        }
        c.close();
        return result;
    }

    /**
     * Gets the total number of calls made for a given issue and list of contacts.
     * @param issueId
     * @param contacts
     * @return total calls
     */
    public int getTotalCallsForIssueAndContacts(String issueId, List<Contact> contacts) {
        String[] contactIdList = new String[contacts.size()];
        for (int i = 0; i < contacts.size(); i++) {
            contactIdList[i] = "'" + sanitizeContactId(contacts.get(i).id) + "'";
        }
        String contactIds = "(" + TextUtils.join(",", contactIdList) + ")";
        String query = "SELECT " + CallsColumns.CONTACT_ID + " FROM " +
                CALLS_TABLE_NAME + " WHERE " + CallsColumns.ISSUE_ID + " = ? AND " +
                CallsColumns.CONTACT_ID + " IN " + contactIds;
        Cursor c = getReadableDatabase().rawQuery(query, new String[] {issueId});
        int result = 0;
        while (c.moveToNext()) {
            result++;
        }
        c.close();
        return result;
    }

    /**
     * Whether a contact has been called for a particular issue and contact.
     * @param issueId
     * @param contactId
     */
    public boolean hasCalled(String issueId, String contactId) {
        contactId = sanitizeContactId(contactId);
        Cursor c = getReadableDatabase().rawQuery("SELECT " + CallsColumns.TIMESTAMP + " FROM " +
                CALLS_TABLE_NAME + " WHERE " + CallsColumns.ISSUE_ID + " = ? AND " +
                CallsColumns.CONTACT_ID + " = ?", new String[] {issueId, contactId});
        boolean result = c.getCount() > 0;
        c.close();
        return result;
    }

    /**
     * The results for calls made for a particular issue and contact.
     * @param issueId
     * @param contactId
     * @return A list of the call results for this issue and contact.
     */
    public List<String> getCallResults(String issueId, String contactId) {
        contactId = sanitizeContactId(contactId);
        Cursor c = getReadableDatabase().rawQuery("SELECT " + CallsColumns.RESULT + " FROM " +
                CALLS_TABLE_NAME + " WHERE " + CallsColumns.ISSUE_ID + " = ? AND " +
                CallsColumns.CONTACT_ID + " = ? ORDER BY " + CallsColumns.TIMESTAMP,
                new String[] {issueId, contactId});
        List<String> result = new ArrayList<>();
        while (c.moveToNext()) {
            result.add(c.getString(0));
        }
        c.close();
        return result;
    }

    /**
     * Whether a call has been made "today" (local time) for a particular issue and contact.
     * @param issueId
     * @param contactId
     * @return True if so, false otherwise.
     */
    public boolean hasCalledToday(String issueId, String contactId) {
        contactId = sanitizeContactId(contactId);
        Calendar rightNow = mTimeProvider.getCalendar();
        rightNow.set(Calendar.HOUR_OF_DAY, 0);
        rightNow.set(Calendar.MINUTE, 0);
        rightNow.set(Calendar.SECOND, 0);
        String[] selectionArgs = new String[] {issueId, contactId, "" + rightNow.getTimeInMillis()};
        Cursor c = getReadableDatabase().rawQuery("SELECT " + CallsColumns.TIMESTAMP + " FROM " +
                        CALLS_TABLE_NAME + " WHERE " + CallsColumns.ISSUE_ID + " = ? AND " +
                        CallsColumns.CONTACT_ID + " = ? AND " + CallsColumns.TIMESTAMP +
                        " >= ? GROUP BY " + CallsColumns.RESULT, selectionArgs);
        boolean result = c.getCount() > 0;
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
     * Gets the list of timestamps of calls of a particular type (voicemail, unavailable, contact)
     * that this user has made
     */
    public List<Long> getCallTimestampsForType(Outcome.Status status) {
        String statusName = status.toString();

        Cursor c = getReadableDatabase().rawQuery(
                "SELECT " + CallsColumns.TIMESTAMP + " FROM " + CALLS_TABLE_NAME + " WHERE " +
                CallsColumns.RESULT + " = '" + statusName + "'", null);
        List<Long> result = new ArrayList<>();
        while (c.moveToNext()) {
            result.add(c.getLong(0));
        }
        c.close();
        return result;
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
            Pair<String, Integer> next = new Pair<>(c.getString(0), c.getInt(1));
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

    @VisibleForTesting
    public static String sanitizeContactId(String contactId) {
        // TODO this only works on single quotes and not double quotes. Triple quotes are still
        // an unknown issue. Should use a regex or something.
        if (contactId.contains("'") && !contactId.contains("''")) {
            return contactId.replace("'", "''");
        }
        return contactId;
    }
}
