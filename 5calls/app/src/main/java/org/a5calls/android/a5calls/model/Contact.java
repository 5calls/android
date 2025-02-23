package org.a5calls.android.a5calls.model;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;

import org.a5calls.android.a5calls.R;

/**
 * Represents a contact.
 *
 */
public class Contact implements Parcelable {
    public String id;
    public String name;
    public String phone;
    public String photoURL;
    public String party;
    public String state;
    public String reason;
    public String area;
    public String district;
    public FieldOffice[] field_offices;

    protected Contact(Parcel in) {
        id = in.readString();
        name = in.readString();
        phone = in.readString();
        photoURL = in.readString();
        party = in.readString();
        state = in.readString();
        reason = in.readString();
        area = in.readString();
        district = in.readString();
        field_offices = in.createTypedArray(FieldOffice.CREATOR);
    }

    public static final Creator<Contact> CREATOR = new Creator<Contact>() {
        @Override
        public Contact createFromParcel(Parcel in) {
            return new Contact(in);
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
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
        dest.writeString(phone);
        dest.writeString(photoURL);
        dest.writeString(party);
        dest.writeString(state);
        dest.writeString(reason);
        dest.writeString(area);
        dest.writeString(district);
        dest.writeTypedArray(field_offices, PARCELABLE_WRITE_RETURN_VALUE);
    }

    public String getDescription(Resources res) {
        if (TextUtils.isEmpty(state)) {
            return "";
        }
        if (!TextUtils.isEmpty(party)) {
            if (!TextUtils.isEmpty(district)) {
                return res.getString(R.string.contact_political_details_all,
                        name, party, state, district);
            }
            return res.getString(R.string.contact_political_details_party_state,
                    name, party, state);
        }
        return res.getString(R.string.contact_political_details_state,
                name, state);
    }
}
