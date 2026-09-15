package org.a5calls.android.a5calls;

import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.util.Locale;

import com.android.volley.toolbox.BasicNetwork;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.net.FakeRequestQueue;
import org.a5calls.android.a5calls.net.FiveCallsApi;
import org.a5calls.android.a5calls.net.MockHttpStack;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

/**
 * Base class for all instrumentation tests in the app.
 * Handles common setup for mocking network requests and accessing context.
 */
@RunWith(AndroidJUnit4.class)
public abstract class BaseIntegrationTest {

    protected Context mContext;
    protected MockHttpStack mHttpStack;
    protected FakeRequestQueue mRequestQueue;
    protected FiveCallsApi mApi;
    protected Locale mLocale;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mLocale = Locale.getDefault();

        // Use a fixed caller ID for consistent test results
        AccountManager.Instance.setCallerID(mContext, "itMe");

        mHttpStack = new MockHttpStack();
        BasicNetwork basicNetwork = new BasicNetwork(mHttpStack);
        mRequestQueue = new FakeRequestQueue(basicNetwork);

        mApi = new FiveCallsApi("itMe", mRequestQueue, mContext);
    }

    @After
    public void tearDown() {
        if (mRequestQueue != null) {
            mRequestQueue.mRequest = null;
        }
    }

    /**
     * Helper to wait for asynchronous network requests to complete in tests.
     */
    protected void waitForHttpRequestComplete() {
        assertNotNull(mRequestQueue.mRequest);
        mRequestQueue.start();

        // Wait for the async stuff.
        // TODO: Use a more robust synchronization mechanism like IdlingResource
        SystemClock.sleep(200);
    }

    /**
     * Custom matcher to check if a RecyclerView has exactly one item
     */
    public static Matcher<View> hasExactlyOneItem() {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(View view) {
                if (!(view instanceof RecyclerView recyclerView)) {
                    return false;
                }
                return recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() == 1;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("RecyclerView with exactly one item");
            }
        };
    }

    // Custom matcher that matches only the first view matching the given matcher.
    public static Matcher<View> first(final Matcher<View> matcher) {
        return new TypeSafeMatcher<>() {
            boolean matched = false;

            @Override
            public boolean matchesSafely(View view) {
                if (matched) {
                    return false;
                }
                if (matcher.matches(view)) {
                    matched = true;
                    return true;
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("first view matching: ");
                matcher.describeTo(description);
            }
        };
    }

    // Custom matcher to check if a CollapsingToolbarLayout's title contains specific text
    public static Matcher<View> withCollapsingToolbarTitle(final Matcher<String> textMatcher) {
        return new TypeSafeMatcher<>() {
            @Override
            public boolean matchesSafely(View view) {
                if (!(view instanceof CollapsingToolbarLayout toolbarLayout)) {
                    return false;
                }
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
     * A custom click action that only requires the view to be displayed,
     * bypassing the 90% visibility constraint.
     */
    public static ViewAction clickVisible() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return allOf(isDisplayed(), isClickable());
            }

            @Override
            public String getDescription() {
                return "click visible view";
            }

            @Override
            public void perform(UiController uiController, View view) {
                view.performClick();
            }
        };
    }

}
