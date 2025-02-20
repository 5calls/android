package org.a5calls.android.a5calls.model;

import android.os.Parcel;

/**
 * Utility functions for creating Model objects for use in tests.
 */
public class TestModelUtils {

    public static Contact createContact(String id, String name) {
        return createContact(id, name, "US House");
    }

    public static Contact createContact(String id, String name, String area) {
        Contact contact = Contact.CREATOR.createFromParcel(Parcel.obtain());
        contact.id = id;
        contact.name = name;
        contact.area = area;
        return contact;
    }

    public static Issue createIssue(String id, String name) {
        Issue issue = Issue.CREATOR.createFromParcel(Parcel.obtain());
        issue.id = id;
        issue.name = name;
        return issue;
    }
}
