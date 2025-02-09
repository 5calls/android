package org.a5calls.android.a5calls.model;

import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.a5calls.android.a5calls.FakeJSONData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class ContactsTest {
    @Test
    public void testReadFromJSON() {
        List<Contact> contacts = getTestContacts();
        assertNotNull(contacts);
        assertEquals(contacts.size(), 6);
    }

    @Test
    public void testParcelable() {
        ArrayList<Contact> contacts = getTestContacts();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("key", contacts);
        ArrayList<Contact> reconstructed = bundle.getParcelableArrayList("key");
        assertEquals(contacts.size(), reconstructed.size());
        for (int i = 0; i < contacts.size(); i++) {
            Contact expected = contacts.get(i);
            Contact actual = reconstructed.get(i);
            assertEquals(expected.id, actual.id);
            assertEquals(expected.name, actual.name);
            assertEquals(expected.reason, actual.reason);
            assertEquals(expected.area, actual.area);
            assertEquals(expected.party, actual.party);
            assertEquals(expected.photoURL, actual.photoURL);
            assertEquals(expected.state, actual.state);
            assertArrayEquals(expected.field_offices, actual.field_offices);
        }
    }

    private ArrayList<Contact> getTestContacts() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        JSONObject contactsResponse;
        try {
            contactsResponse = new JSONObject(FakeJSONData.REPS_DATA);
        } catch (JSONException e) {
            fail(e.getMessage());
            return new ArrayList<>();
        }
        JSONArray jsonArray = contactsResponse.optJSONArray("representatives");
        assertNotNull(jsonArray);
        Type listType = new TypeToken<ArrayList<Contact>>(){}.getType();
        return gson.fromJson(jsonArray.toString(), listType);
    }
}