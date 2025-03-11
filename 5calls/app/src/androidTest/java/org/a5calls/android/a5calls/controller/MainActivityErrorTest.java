package org.a5calls.android.a5calls.controller;

import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
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
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * Integration test for MainActivity that tests error handling.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityErrorTest {

    private MockHttpStack mHttpStack;
    private RequestQueue mOriginalRequestQueue;
    private FiveCallsApi mOriginalApi;
    private String mOriginalAddress;
    private ActivityScenario<MainActivity> scenario;

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
     * @param waitTimeMs time to wait for the activity to load and process errors
     */
    private void launchMainActivity(int waitTimeMs) {
        // Launch the activity
        scenario = ActivityScenario.launch(MainActivity.class);

        // Wait for error processing and UI to update
        try {
            Thread.sleep(waitTimeMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Custom matcher to check if a RecyclerView has exactly one item
     */
    public static Matcher<View> hasExactlyOneItem() {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View view) {
                if (!(view instanceof RecyclerView)) {
                    return false;
                }
                RecyclerView recyclerView = (RecyclerView) view;
                return recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() == 1;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("RecyclerView with exactly one item");
            }
        };
    }

    /**
     * Verifies that error UI is displayed correctly
     */
    private void verifyErrorUI() {
        // Check that the error message is displayed
        onView(withText(R.string.request_error))
                .check(matches(isDisplayed()));

        // Verify that the RecyclerView is displayed and has at least one item
        onView(withId(R.id.issues_recycler_view))
                .check(matches(isDisplayed()))
                .check(matches(hasExactlyOneItem()));
    }

    @Test
    public void testNetworkErrorDisplaysSnackbar() {
        // Set up mock to throw network error
        mHttpStack.setExceptionToThrow(new IOException("Network error"));

        // Set up mock request queue
        setupMockRequestQueue();

        // Launch the activity and wait for error processing
        // TODO: rather than hard-coding the wait time, we should wait for the error to be processed
        launchMainActivity(3000);

        // Verify error UI is displayed
        verifyErrorUI();
    }

    @Test
    public void testJsonErrorDisplaysSnackbar() {
        // Set up mock to return malformed JSON
        HttpResponse response = new HttpResponse(200, new ArrayList<>(), "Not valid JSON".getBytes());
        mHttpStack.setResponseToReturn(response);

        // Set up mock request queue
        setupMockRequestQueue();

        // Launch the activity and wait for error processing
        launchMainActivity(1000);

        // Verify error UI is displayed
        verifyErrorUI();
    }
}