package org.a5calls.android.a5calls;

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
    public Contact[] contacts;

    protected Issue(Parcel in) {
        id = in.readString();
        name = in.readString();
        reason = in.readString();
        script = in.readString();
        contacts = in.createTypedArray(Contact.CREATOR);
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
        dest.writeTypedArray(contacts, PARCELABLE_WRITE_RETURN_VALUE);
    }
}
