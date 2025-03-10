package org.a5calls.android.a5calls.controller;

import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HttpResponse;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.FakeJSONData;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.net.FakeRequestQueue;
import org.a5calls.android.a5calls.net.FiveCallsApi;
import org.a5calls.android.a5calls.net.MockHttpStack;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
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
public class MainActivityHappyPathTest {

    private MockHttpStack mHttpStack;
    private RequestQueue mOriginalRequestQueue;
    private FiveCallsApi mOriginalApi;
    private String mOriginalAddress;
    private ActivityScenario<MainActivity> scenario;

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

    @Before
    public void setUp() {
        // Save original state
        mOriginalRequestQueue = AppSingleton.getInstance(
                InstrumentationRegistry.getInstrumentation().getTargetContext()).getRequestQueue();
        mOriginalApi = AppSingleton.getInstance(
                InstrumentationRegistry.getInstrumentation().getTargetContext()).getJsonController();

        // Save original location
        mOriginalAddress = AccountManager.Instance.getAddress(
                InstrumentationRegistry.getInstrumentation().getTargetContext());

        // Set a mock location to avoid location prompts
        AccountManager.Instance.setAddress(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                "90210");

        // Mark tutorial as seen to bypass onboarding screen
        AccountManager.Instance.setTutorialSeen(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                true);

        // Create mock HTTP stack
        mHttpStack = new MockHttpStack();
    }

    @After
    public void tearDown() {
        // Restore original state
        AppSingleton.getInstance(
                InstrumentationRegistry.getInstrumentation().getTargetContext())
                .setRequestQueue(mOriginalRequestQueue);
        AppSingleton.getInstance(
                InstrumentationRegistry.getInstrumentation().getTargetContext())
                .setFiveCallsApi(mOriginalApi);

        // Restore original location
        AccountManager.Instance.setAddress(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                mOriginalAddress);

        // Close the activity scenario if it's open
        if (scenario != null) {
            scenario.close();
        }
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

    /**
     * Sets up the mock request queue and API
     */
    private void setupMockRequestQueue() {
        // Create a custom RequestQueue with our mock HTTP stack
        BasicNetwork basicNetwork = new BasicNetwork(mHttpStack);
        FakeRequestQueue requestQueue = new FakeRequestQueue(basicNetwork);
        requestQueue.start();

        // Replace the app's RequestQueue with our mock
        AppSingleton.getInstance(
                InstrumentationRegistry.getInstrumentation().getTargetContext())
                .setRequestQueue(requestQueue);

        // Create a new FiveCallsApi with our mock RequestQueue
        String callerId = AccountManager.Instance.getCallerID(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
        FiveCallsApi api = new FiveCallsApi(callerId, requestQueue);
        AppSingleton.getInstance(
                InstrumentationRegistry.getInstrumentation().getTargetContext())
                .setFiveCallsApi(api);
    }

    /**
     * Launches the MainActivity and waits for it to load
     */
    private void launchMainActivity() {
        // Launch the activity
        scenario = ActivityScenario.launch(MainActivity.class);

        // Wait for all requests to complete and UI to update
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMainUILoadsCorrectly() throws JSONException {
        // Set up mock responses
        setupMockResponses();

        // Set up mock request queue
        setupMockRequestQueue();

        // Launch the activity
        launchMainActivity();

        // Verify that the toolbar is displayed
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));

        // Verify that the issues recycler view is displayed
        onView(withId(R.id.issues_recycler_view)).check(matches(isDisplayed()));

        // Verify that the filter spinner is displayed
        onView(withId(R.id.filter)).check(matches(isDisplayed()));

        // Verify that a real issue is displayed (using the first issue from the real data)
        onView(withText("Condemn a US Takeover of Gaza")).check(matches(isDisplayed()));

        // Wait a bit longer to ensure the title is fully loaded
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check that the toolbar is displayed
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));

        // Check that the collapsing toolbar is displayed and contains the location text
        onView(withId(R.id.collapsing_toolbar))
            .check(matches(isDisplayed()))
            .check(matches(withCollapsingToolbarTitle(containsString("BOWLING GREEN"))));
    }

    @Test
    public void testNavigationDrawerOpens() throws JSONException {
        // Set up mock responses
        setupMockResponses();

        // Set up mock request queue
        setupMockRequestQueue();

        // Launch the activity
        launchMainActivity();

        // Verify that the drawer layout is displayed
        onView(withId(R.id.drawer_layout)).check(matches(isDisplayed()));

        // Verify that the toolbar is displayed
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));

        // Open the drawer using the activity's drawer layout directly
        scenario.onActivity(activity -> {
            // Find the drawer layout by ID
            DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
            // Open the drawer
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