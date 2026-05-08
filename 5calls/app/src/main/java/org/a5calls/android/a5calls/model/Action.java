package org.a5calls.android.a5calls.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents an action, such as a survey, that can be shown
 * after an issue's calls are completed.
 */
public class Action implements Parcelable {
    public static final String TYPE_FREEFORM = "freeform";
    public static final String TYPE_DONATE = "donate";

    public String type;
    public String title;
    public String body;
    public String buttonText;
    public String buttonURL;

    public Action() {
    }

    protected Action(Parcel in) {
        type = in.readString();
        title = in.readString();
        body = in.readString();
        buttonText = in.readString();
        buttonURL = in.readString();
    }

    public static final Creator<Action> CREATOR = new Creator<Action>() {
        @Override
        public Action createFromParcel(Parcel in) {
            return new Action(in);
        }

        @Override
        public Action[] newArray(int size) {
            return new Action[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeString(title);
        dest.writeString(body);
        dest.writeString(buttonText);
        dest.writeString(buttonURL);
    }
}
