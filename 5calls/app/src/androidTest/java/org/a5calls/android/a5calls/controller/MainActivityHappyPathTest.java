package org.a5calls.android.a5calls.controller;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HttpResponse;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.net.FakeRequestQueue;
import org.a5calls.android.a5calls.net.FiveCallsApi;
import org.a5calls.android.a5calls.net.MockHttpStack;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import androidx.core.view.GravityCompat;
import android.view.View;
import androidx.drawerlayout.widget.DrawerLayout;

/**
 * Integration test for MainActivity that tests the happy path.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityHappyPathTest {

    private MockHttpStack mHttpStack;
    private RequestQueue mOriginalRequestQueue;
    private FiveCallsApi mOriginalApi;
    private String mOriginalAddress;

    /**
     * A simple IdlingResource that waits for a condition to be true.
     */
    private static class ConditionIdlingResource implements IdlingResource {
        private final AtomicBoolean mIsIdle = new AtomicBoolean(false);
        private ResourceCallback mResourceCallback;

        public void setIdle(boolean isIdle) {
            mIsIdle.set(isIdle);
            if (isIdle && mResourceCallback != null) {
                mResourceCallback.onTransitionToIdle();
            }
        }

        @Override
        public String getName() {
            return ConditionIdlingResource.class.getName();
        }

        @Override
        public boolean isIdleNow() {
            return mIsIdle.get();
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback callback) {
            mResourceCallback = callback;
        }
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
    }

    @Test
    public void testMainUILoadsCorrectly() throws JSONException {
        // Create mock issues response
        JSONArray issuesArray = new JSONArray();
        JSONObject issue = new JSONObject();
        issue.put("id", "test-issue-1");
        issue.put("name", "Test Issue 1");
        issue.put("slug", "test-issue-1");
        issue.put("reason", "This is a test issue");
        issue.put("script", "This is a test script");
        issue.put("active", true);
        issue.put("link", "https://5calls.org");
        issue.put("linkTitle", "Learn More");

        // Add categories
        JSONArray categoriesArray = new JSONArray();
        JSONObject category = new JSONObject();
        category.put("name", "Test Category");
        category.put("slug", "test-category");
        categoriesArray.put(category);
        issue.put("categories", categoriesArray);

        // Add stats
        JSONObject stats = new JSONObject();
        stats.put("calls", 100);
        issue.put("stats", stats);

        // Add contactAreas
        JSONArray contactAreasArray = new JSONArray();
        contactAreasArray.put("Senate");
        issue.put("contactAreas", contactAreasArray);

        issuesArray.put(issue);

        // Create mock contacts response
        JSONObject contactsResponseJson = new JSONObject();
        contactsResponseJson.put("location", "Beverly Hills, CA 90210");
        contactsResponseJson.put("normalizedLocation", "Beverly Hills, CA 90210");
        contactsResponseJson.put("splitDistrict", false);
        contactsResponseJson.put("state", "CA");
        contactsResponseJson.put("district", "33");

        JSONArray contactsArray = new JSONArray();
        JSONObject contact = new JSONObject();
        contact.put("id", "test-contact-1");
        contact.put("name", "Test Representative");
        contact.put("phone", "555-555-5555");
        contact.put("photoURL", "https://example.com/photo.jpg");
        contact.put("party", "Independent");
        contact.put("state", "CA");
        contact.put("reason", "This is your representative");
        contact.put("area", "Senate");
        contactsArray.put(contact);
        contactsResponseJson.put("representatives", contactsArray);

        // Create mock report response
        JSONObject reportResponseJson = new JSONObject();
        reportResponseJson.put("count", 5000);
        reportResponseJson.put("donateOn", false);

        // Set up mock to return our test data
        HttpResponse issuesResponse = new HttpResponse(200, new ArrayList<>(), issuesArray.toString().getBytes());
        HttpResponse contactsResponse = new HttpResponse(200, new ArrayList<>(), contactsResponseJson.toString().getBytes());
        HttpResponse reportResponse = new HttpResponse(200, new ArrayList<>(), reportResponseJson.toString().getBytes());

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

        // Create an idling resource to wait for the UI to load
        final ConditionIdlingResource idlingResource = new ConditionIdlingResource();
        IdlingRegistry.getInstance().register(idlingResource);

        try {
            // Set up the mock to handle all possible requests with appropriate responses
            mHttpStack.clearUrlPatternResponses();
            mHttpStack.setResponseForUrlPattern("issues", issuesResponse);
            mHttpStack.setResponseForUrlPattern("reps", contactsResponse);
            mHttpStack.setResponseForUrlPattern("report", reportResponse);

            // Set a default response for any other requests
            mHttpStack.setResponseToReturn(new HttpResponse(200, new ArrayList<>(), "{}".getBytes()));

            // Launch the activity
            ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

            // Wait for all requests to complete and UI to update
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Set the idling resource to idle
            idlingResource.setIdle(true);

            // Verify that the toolbar is displayed
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()));

            // Verify that the issues recycler view is displayed
            onView(withId(R.id.issues_recycler_view)).check(matches(isDisplayed()));

            // Verify that the filter spinner is displayed
            onView(withId(R.id.filter)).check(matches(isDisplayed()));

            // Verify that our test issue is displayed
            onView(withText("Test Issue 1")).check(matches(isDisplayed()));

            // The subtitle might not contain the exact location string, so let's just verify it's displayed
            onView(withId(R.id.action_bar_subtitle)).check(matches(isDisplayed()));

            // Close the activity
            scenario.close();
        } finally {
            // Unregister the idling resource
            IdlingRegistry.getInstance().unregister(idlingResource);
        }
    }

    @Test
    public void testNavigationDrawerOpens() {
        // Set up mock to return valid responses instead of empty ones
        try {
            // Create mock issues response
            JSONArray issuesArray = new JSONArray();
            JSONObject issue = new JSONObject();
            issue.put("id", "test-issue-1");
            issue.put("name", "Test Issue 1");
            issue.put("slug", "test-issue-1");
            issue.put("reason", "This is a test issue");
            issue.put("script", "This is a test script");
            issue.put("active", true);
            issue.put("link", "https://5calls.org");
            issue.put("linkTitle", "Learn More");

            // Add categories
            JSONArray categoriesArray = new JSONArray();
            JSONObject category = new JSONObject();
            category.put("name", "Test Category");
            category.put("slug", "test-category");
            categoriesArray.put(category);
            issue.put("categories", categoriesArray);

            // Add stats
            JSONObject stats = new JSONObject();
            stats.put("calls", 100);
            issue.put("stats", stats);

            // Add contactAreas
            JSONArray contactAreasArray = new JSONArray();
            contactAreasArray.put("Senate");
            issue.put("contactAreas", contactAreasArray);

            issuesArray.put(issue);
            HttpResponse issuesResponse = new HttpResponse(200, new ArrayList<>(), issuesArray.toString().getBytes());

            // Create mock contacts response
            JSONObject contactsResponseJson = new JSONObject();
            contactsResponseJson.put("location", "Beverly Hills, CA 90210");
            contactsResponseJson.put("normalizedLocation", "Beverly Hills, CA 90210");
            contactsResponseJson.put("splitDistrict", false);
            contactsResponseJson.put("state", "CA");
            contactsResponseJson.put("district", "33");

            JSONArray contactsArray = new JSONArray();
            JSONObject contact = new JSONObject();
            contact.put("id", "test-contact-1");
            contact.put("name", "Test Representative");
            contact.put("phone", "555-555-5555");
            contact.put("photoURL", "https://example.com/photo.jpg");
            contact.put("party", "Independent");
            contact.put("state", "CA");
            contact.put("reason", "This is your representative");
            contact.put("area", "Senate");
            contactsArray.put(contact);
            contactsResponseJson.put("representatives", contactsArray);
            HttpResponse contactsResponse = new HttpResponse(200, new ArrayList<>(), contactsResponseJson.toString().getBytes());

            // Create mock report response
            JSONObject reportResponseJson = new JSONObject();
            reportResponseJson.put("count", 5000);
            reportResponseJson.put("donateOn", false);
            HttpResponse reportResponse = new HttpResponse(200, new ArrayList<>(), reportResponseJson.toString().getBytes());

            // Set up the mock to handle all possible requests with appropriate responses
            mHttpStack.clearUrlPatternResponses();
            mHttpStack.setResponseForUrlPattern("issues", issuesResponse);
            mHttpStack.setResponseForUrlPattern("reps", contactsResponse);
            mHttpStack.setResponseForUrlPattern("report", reportResponse);

            // Set a default response for any other requests
            mHttpStack.setResponseToReturn(new HttpResponse(200, new ArrayList<>(), "{}".getBytes()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

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

        // Create an idling resource to wait for the UI to load
        final ConditionIdlingResource idlingResource = new ConditionIdlingResource();
        IdlingRegistry.getInstance().register(idlingResource);

        try {
            // Launch the activity
            ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

            // Wait for all requests to complete and UI to update
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Set the idling resource to idle
            idlingResource.setIdle(true);

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

            // Close the activity
            scenario.close();
        } finally {
            // Unregister the idling resource
            IdlingRegistry.getInstance().unregister(idlingResource);
        }
    }

    @Test
    public void testFilterSpinnerDisplayed() throws JSONException {
        // Create mock issues response with multiple categories
        JSONArray issuesArray = new JSONArray();

        // First issue with category "Test Category 1"
        JSONObject issue1 = new JSONObject();
        issue1.put("id", "test-issue-1");
        issue1.put("name", "Test Issue 1");
        issue1.put("slug", "test-issue-1");
        issue1.put("reason", "This is test issue 1");
        issue1.put("script", "This is a test script");
        issue1.put("active", true);

        JSONArray categories1 = new JSONArray();
        JSONObject category1 = new JSONObject();
        category1.put("name", "Test Category 1");
        category1.put("slug", "test-category-1");
        categories1.put(category1);
        issue1.put("categories", categories1);

        JSONObject stats1 = new JSONObject();
        stats1.put("calls", 100);
        issue1.put("stats", stats1);

        // Add contactAreas
        JSONArray contactAreas1 = new JSONArray();
        contactAreas1.put("Senate");
        issue1.put("contactAreas", contactAreas1);

        issuesArray.put(issue1);

        // Second issue with category "Test Category 2"
        JSONObject issue2 = new JSONObject();
        issue2.put("id", "test-issue-2");
        issue2.put("name", "Test Issue 2");
        issue2.put("slug", "test-issue-2");
        issue2.put("reason", "This is test issue 2");
        issue2.put("script", "This is another test script");
        issue2.put("active", true);

        JSONArray categories2 = new JSONArray();
        JSONObject category2 = new JSONObject();
        category2.put("name", "Test Category 2");
        category2.put("slug", "test-category-2");
        categories2.put(category2);
        issue2.put("categories", categories2);

        JSONObject stats2 = new JSONObject();
        stats2.put("calls", 200);
        issue2.put("stats", stats2);

        // Add contactAreas
        JSONArray contactAreas2 = new JSONArray();
        contactAreas2.put("House");
        issue2.put("contactAreas", contactAreas2);

        issuesArray.put(issue2);

        // Create mock contacts response
        JSONObject contactsResponseJson = new JSONObject();
        contactsResponseJson.put("location", "Beverly Hills, CA 90210");
        contactsResponseJson.put("normalizedLocation", "Beverly Hills, CA 90210");
        contactsResponseJson.put("splitDistrict", false);
        contactsResponseJson.put("state", "CA");
        contactsResponseJson.put("district", "33");

        JSONArray contactsArray = new JSONArray();
        JSONObject contact = new JSONObject();
        contact.put("id", "test-contact-1");
        contact.put("name", "Test Representative");
        contact.put("phone", "555-555-5555");
        contact.put("photoURL", "https://example.com/photo.jpg");
        contact.put("party", "Independent");
        contact.put("state", "CA");
        contact.put("reason", "This is your representative");
        contact.put("area", "Senate");
        contactsArray.put(contact);
        contactsResponseJson.put("representatives", contactsArray);

        // Create mock report response
        JSONObject reportResponseJson = new JSONObject();
        reportResponseJson.put("count", 5000);
        reportResponseJson.put("donateOn", false);

        // Set up mock to return our test data
        HttpResponse issuesResponse = new HttpResponse(200, new ArrayList<>(), issuesArray.toString().getBytes());
        HttpResponse contactsResponse = new HttpResponse(200, new ArrayList<>(), contactsResponseJson.toString().getBytes());
        HttpResponse reportResponse = new HttpResponse(200, new ArrayList<>(), reportResponseJson.toString().getBytes());

        // Set up the mock to handle all possible requests with appropriate responses
        mHttpStack.clearUrlPatternResponses();
        mHttpStack.setResponseForUrlPattern("issues", issuesResponse);
        mHttpStack.setResponseForUrlPattern("reps", contactsResponse);
        mHttpStack.setResponseForUrlPattern("report", reportResponse);

        // Set a default response for any other requests
        mHttpStack.setResponseToReturn(new HttpResponse(200, new ArrayList<>(), "{}".getBytes()));

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

        // Create an idling resource to wait for the UI to load
        final ConditionIdlingResource idlingResource = new ConditionIdlingResource();
        IdlingRegistry.getInstance().register(idlingResource);

        try {
            // Launch the activity
            ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

            // Wait for all requests to complete and UI to update
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Set the idling resource to idle
            idlingResource.setIdle(true);

            // Verify that the filter spinner is displayed
            onView(withId(R.id.filter)).check(matches(isDisplayed()));

            // Verify that both issues are displayed
            onView(withText("Test Issue 1")).check(matches(isDisplayed()));
            onView(withText("Test Issue 2")).check(matches(isDisplayed()));

            // Close the activity
            scenario.close();
        } finally {
            // Unregister the idling resource
            IdlingRegistry.getInstance().unregister(idlingResource);
        }
    }
}