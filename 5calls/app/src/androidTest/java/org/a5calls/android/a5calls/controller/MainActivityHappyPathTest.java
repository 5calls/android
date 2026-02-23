package org.a5calls.android.a5calls.controller;

import android.content.Context;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.volley.toolbox.HttpResponse;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.FakeJSONData;
import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.DatabaseHelper;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withInputType;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for MainActivity that tests the happy path.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityHappyPathTest extends MainActivityBaseTest {

    // Custom matcher to check if a CollapsingToolbarLayout's title contains specific text
    public static Matcher<View> withCollapsingToolbarTitle(final Matcher<String> textMatcher) {
        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                if (!(view instanceof CollapsingToolbarLayout)) {
                    return false;
                }
                CollapsingToolbarLayout toolbarLayout = (CollapsingToolbarLayout) view;
                CharSequence title = toolbarLayout.getTitle();
                return title != null && textMatcher.matches(title.toString());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with toolbar title: ");
                textMatcher.describeTo(description);
            }
        };
    }

    /**
     * Sets up mock responses for API calls
     */
    private void setupMockResponses(boolean isSplit, boolean hasLocation) {
        // Set up the mock to handle all possible requests with appropriate responses
        mHttpStack.clearUrlPatternResponses();

        // Use FakeJSONData for mock issues response
        JSONArray issuesArray = FakeJSONData.getIssueJSON();
        HttpResponse issuesResponse = new HttpResponse(200, new ArrayList<>(), issuesArray.toString().getBytes());
        mHttpStack.setResponseForUrlPattern("issues", issuesResponse);

        // Use FakeJSONData for mock contacts response
        if (hasLocation) {
            JSONObject contactsResponseJson = FakeJSONData.getRepsJSON(isSplit);
            HttpResponse contactsResponse = new HttpResponse(200, new ArrayList<>(), contactsResponseJson.toString().getBytes());
            mHttpStack.setResponseForUrlPattern("reps", contactsResponse);
        }

        // Use FakeJSONData for mock report response
        JSONObject reportResponseJson = FakeJSONData.getReportJSON();
        HttpResponse reportResponse = new HttpResponse(200, new ArrayList<>(), reportResponseJson.toString().getBytes());
        mHttpStack.setResponseForUrlPattern("report", reportResponse);

        // Set a default response for any other requests
        mHttpStack.setResponseToReturn(new HttpResponse(200, new ArrayList<>(), "{}".getBytes()));
    }

    @Test
    public void testMainUILoadsCorrectly() throws JSONException {
        setupMockResponses(/*isSplit=*/false, /*hasLocation=*/true);

        setupMockRequestQueue();

        launchMainActivity(1000);

        // Verify the location placeholder in the header is not shown.
        onView(withContentDescription("5 Calls for BOWLING GREEN")).check(matches(isDisplayed()));

        // Verify that the toolbar is displayed
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));

        // Verify that the issues recycler view is displayed
        onView(withId(R.id.issues_recycler_view)).check(matches(isDisplayed()));

        // Verify that the filter spinner is displayed
        onView(withId(R.id.filter)).check(matches(isDisplayed()));

        // Verify that a real issue is displayed (using the first issue from the real data)
        onView(withText("Condemn a US Takeover of Gaza")).check(matches(isDisplayed()));

        // Check that the collapsing toolbar is displayed and contains the location text
        onView(withId(R.id.collapsing_toolbar))
                .check(matches(isDisplayed()))
                .check(matches(withCollapsingToolbarTitle(containsString("BOWLING GREEN"))));

        // Check that no location error was shown.
        onView(withText(R.string.low_accuracy_warning)).check(doesNotExist());
    }

    @Test
    public void testMainUILoadsCorrectly_SplitWarning() {
        setupMockResponses(/*isSplit=*/true, /*hasLocation=*/true);

        setupMockRequestQueue();

        launchMainActivity(1000);

        // Verify that a real issue is displayed (using the first issue from the real data)
        onView(withText("Condemn a US Takeover of Gaza")).check(matches(isDisplayed()));

        // Check that the location error was shown that is specific to split districts.
        onView(withText(R.string.split_district_warning)).check(matches(isDisplayed()));

        // No button to set location is shown because some location was set.
        onView(withContentDescription(R.string.first_location_title)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testMainUILoadsCorrectly_NoLocation() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Clear the location.
        String address = AccountManager.Instance.getAddress(context);
        AccountManager.Instance.setAddress(context, "");
        setupMockResponses(/*isSplit=*/false, /*hasLocation=*/false);

        setupMockRequestQueue();

        launchMainActivity(1000);

        // Verify that a real issue is displayed (using the first issue from the real data)
        onView(withText("Condemn a US Takeover of Gaza")).check(matches(isDisplayed()));

        // Verify that a "set your location" button is displayed.
        onView(withContentDescription(R.string.first_location_title)).check(matches(isDisplayed()));

        // No calls to make displayed.
        onView(withText("3 calls to make")).check(doesNotExist());
        onView(withText("2 calls to make")).check(doesNotExist());

        // Set the address again for the sake of the next test.
        AccountManager.Instance.setAddress(context, address);
    }

    @Test
    public void testMainUILoadsCorrectly_noLocation_placeholderShown() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Clear the location.
        String address = AccountManager.Instance.getAddress(context);
        AccountManager.Instance.setAddress(context, "");
        setupMockResponses(/*isSplit=*/false, /*hasLocation=*/false);
        setupMockRequestQueue();

        launchMainActivity(1000);

        // Verify that the demo issue is displayed.
        onView(withText(R.string.demo_issue_name)).check(matches(isDisplayed()));
        // There should be a "1 call to make" note for the demo issue.
        onView(withText(R.string.call_count_one)).check(matches(isDisplayed()));

        // Reset address.
        AccountManager.Instance.setAddress(context, address);
    }

    @Test
    public void testMainUILoadCorrectly_oneCall_placeholderShown() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        setupMockResponses(/*isSplit=*/false, /*hasLocation=*/true);
        setupMockRequestQueue();
        DatabaseHelper databaseHelper = AppSingleton.getInstance(context).getDatabaseHelper();
        // Add three fake calls.
        databaseHelper.addCall("issueId", "issueName", "contactId", "contactName", "result", "location");

        launchMainActivity(1000);

        // Verify that the demo issue is displayed.
        onView(withText(R.string.demo_issue_name)).check(matches(isDisplayed()));
        // There should be a "1 call to make" note for the demo issue.
        onView(withText(R.string.call_count_one)).check(matches(isDisplayed()));

        // Reset the database.
        databaseHelper.getWritableDatabase().delete("UserCallsDatabase", null, null);
    }

    @Test
    public void testMainUILoadCorrectly_twoCalls_placeholderNotShown() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        setupMockResponses(/*isSplit=*/false, /*hasLocation=*/true);
        setupMockRequestQueue();
        DatabaseHelper databaseHelper = AppSingleton.getInstance(context).getDatabaseHelper();
        // Add four fake calls.
        databaseHelper.addCall("issueId", "issueName", "contactId", "contactName", "result", "location");
        databaseHelper.addCall("issueId", "issueName", "contactId", "contactName", "result", "location");

        launchMainActivity(1000);

        // Verify that the demo issue is not displayed.
        onView(withText(R.string.demo_issue_name)).check(doesNotExist());

        // Reset the database.
        databaseHelper.getWritableDatabase().delete("UserCallsDatabase", null, null);
    }

    @Test
    public void testMainUILoadCorrectly_fourCalls_placeholderPrefSet_placeholderShown() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        setupMockResponses(/*isSplit=*/false, /*hasLocation=*/true);
        setupMockRequestQueue();
        DatabaseHelper databaseHelper = AppSingleton.getInstance(context).getDatabaseHelper();
        // Add four fake calls.
        databaseHelper.addCall("issueId", "issueName", "contactId", "contactName", "result", "location");
        databaseHelper.addCall("issueId", "issueName", "contactId", "contactName", "result", "location");
        databaseHelper.addCall("issueId", "issueName", "contactId", "contactName", "result", "location");
        databaseHelper.addCall("issueId", "issueName", "contactId", "contactName", "result", "location");
        // Pretend the user has turned on the placeholder anyway in settings.
        AccountManager.Instance.setShowPlaceholderIssue(context, true);

        launchMainActivity(1000);

        // Verify that the demo issue is displayed with "one call to make".
        onView(withText(R.string.demo_issue_name)).check(matches(isDisplayed()));
        onView(withText(R.string.demo_previous_call_stats_one)).check(doesNotExist());
        onView(withText(R.string.call_count_one)).check(matches(isDisplayed()));

        // Reset the database.
        databaseHelper.getWritableDatabase().delete("UserCallsDatabase", null, null);
        AccountManager.Instance.setShowPlaceholderIssue(context, false);
    }

    @Test
    public void testMainUILoadCorrectly_placeholderAlreadyCalled_placeholderPrefSet_placeholderShown() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        setupMockResponses(/*isSplit=*/false, /*hasLocation=*/true);
        setupMockRequestQueue();
        // The user has already done the placeholder call once.
        AccountManager.Instance.setPlaceholderIssueCalled(context, true);
        // Pretend the user has turned on the placeholder anyway in settings.
        AccountManager.Instance.setShowPlaceholderIssue(context, true);

        launchMainActivity(1000);

        // Verify that the demo issue is displayed.
        onView(withText(R.string.demo_issue_name)).check(matches(isDisplayed()));
        // The "one pretend previous call" is shown.
        onView(withText(R.string.demo_previous_call_stats_one)).check(matches(isDisplayed()));

        // Reset state
        AccountManager.Instance.setPlaceholderIssueCalled(context, false);
        AccountManager.Instance.setShowPlaceholderIssue(context, false);
    }


    @Test
    public void testNavigationDrawerOpens() throws JSONException {
        setupMockResponses(/*isSplit=*/ false, /*hasLocation=*/true);

        setupMockRequestQueue();

        launchMainActivity(1000);

        // Verify that the drawer layout is displayed
        onView(withId(R.id.drawer_layout)).check(matches(isDisplayed()));

        // Verify that the toolbar is displayed
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));

        // Open the drawer using the activity's drawer layout directly
        scenario.onActivity(activity -> {
            DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // Wait for drawer animation to complete
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify that the navigation view is now displayed
        onView(withId(R.id.navigation_view)).check(matches(isDisplayed()));

        // Verify that navigation menu items are displayed
        onView(withText("About 5 Calls")).check(matches(isDisplayed()));
        onView(withText("Your impact")).check(matches(isDisplayed()));
        onView(withText("Settings")).check(matches(isDisplayed()));
        onView(withText("FAQ")).check(matches(isDisplayed()));
        onView(withText("Update location")).check(matches(isDisplayed()));
    }

    @Test
    // TODO: Consider moving to a tutorial-specific test file.
    public void MainActivity_ShowsTutorialOnce() throws InterruptedException {
        // Mark tutorial as not seen yet.
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AccountManager.Instance.setTutorialSeen(context, false);
        // Clear the location.
        String address = AccountManager.Instance.getAddress(context);
        AccountManager.Instance.setAddress(context, "");

        // Now we are in the state of starting the app for the first time.

        setupMockResponses(/*isSplit=*/ false, /*hasLocation=*/true);
        setupMockRequestQueue();

        launchMainActivity(1000);

        // First tutorial screen shown.
        onView(withText(R.string.about_p2)).check(matches(isDisplayed()));
        onView(allOf(withText(R.string.next), isDisplayed())).perform(click());

        // Second tutorial screen shown.
        onView(withText(R.string.about_p2_2)).check(matches(isDisplayed()));
        onView(allOf(withText(R.string.next), isDisplayed())).perform(click());

        // Third tutorial screen shown.
        onView(withText(R.string.about_splash_3_1)).check(matches(isDisplayed()));
        onView(allOf(withText(R.string.next), isDisplayed())).perform(click());

        // Fourth tutorial screen shown
        onView(withText(R.string.about_splash_4_1)).check(matches(isDisplayed()));
        onView(allOf(withText(R.string.get_started_btn), isDisplayed())).perform(click());

        // Location screen shown.
        onView(withText(R.string.location_prompt)).check(matches(isDisplayed()));
        onView(withText(R.string.skip_location_btn)).perform(click());

        // When we reach main activity, the tutorial is seen and the button
        // to set location is shown.
        Thread.sleep(1000);
        onView(withContentDescription(R.string.first_location_title)).check(matches(isDisplayed()));
        assertTrue(AccountManager.Instance.isTutorialSeen(context));

        // Put the address back.
        AccountManager.Instance.setAddress(context, address);
    }
}