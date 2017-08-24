package org.a5calls.android.a5calls.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Outcome implements Parcelable {

    public String label;
    public Status status;

    public Outcome(String label, Status status) {
        this.label = label;
        this.status = status;
    }

    protected Outcome(Parcel in) {
        label = in.readString();
        status = Status.fromString(in.readString());
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
        dest.writeString(status.toString());
    }

    public enum Status {
        UNAVAILABLE("unavailable"),
        VOICEMAIL("voicemail"),
        CONTACT("contact"),
        SKIP("skip"),
        UNKNOWN("unknown");

        String status;

        Status(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return status;
        }

        public static Status fromString(String input) {
            for (Status status : values()) {
                if (status.toString().equals(input)) {
                    return status;
                }
            }

            return UNKNOWN;
        }
    }
}
