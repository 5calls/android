package org.a5calls.android.a5calls.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Outcome implements Parcelable {

    public String label;
    public String status;

    protected Outcome(Parcel in) {
        label = in.readString();
        status = in.readString();
    }

    public static final Creator<Outcome> CREATOR = new Creator<Outcome>() {
        @Override
        public Outcome createFromParcel(Parcel in) {
            return new Outcome(in);
        }

        @Override
        public Outcome[] newArray(int size) {
            return new Outcome[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(label);
        dest.writeString(status);
    }
}
