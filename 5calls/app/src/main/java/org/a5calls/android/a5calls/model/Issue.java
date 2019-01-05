package org.a5calls.android.a5calls.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Represents an issue.
 */
public class Issue implements Parcelable {
    public String id;
    public String name;
    public String reason;
    public String script;
    public boolean active;
    public String link;
    public String linkTitle;

    public List<Contact> contacts;
    public List<String> contactAreas;
    public List<Outcome> outcomeModels;
    public Category[] categories;
    public boolean isSplit;

    protected Issue(Parcel in) {
        id = in.readString();
        name = in.readString();
        reason = in.readString();
        script = in.readString();
        link = in.readString();
        linkTitle = in.readString();
        active = in.readInt() != 0;
        isSplit = in.readInt() != 0;
        contacts = in.createTypedArrayList(Contact.CREATOR);
        contactAreas = in.createStringArrayList();
        outcomeModels = in.createTypedArrayList(Outcome.CREATOR);
        categories = in.createTypedArray(Category.CREATOR);
    }

    public static final Creator<Issue> CREATOR = new Creator<Issue>() {
        @Override
        public Issue createFromParcel(Parcel in) {
            return new Issue(in);
        }

        @Override
        public Issue[] newArray(int size) {
            return new Issue[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(reason);
        dest.writeString(script);
        dest.writeString(link);
        dest.writeString(linkTitle);
        dest.writeInt(active ? 1 : 0);
        dest.writeInt(isSplit ? 1 : 0);
        dest.writeTypedList(contacts);
        dest.writeStringList(contactAreas);
        dest.writeTypedList(outcomeModels);
        dest.writeTypedArray(categories, PARCELABLE_WRITE_RETURN_VALUE);
    }
}
