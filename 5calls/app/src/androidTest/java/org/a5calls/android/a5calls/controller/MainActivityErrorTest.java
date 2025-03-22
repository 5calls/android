package org.a5calls.android.a5calls.controller;

import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
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
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

/**
 * Integration test for MainActivity that tests error handling.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityErrorTest extends MainActivityBaseTest {

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
     * Verifies that error UI is displayed correctly.
     */
    private void verifyErrorUI() {
        // Check that the error message is displayed in the RecyclerView.
        onView(allOf(withText(R.string.request_error), isDescendantOfA(withId(R.id.issues_recycler_view))))
                .check(matches(isDisplayed()));

        // And in a snackbar.
        onView(allOf(withText(R.string.request_error), withId(com.google.android.material.R.id.snackbar_text)))
                .check(matches(isDisplayed()));

        // Verify that the RecyclerView is displayed and has at least one item
        onView(withId(R.id.issues_recycler_view))
                .check(matches(isDisplayed()))
                .check(matches(hasExactlyOneItem()));
    }

    @Test
    public void testNetworkErrorDisplaysError() {
        // Set up mock to throw network error
        mHttpStack.setExceptionToThrow(new IOException("Network error"));

        setupMockRequestQueue();

        // TODO: rather than hard-coding the wait time, we should wait for the error to be processed
        launchMainActivity(1000);

        verifyErrorUI();
    }

    @Test
    public void testJsonErrorDisplaysError() {
        // Set up mock to return malformed JSON
        HttpResponse response = new HttpResponse(200, new ArrayList<>(), "Not valid JSON".getBytes());
        mHttpStack.setResponseToReturn(response);

        setupMockRequestQueue();

        launchMainActivity(1000);

        verifyErrorUI();
    }
}