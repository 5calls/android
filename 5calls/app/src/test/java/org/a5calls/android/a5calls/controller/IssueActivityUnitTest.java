package org.a5calls.android.a5calls.controller;

import android.os.Parcel;

import org.a5calls.android.a5calls.model.Category;
import org.a5calls.android.a5calls.model.Issue;
import org.a5calls.android.a5calls.model.IssueStats;
import org.a5calls.android.a5calls.model.TestModelUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;

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
}
