package org.a5calls.android.a5calls.controller;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasData;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.volley.toolbox.HttpResponse;

import org.a5calls.android.a5calls.R;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Instrumentation test for AboutActivity.
 */
public class AboutActivityTest extends MainActivityBaseTest {

    private ActivityScenario<AboutActivity> aboutScenario;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        setupMockRequestQueue();
        Intents.init();
    }

    @After
    @Override
    public void tearDown() {
        if (aboutScenario != null) {
            aboutScenario.close();
        }
        Intents.release();
        super.tearDown();
    }

    @Test
    public void testCheckRegistrationButton_launchesIntent() {
        // Mock the report response to avoid errors in AboutActivity
        mHttpStack.setResponseToReturn(new HttpResponse(200, new ArrayList<>(), "{\"count\": 100}".getBytes()));

        aboutScenario = ActivityScenario.launch(AboutActivity.class);

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String expectedUrl = context.getString(R.string.check_your_registration_url);

        // Prepare a stub result so the intent is intercepted and blocked from launching
        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(Activity.RESULT_OK, new Intent());
        intending(hasAction(Intent.ACTION_VIEW)).respondWith(result);

        // The button might be inside a ScrollView, so we use scrollTo()
        onView(withId(R.id.check_registration_button)).perform(scrollTo(), click());

        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(Uri.parse(expectedUrl))));
    }
}
