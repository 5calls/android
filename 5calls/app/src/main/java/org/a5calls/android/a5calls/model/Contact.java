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
    public static final String AREA_HOUSE = "US House";
    public static final String AREA_SENATE = "US Senate";
    public static final String AREA_STATE_LOWER = "StateLower";
    public static final String AREA_STATE_UPPER = "StateUpper";
    public static final String AREA_GOVERNOR = "Governor";
    public static final String AREA_ATTORNEY_GENERAL = "AttorneyGeneral";
    public static final String AREA_SECRETARY_OF_STATE = "SecretaryOfState";

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
    // This defaults to false when populated with JSON that doesn't have
    // the equivalent key. It is just used within the app to denote a placeholder
    // rep, e.g. used for a vacant seat.
    public boolean isPlaceholder;

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
        isPlaceholder = in.readInt() == 1;
    }

    protected Contact(String id, String name, String reason, String area, boolean isPlaceholder) {
        this.id = id;
        this.name = name;
        this.reason = reason;
        this.area = area;
        this.isPlaceholder = isPlaceholder;
    }

    public static Contact createPlaceholder(String id, String name, String reason, String area) {
        return new Contact(id, name, reason, area, /* isPlaceholder= */ true);
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
        dest.writeInt(isPlaceholder ? 1 : 0);
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
