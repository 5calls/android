package org.a5calls.android.a5calls.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a customized script for a specific contact.
 */
public class CustomizedContactScript implements Parcelable {
    public String id;
    public String script;

    public CustomizedContactScript() {
    }

    public CustomizedContactScript(String id, String script) {
        this.id = id;
        this.script = script;
    }

    protected CustomizedContactScript(Parcel in) {
        id = in.readString();
        script = in.readString();
    }

    public static final Creator<CustomizedContactScript> CREATOR = new Creator<CustomizedContactScript>() {
        @Override
        public CustomizedContactScript createFromParcel(Parcel in) {
            return new CustomizedContactScript(in);
        }

        @Override
        public CustomizedContactScript[] newArray(int size) {
            return new CustomizedContactScript[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(script);
    }
}