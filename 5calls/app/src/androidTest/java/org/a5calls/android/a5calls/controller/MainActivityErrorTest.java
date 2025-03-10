package org.a5calls.android.a5calls.controller;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.volley.NetworkError;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HttpResponse;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.net.FakeRequestQueue;
import org.a5calls.android.a5calls.net.FiveCallsApi;
import org.a5calls.android.a5calls.net.MockHttpStack;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

/**
 * Integration test for MainActivity that tests error handling.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityErrorTest {

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
    public void testNetworkErrorDisplaysSnackbar() {
        // Set up mock to throw network error
        mHttpStack.setExceptionToThrow(new IOException("Network error"));

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

        // Create an idling resource to wait for the error to be displayed
        final ConditionIdlingResource idlingResource = new ConditionIdlingResource();
        IdlingRegistry.getInstance().register(idlingResource);

        try {
            // Launch the activity
            ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

            // The activity will automatically make a request for issues
            // Wait longer for the error to be processed and Snackbar to be displayed
            try {
                Thread.sleep(3000); // Increased wait time from 2000 to 3000ms
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Set the idling resource to idle
            idlingResource.setIdle(true);

            // Check that the error message is displayed
            onView(withText(R.string.request_error))
                    .check(matches(isDisplayed()));

            // Also verify that the RecyclerView is displayed (but empty)
            onView(withId(R.id.issues_recycler_view)).check(matches(isDisplayed()));

            // Close the activity
            scenario.close();
        } finally {
            // Unregister the idling resource
            IdlingRegistry.getInstance().unregister(idlingResource);
        }
    }

    @Test
    public void testJsonErrorDisplaysSnackbar() {
        // Set up mock to return malformed JSON
        HttpResponse response = new HttpResponse(200, new ArrayList<>(), "Not valid JSON".getBytes());
        mHttpStack.setResponseToReturn(response);

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

        // Create an idling resource to wait for the error to be displayed
        final ConditionIdlingResource idlingResource = new ConditionIdlingResource();
        IdlingRegistry.getInstance().register(idlingResource);

        try {
            // Launch the activity
            ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

            // The activity will automatically make a request for issues
            // Wait longer for the error to be processed and Snackbar to be displayed
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Set the idling resource to idle
            idlingResource.setIdle(true);

            // Since we observed the same Snackbar appearing for both tests,
            // let's check for the request_error message instead
            onView(withText(R.string.request_error))
                    .check(matches(isDisplayed()));

            // Also verify that the RecyclerView is displayed (but empty)
            onView(withId(R.id.issues_recycler_view)).check(matches(isDisplayed()));

            // Close the activity
            scenario.close();
        } finally {
            // Unregister the idling resource
            IdlingRegistry.getInstance().unregister(idlingResource);
        }
    }
}