package org.a5calls.android.a5calls.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a field office, which has a city name and phone number field.
 */
public class FieldOffice implements Parcelable {
    public String city;
    public String phone;

    protected FieldOffice(Parcel in) {
        city = in.readString();
        phone = in.readString();
    }

    public static final Creator<FieldOffice> CREATOR = new Creator<FieldOffice>() {
        @Override
        public FieldOffice createFromParcel(Parcel in) {
            return new FieldOffice(in);
        }

        @Override
        public FieldOffice[] newArray(int size) {
            return new FieldOffice[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(city);
        dest.writeString(phone);
    }
}
