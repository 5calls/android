package org.a5calls.android.a5calls.model;

import org.a5calls.android.a5calls.util.ScriptReplacements;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for ScriptReplacements.
 */
@RunWith(AndroidJUnit4.class)
public class ScriptReplacementsTest {

    private Contact mHouseContact = TestModelUtils.createContact(
            "house1", "Housy McHouseface", "US House");
    private Contact mSenateContact = TestModelUtils.createContact(
            "sen1", "Senatey McDefinitelyOld", "US Senate");
    private Contact mUnknownContact = TestModelUtils.createContact(
            "may1", "Mayor McMayorface", "Mayor");
    private String mLocation = "San Francisco";
    private String mUserName = "Lyra";

    @Test
    public void testNoReplacements() {
        String script = "The meaning of life, the universe, and everything.";
        String result = ScriptReplacements.replacing(
                getApplicationContext(), script, mHouseContact, mLocation, mUserName);
        // Nothing changed.
        assertEquals(script, result);
    }

    @Test
    public void testSingleContactReplacement_houseContact() {
        String script = "Hello [REP/SEN NAME], I'm calling to register an opinion.";
        String result = ScriptReplacements.replacing(
                getApplicationContext(), script, mHouseContact, mLocation, mUserName);
        assertEquals("Hello Representative Housy McHouseface, I'm calling to register an opinion.", result);
    }

    @Test
    public void testSingleContactReplacement_representative_houseContact() {
        String script = "Hello [REPRESENTATIVE NAME], I'm calling to register an opinion.";
        String result = ScriptReplacements.replacing(
                getApplicationContext(), script, mHouseContact, mLocation, mUserName);
        assertEquals("Hello Representative Housy McHouseface, I'm calling to register an opinion.", result);
    }

    @Test
    public void testSingleContactReplacement_senator_houseContact() {
        String script = "Hello [SENATOR NAME], I'm calling to register an opinion.";
        String result = ScriptReplacements.replacing(
                getApplicationContext(), script, mHouseContact, mLocation, mUserName);
        assertEquals("Hello Representative Housy McHouseface, I'm calling to register an opinion.", result);
    }

    @Test
    public void testSingleContactReplacement_senateContact() {
        String script = "Hello [REP/SEN NAME], I'm calling to register an opinion.";
        String result = ScriptReplacements.replacing(
                getApplicationContext(), script, mSenateContact, mLocation, mUserName);
        assertEquals("Hello Senator Senatey McDefinitelyOld, I'm calling to register an opinion.", result);
    }

    @Test
    public void testSingleContactReplacement_representative_senateContact() {
        String script = "Hello [REPRESENTATIVE NAME], I'm calling to register an opinion.";
        String result = ScriptReplacements.replacing(
                getApplicationContext(), script, mSenateContact, mLocation, mUserName);
        assertEquals("Hello Senator Senatey McDefinitelyOld, I'm calling to register an opinion.", result);
    }

    @Test
    public void testSingleContactReplacement_senator_senateContact() {
        String script = "Hello [SENATOR NAME], I'm calling to register an opinion.";
        String result = ScriptReplacements.replacing(
                getApplicationContext(), script, mSenateContact, mLocation, mUserName);
        assertEquals("Hello Senator Senatey McDefinitelyOld, I'm calling to register an opinion.", result);
    }

    @Test
    public void testUnknownAreaContactReplacement() {
        String script = "Hello [REP/SEN NAME], I'm calling to register an opinion.";
        String result = ScriptReplacements.replacing(
                getApplicationContext(), script, mUnknownContact, mLocation, mUserName);
        assertEquals("Hello Mayor McMayorface, I'm calling to register an opinion.", result);
    }

    @Test
    public void testLocationReplacement_cityZip() {
        String script = "I'm calling from [CITY/ZIP].";
        String result = ScriptReplacements.replacing(
                getApplicationContext(), script, mHouseContact, mLocation, mUserName);
        assertEquals("I'm calling from San Francisco.", result);
    }

    @Test
    public void testLocationReplacement_cityState() {
        String script = "I'm calling from [CITY/STATE].";
        String result = ScriptReplacements.replacing(
                getApplicationContext(), script, mHouseContact, mLocation, mUserName);
        assertEquals("I'm calling from San Francisco.", result);
    }

    @Test
    public void testUserNameReplacement() {
        String script = "My name is [NAME] and I'm calling to register an opinion.";
        String result = ScriptReplacements.replacing(
                getApplicationContext(), script, mSenateContact, mLocation, mUserName);
        assertEquals("My name is Lyra and I'm calling to register an opinion.", result);
    }

    @Test
    public void testSubscriptReplacement_chooseHouse() {
        String script = "Hello!\n\n" +
                "**WHEN CALLING HOUSE:**\n" +
                "I'm calling to urge **[REPRESENTATIVE NAME]** to support house bill.\n\n" +
                "**WHEN CALLING SENATE:**\n" +
                "I'm calling to urge **[SENATOR NAME]** to support senate bill.\n\n" +
                "Thank you for your time and consideration.";
        String result = ScriptReplacements.replacing(
                getApplicationContext(), script, mHouseContact, mLocation, mUserName);
        assertEquals("Hello!\n\n" +
                "I'm calling to urge **Representative Housy McHouseface** to support house bill.\n\n" +
                "Thank you for your time and consideration.", result);
    }

    @Test
    public void testSubscriptReplacement_chooseSenate() {
        String script = "Hello!\n\n" +
                "**WHEN CALLING HOUSE:**\n" +
                "I'm calling to urge **[REPRESENTATIVE NAME]** to support house bill.\n\n" +
                "**WHEN CALLING SENATE:**\n" +
                "I'm calling to urge **[SENATOR NAME]** to support senate bill.\n\n" +
                "Thank you for your time and consideration.";
        String result = ScriptReplacements.replacing(
                getApplicationContext(), script, mSenateContact, mLocation, mUserName);
        assertEquals("Hello!\n\n" +
                "I'm calling to urge **Senator Senatey McDefinitelyOld** to support senate bill.\n\n" +
                "Thank you for your time and consideration.", result);
    }

    @Test
    public void testSubscriptReplacement_unknownArea() {
        String script = "Hello!\n\n" +
                "**WHEN CALLING HOUSE:**\n" +
                "I'm calling to urge **[REPRESENTATIVE NAME]** to support house bill.\n\n" +
                "**WHEN CALLING SENATE:**\n" +
                "I'm calling to urge **[SENATOR NAME]** to support senate bill.\n\n" +
                "Thank you for your time and consideration.";
        String result = ScriptReplacements.replacing(
                getApplicationContext(), script, mUnknownContact, mLocation, mUserName);
        String expected = "Hello!\n\n" +
                "**WHEN CALLING HOUSE:**\n" +
                "I'm calling to urge **Mayor McMayorface** to support house bill.\n\n" +
                "**WHEN CALLING SENATE:**\n" +
                "I'm calling to urge **Mayor McMayorface** to support senate bill.\n\n" +
                "Thank you for your time and consideration.";
        assertEquals(expected, result);
    }

    @Test
    public void testSubscriptReplacement_multipleHouseSections() {
        String script = """
            Hello!
            
            **WHEN CALLING HOUSE:**
            First house section with **[REPRESENTATIVE NAME]**
            
            Some middle text.
            
            **WHEN CALLING HOUSE:**
            Second house section
            
            **WHEN CALLING SENATE:**
            Senate section should be removed
            
            Thank you for your time.
            """;

        String result = ScriptReplacements.replacing(
                getApplicationContext(), script, mHouseContact, mLocation, mUserName);

        String expected = """
            Hello!
            
            First house section with **Representative Housy McHouseface**
            
            Some middle text.
            
            Second house section
            
            Thank you for your time.
            """;
        assertEquals(expected, result);
    }

    @Test
    public void testSubscriptReplacement_multipleSenateSections() {
        String script = """
            Hello!
            
            **WHEN CALLING SENATE:**
            First senate section with **[SENATOR NAME]**
            
            Some middle text.
            
            **WHEN CALLING HOUSE:**
            House section should be removed
            
            **WHEN CALLING SENATE:**
            Second senate section
            
            Thank you for your time.
            """;

        String result = ScriptReplacements.replacing(
                getApplicationContext(), script, mSenateContact, mLocation, mUserName);

        String expected = """
            Hello!
            
            First senate section with **Senator Senatey McDefinitelyOld**
            
            Some middle text.
            
            Second senate section
            
            Thank you for your time.
            """;
        assertEquals(expected, result);
    }
}
