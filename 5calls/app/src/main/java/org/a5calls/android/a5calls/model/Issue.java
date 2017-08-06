package org.a5calls.android.a5calls.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents an issue.
 */
public class Issue implements Parcelable {
    // TODO: We need to store local state about which issues have been modified by the user, i.e.
    // which contacts have been called.
    public String id;
    public String name;
    public String reason;
    public String script;
    public boolean inactive;

    public Contact[] contacts;
    public String[] outcomes;

    protected Issue(Parcel in) {
        id = in.readString();
        name = in.readString();
        reason = in.readString();
        script = in.readString();
        inactive = in.readInt() != 0;
        contacts = in.createTypedArray(Contact.CREATOR);
        outcomes = in.createStringArray();
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
        dest.writeInt(inactive ? 1 : 0);
        dest.writeTypedArray(contacts, PARCELABLE_WRITE_RETURN_VALUE);
        dest.writeStringArray(outcomes);
    }
}
