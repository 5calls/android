package org.a5calls.android.a5calls.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import org.a5calls.android.a5calls.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Models the result of a phone call with a represent or office
 * Holds the label displayed to the user and the status reported to the backend
 */
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
        UNKNOWN("unknown"),
        VM("vm"), // deprecated
        CONTACTED("contacted"); // deprecated

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

    public static String getDisplayString(Context context, String outcome) {
        String result;

        switch (Outcome.Status.fromString(outcome)) {
            case VOICEMAIL:
            case VM:
                result = context.getResources().getString(R.string.voicemail_btn);
                break;
            case UNAVAILABLE:
                result = context.getResources().getString(R.string.unavailable_btn);
                break;
            case CONTACT:
            case CONTACTED:
                result = context.getResources().getString(R.string.made_contact_btn);
                break;
            default:
                result = outcome;
        }

        return result;
    }

    public static List<Issue> filterSkipOutcomes(List<Issue> issues) {
        List<Issue> result = new ArrayList<>();

        if (issues != null) {
            for (Issue issue : issues) {
                if (issue.outcomeModels != null) {
                    List<Outcome> filteredOutcomes = new ArrayList<>();

                    for (Outcome outcome : issue.outcomeModels) {
                        if (!Outcome.Status.SKIP.equals(outcome.status)) {
                            filteredOutcomes.add(outcome);
                        }
                    }

                    issue.outcomeModels = filteredOutcomes;
                }

                result.add(issue);
            }
        }

        return result;
    }
}
