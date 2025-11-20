package org.a5calls.android.a5calls.controller;

import android.content.Context;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.volley.toolbox.HttpResponse;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import org.a5calls.android.a5calls.FakeJSONData;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
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
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;

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
    private void setupMockResponses(boolean isSplit, boolean hasLocation)  {
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

        // Set the address again for the sake of the next test.
        AccountManager.Instance.setAddress(context, address);
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
    public void MainActivity_ShowsTutorialOnce() {
        // Mark tutorial as not seen yet.
        AccountManager.Instance.setTutorialSeen(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                false);

        setupMockResponses(/*isSplit=*/ false, /*hasLocation=*/true);
        setupMockRequestQueue();

        launchMainActivity(1000);

        // First tutorial screen shown.
        onView(withText(R.string.about_header)).check(matches(isDisplayed()));
        onView(withText(R.string.next)).perform(ViewAction.click())
    }
}