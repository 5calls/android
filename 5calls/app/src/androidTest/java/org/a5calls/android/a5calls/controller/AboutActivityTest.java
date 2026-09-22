package org.a5calls.android.a5calls.controller;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasData;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.a5calls.android.a5calls.FakeJSONData.REPORT_DATA;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

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
        mHttpStack.setResponseToReturn(new HttpResponse(200, new ArrayList<>(), REPORT_DATA.getBytes()));

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

    @Test
    public void testCallsToday_calculatesTotalSinceLocalMidnight() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long localMidnightSec = calendar.getTimeInMillis() / 1000;

        // Create hourly call count entries:
        // 1. 1 hour before local midnight -> should be excluded (500 calls)
        // 2. At local midnight -> should be included (150 calls)
        // 3. 1 hour after local midnight -> should be included (100 calls)
        // Total today count expected = 150 + 100 = 250
        long beforeMidnight = localMidnightSec - 3600;
        long afterMidnight = localMidnightSec + 3600;

        String reportJson = String.format(Locale.US,
                "{\"count\":10000,\"donateOn\":false,\"hourlyCalls\":[" +
                        "{\"time\":%d,\"count\":500}," +
                        "{\"time\":%d,\"count\":150}," +
                        "{\"time\":%d,\"count\":100}" +
                        "]}",
                beforeMidnight, localMidnightSec, afterMidnight);

        mHttpStack.setResponseToReturn(new HttpResponse(200, new ArrayList<>(), reportJson.getBytes()));

        aboutScenario = ActivityScenario.launch(AboutActivity.class);

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String expectedCallsTodayText = String.format(
                context.getString(R.string.calls_today),
                NumberFormat.getNumberInstance(Locale.getDefault()).format(250));

        onView(withId(R.id.calls_today))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
                .check(matches(withText(expectedCallsTodayText)));
    }

    @Test
    public void testCallsToday_hiddenWhenCountBelowThreshold() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long localMidnightSec = calendar.getTimeInMillis() / 1000;

        // Only 50 calls after local midnight (below MIN_CALLS_TO_SHOW threshold of 200)
        long afterMidnight = localMidnightSec + 3600;

        String reportJson = String.format(Locale.US,
                "{\"count\":10000,\"donateOn\":false,\"hourlyCalls\":[" +
                        "{\"time\":%d,\"count\":50}" +
                        "]}",
                afterMidnight);

        mHttpStack.setResponseToReturn(new HttpResponse(200, new ArrayList<>(), reportJson.getBytes()));

        aboutScenario = ActivityScenario.launch(AboutActivity.class);

        onView(withId(R.id.calls_today)).check(matches(not(isDisplayed())));
    }
}
