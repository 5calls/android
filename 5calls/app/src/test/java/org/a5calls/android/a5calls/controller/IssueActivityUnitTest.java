package org.a5calls.android.a5calls.controller;

import android.os.Parcel;

import org.a5calls.android.a5calls.model.Category;
import org.a5calls.android.a5calls.model.Issue;
import org.a5calls.android.a5calls.model.IssueStats;
import org.a5calls.android.a5calls.model.Outcome;
import org.a5calls.android.a5calls.model.TestModelUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for IssueActivity.
 */
@RunWith(AndroidJUnit4.class)
public class IssueActivityUnitTest {

    @Test
    public void testIssueDetailMessage() {
        Issue issue = TestModelUtils.createIssue("", "Unicorn sparkles");
        Category[] categories = {Category.CREATOR.createFromParcel(Parcel.obtain())};
        categories[0].name = "Super cheetahs";
        issue.categories = categories;
        issue.stats = IssueStats.CREATOR.createFromParcel(Parcel.obtain());
        issue.stats.calls = 42;
        issue.createdAt = "2025-03-22T12:11:10Z";
        String result = IssueActivity.getIssueDetailsMessage(getApplicationContext(), issue);
        assertEquals("Category: Super cheetahs\n\n" +
                "Total calls on this topic: 42\n\n" +
                "First posted: March 22, 2025", result);
    }

    @Test
    public void testIssueDetailMessage_notEnoughCalls() {
        Issue issue = TestModelUtils.createIssue("", "Unicorn sparkles");
        Category[] categories = {Category.CREATOR.createFromParcel(Parcel.obtain())};
        categories[0].name = "Super cheetahs";
        issue.categories = categories;
        issue.stats = IssueStats.CREATOR.createFromParcel(Parcel.obtain());
        issue.stats.calls = 2;
        issue.createdAt = "2025-03-22T12:11:10Z";
        String result = IssueActivity.getIssueDetailsMessage(getApplicationContext(), issue);
        assertEquals("Category: Super cheetahs\n\n" +
                "First posted: March 22, 2025", result);
    }

    @Test
    public void testIssueDetailMessage_multipleCategories() {
        Issue issue = TestModelUtils.createIssue("", "Unicorn sparkles");
        Category[] categories = {Category.CREATOR.createFromParcel(Parcel.obtain()),
                Category.CREATOR.createFromParcel(Parcel.obtain())};
        categories[0].name = "Super cheetahs";
        categories[1].name = "Incredible Ocelots";
        issue.categories = categories;
        issue.stats = IssueStats.CREATOR.createFromParcel(Parcel.obtain());
        issue.stats.calls = 42;
        issue.createdAt = "2025-03-22T12:11:10Z";
        String result = IssueActivity.getIssueDetailsMessage(getApplicationContext(), issue);
        assertEquals("Categories: Super cheetahs, Incredible Ocelots\n\n" +
                "Total calls on this topic: 42\n\n" +
                "First posted: March 22, 2025", result);
    }

    @Test
    public void testIssueDetailMessage_cannotParseDate() {
        // Test this since it can throw an exception.
        Issue issue = TestModelUtils.createIssue("", "Unicorn sparkles");
        Category[] categories = {Category.CREATOR.createFromParcel(Parcel.obtain())};
        categories[0].name = "Super cheetahs";
        issue.categories = categories;
        issue.stats = IssueStats.CREATOR.createFromParcel(Parcel.obtain());
        issue.stats.calls = 42;
        issue.createdAt = "sandwich2025-03-22T12:11:10Z";
        String result = IssueActivity.getIssueDetailsMessage(getApplicationContext(), issue);
        assertEquals("Category: Super cheetahs\n\n" +
                "Total calls on this topic: 42\n\n", result);
    }

    @Test
    public void testIssueDetailMessage_noCategories() {
        // Test this since it can throw an exception.
        Issue issue = TestModelUtils.createIssue("", "Unicorn sparkles");
        issue.stats = IssueStats.CREATOR.createFromParcel(Parcel.obtain());
        issue.stats.calls = 42;
        issue.createdAt = "2025-03-22T12:11:10Z";
        String result = IssueActivity.getIssueDetailsMessage(getApplicationContext(), issue);
        assertEquals("Total calls on this topic: 42\n\n" +
                "First posted: March 22, 2025", result);
    }

    @Test
    public void testCapitalizeFirst_null() {
        assertNull(IssueActivity.capitalizeFirst(null));
    }

    @Test
    public void testCapitalizeFirst_empty() {
        assertEquals("", IssueActivity.capitalizeFirst(""));
    }

    @Test
    public void testCapitalizeFirst_singleChar() {
        // Guards the substring(1) boundary: s.length() == 1 must not throw.
        assertEquals("A", IssueActivity.capitalizeFirst("a"));
    }

    @Test
    public void testCapitalizeFirst_singleCharAlreadyUpper() {
        assertEquals("A", IssueActivity.capitalizeFirst("A"));
    }

    @Test
    public void testCapitalizeFirst_word() {
        assertEquals("Hello", IssueActivity.capitalizeFirst("hello"));
    }

    @Test
    public void testCapitalizeFirst_wordAlreadyCapitalized() {
        assertEquals("Hello", IssueActivity.capitalizeFirst("Hello"));
    }

    @Test
    public void testBuildUndoSnackbarMessage_voicemail() {
        assertEquals("Left voicemail reported",
                IssueActivity.buildUndoSnackbarMessage(
                        getApplicationContext(), Outcome.Status.VOICEMAIL));
    }

    @Test
    public void testBuildUndoSnackbarMessage_contact() {
        assertEquals("Made contact reported",
                IssueActivity.buildUndoSnackbarMessage(
                        getApplicationContext(), Outcome.Status.CONTACT));
    }

    @Test
    public void testBuildUndoSnackbarMessage_unavailable() {
        assertEquals("Unavailable reported",
                IssueActivity.buildUndoSnackbarMessage(
                        getApplicationContext(), Outcome.Status.UNAVAILABLE));
    }

    @Test
    @Config(qualifiers = "es")
    public void testBuildUndoSnackbarMessage_spanish_voicemail() {
        assertEquals("Dejé mensaje de voz registrado",
                IssueActivity.buildUndoSnackbarMessage(
                        getApplicationContext(), Outcome.Status.VOICEMAIL));
    }

    @Test
    @Config(qualifiers = "es")
    public void testBuildUndoSnackbarMessage_spanish_contact() {
        assertEquals("Hice contacto registrado",
                IssueActivity.buildUndoSnackbarMessage(
                        getApplicationContext(), Outcome.Status.CONTACT));
    }

    @Test
    @Config(qualifiers = "es")
    public void testBuildUndoSnackbarMessage_spanish_unavailable() {
        assertEquals("No disponible registrado",
                IssueActivity.buildUndoSnackbarMessage(
                        getApplicationContext(), Outcome.Status.UNAVAILABLE));
    }
}
