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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
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
            assertEquals(expected.district, actual.district);
            assertArrayEquals(expected.field_offices, actual.field_offices);
        }
    }

    @Test
    public void testDescriptionSenate() {
        List<Contact> contacts = getTestContacts();
        String description = contacts.get(1).getDescription(getApplicationContext().getResources());
        assertEquals("Kirsten Gillibrand is a Democrat from NY.", description);
    }

    @Test
    public void testDescriptionHouse() {
        List<Contact> contacts = getTestContacts();
        String description = contacts.get(0).getDescription(getApplicationContext().getResources());
        assertEquals("Dan Goldman is a Democrat from NY District 10.", description);
    }

    @Test
    public void testDescriptionAttorneyGeneral() {
        List<Contact> contacts = getTestContacts();
        String description = contacts.get(3).getDescription(getApplicationContext().getResources());
        assertEquals("Letitia A. James is from NY.", description);
    }

    @Test
    public void testDescriptionGovernor() {
        List<Contact> contacts = getTestContacts();
        String description = contacts.get(4).getDescription(getApplicationContext().getResources());
        assertEquals("Kathy Hochul is a Democrat from NY.", description);
    }

    @Test
    public void testDescriptionMissingFields() {
        Contact contact = TestModelUtils.createContact("contactId", "name");
        assertEquals("", contact.getDescription(getApplicationContext().getResources()));
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
        Type listType = new TypeToken<ArrayList<Contact>>() {
        }.getType();
        return gson.fromJson(jsonArray.toString(), listType);
    }
}