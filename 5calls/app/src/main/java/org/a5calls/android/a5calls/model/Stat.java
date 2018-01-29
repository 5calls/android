package org.a5calls.android.a5calls.model;

import android.os.Parcel;

import java.util.List;

/**
 * Represents a stat on the server.
 */
public class Stat {
    public String issueID;
    public String contactID;
    public String result;
    public long time;

    public static String toJsonString(List<Stat> stats) {
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < stats.size(); i++) {
            Stat stat = stats.get(i);
            result.append(String.format("{issueID: \"%s\",", stat.issueID));
            result.append(String.format("contactID: \"%s,\"", stat.contactID));
            result.append(String.format("result: \"%s\",", stat.result));
            result.append(String.format("time: : \"%s\"}", stat.time));
            if (i < stats.size() - 2) {
                result.append(",");
            }
        }
        return result.toString();
    }
}
