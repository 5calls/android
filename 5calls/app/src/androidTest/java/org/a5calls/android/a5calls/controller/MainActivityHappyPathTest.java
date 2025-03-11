package org.a5calls.android.a5calls.controller;

import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.volley.toolbox.HttpResponse;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import org.a5calls.android.a5calls.FakeJSONData;
import org.a5calls.android.a5calls.R;
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
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
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
    private void setupMockResponses() throws JSONException {
        // Use FakeJSONData for mock issues response
        JSONArray issuesArray = FakeJSONData.getIssueJSON();
        HttpResponse issuesResponse = new HttpResponse(200, new ArrayList<>(), issuesArray.toString().getBytes());

        // Use FakeJSONData for mock contacts response
        JSONObject contactsResponseJson = FakeJSONData.getRepsJSON();
        HttpResponse contactsResponse = new HttpResponse(200, new ArrayList<>(), contactsResponseJson.toString().getBytes());

        // Use FakeJSONData for mock report response
        JSONObject reportResponseJson = FakeJSONData.getReportJSON();
        HttpResponse reportResponse = new HttpResponse(200, new ArrayList<>(), reportResponseJson.toString().getBytes());

        // Set up the mock to handle all possible requests with appropriate responses
        mHttpStack.clearUrlPatternResponses();
        mHttpStack.setResponseForUrlPattern("issues", issuesResponse);
        mHttpStack.setResponseForUrlPattern("reps", contactsResponse);
        mHttpStack.setResponseForUrlPattern("report", reportResponse);

        // Set a default response for any other requests
        mHttpStack.setResponseToReturn(new HttpResponse(200, new ArrayList<>(), "{}".getBytes()));
    }

    @Test
    public void testMainUILoadsCorrectly() throws JSONException {
        setupMockResponses();

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

        // Check that the toolbar is displayed
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));

        // Check that the collapsing toolbar is displayed and contains the location text
        onView(withId(R.id.collapsing_toolbar))
            .check(matches(isDisplayed()))
            .check(matches(withCollapsingToolbarTitle(containsString("BOWLING GREEN"))));
    }

    @Test
    public void testNavigationDrawerOpens() throws JSONException {
        setupMockResponses();

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
}