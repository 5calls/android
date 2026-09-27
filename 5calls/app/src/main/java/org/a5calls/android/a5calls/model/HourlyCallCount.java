package org.a5calls.android.a5calls.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class HourlyCallCount implements Parcelable {
    public long time;
    public int count;

    protected HourlyCallCount(Parcel in) {
        time = in.readLong();
        count = in.readInt();
    }

    public Date getTime() {
        return new Date(time * 1000);
    }

    public static final Creator<HourlyCallCount> CREATOR = new Creator<HourlyCallCount>() {
        @Override
        public HourlyCallCount createFromParcel(Parcel in) {
            return new HourlyCallCount(in);
        }

        @Override
        public HourlyCallCount[] newArray(int size) {
            return new HourlyCallCount[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(time);
        dest.writeInt(count);
    }
}
