package org.a5calls.android.a5calls.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.Collections;
import org.a5calls.android.a5calls.util.StateMapping;

import java.util.List;

import androidx.annotation.Nullable;

/**
 * Represents an issue.
 */
public class Issue implements Parcelable {
    public String id;
    public String name;
    public String permalink;
    public String reason;
    public String script;
    public boolean active;
    public String link;
    public String linkTitle;
    public String createdAt;
    public String meta = "";
    public int sort = 0;

    public List<Contact> contacts;
    public List<String> contactAreas;
    public List<Outcome> outcomeModels;
    public Category[] categories;
    public boolean isSplit;
    public IssueStats stats;
    public boolean isPlaceholder = false;
    
    public List<CustomizedContactScript> customizedScripts;

    public static Issue createPlaceholder(String id, String name, String permalink, String reason,
                 String script, boolean active, int sort, List<Contact> contacts,
                 List<String> contactAreas, List<Outcome> outcomeModels) {
        Issue issue = new Issue();
        issue.id = id;
        issue.name = name;
        issue.permalink = permalink;
        issue.reason = reason;
        issue.script = script;
        issue.active = active;
        issue.sort = sort;
        issue.contacts = contacts;
        issue.contactAreas = Collections.singletonList("demo");
        issue.outcomeModels = outcomeModels;

        issue.stats = new IssueStats(0);
        issue.isPlaceholder = true;
        return issue;
    }

    private Issue() {

    }

    protected Issue(Parcel in) {
        id = in.readString();
        name = in.readString();
        permalink = in.readString();
        reason = in.readString();
        script = in.readString();
        link = in.readString();
        linkTitle = in.readString();
        active = in.readInt() != 0;
        isSplit = in.readInt() != 0;
        createdAt = in.readString();
        meta = in.readString();
        if (meta == null) {
            meta = "";
        }
        sort = in.readInt();
        contacts = in.createTypedArrayList(Contact.CREATOR);
        contactAreas = in.createStringArrayList();
        outcomeModels = in.createTypedArrayList(Outcome.CREATOR);
        categories = in.createTypedArray(Category.CREATOR);
        stats = IssueStats.CREATOR.createFromParcel(in);
        customizedScripts = in.createTypedArrayList(CustomizedContactScript.CREATOR);
        isPlaceholder = in.readInt() != 0;
    }

    public static final Creator<Issue> CREATOR = new Creator<Issue>() {
        @Override
        public Issue createFromParcel(Parcel in) {
            return new Issue(in);
        }

        @Override
        public Issue[] newArray(int size) {
            return new Issue[size];
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
        dest.writeString(permalink);
        dest.writeString(reason);
        dest.writeString(script);
        dest.writeString(link);
        dest.writeString(linkTitle);
        dest.writeInt(active ? 1 : 0);
        dest.writeInt(isSplit ? 1 : 0);
        dest.writeString(createdAt);
        dest.writeString(meta);
        dest.writeInt(sort);
        dest.writeTypedList(contacts);
        dest.writeStringList(contactAreas);
        dest.writeTypedList(outcomeModels);
        dest.writeTypedArray(categories, PARCELABLE_WRITE_RETURN_VALUE);
        stats.writeToParcel(dest, flags);
        dest.writeTypedList(customizedScripts);
        dest.writeInt(isPlaceholder ? 1 : 0);
    }
    
    public String getScriptForContact(String contactId) {
        if (customizedScripts != null && !TextUtils.isEmpty(contactId)) {
            for (CustomizedContactScript customizedScript : customizedScripts) {
                if (TextUtils.equals(customizedScript.id, contactId)) {
                    return customizedScript.script;
                }
            }
        }
        return script;
    }

    public @Nullable String getStateName() {
        if (TextUtils.isEmpty(meta)) {
            return null;
        }
        return StateMapping.getStateName(meta);
    }
}
