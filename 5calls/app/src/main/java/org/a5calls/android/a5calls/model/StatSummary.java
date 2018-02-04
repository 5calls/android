package org.a5calls.android.a5calls.model;

/**
 * Represents a summary of stats from the server
 * {"stats": {
 * "contact": 221,
 * "voicemail": 158,
 * "unavailable": 32
 }}
 */
public class StatSummary {
    public int contact;
    public int voicemail;
    public int unavailable;

    public StatSummary(int contact, int voicemail, int unavailable) {
        this.contact = contact;
        this.voicemail = voicemail;
        this.unavailable = unavailable;
    }

    public int getTotalCalls() {
        return contact + voicemail + unavailable;
    }
}
