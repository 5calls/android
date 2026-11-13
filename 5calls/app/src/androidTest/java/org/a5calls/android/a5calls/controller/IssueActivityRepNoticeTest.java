package org.a5calls.android.a5calls.controller;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.volley.toolbox.HttpResponse;

import org.a5calls.android.a5calls.FakeJSONData;
import org.a5calls.android.a5calls.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Checks the server's notice shows above the reps on the issue screen. Also saves a screenshot
 * of each state to the app's external files dir (screenshots/) so the UI can be reviewed:
 * adb pull /sdcard/Android/data/org.a5calls.android.a5calls/files/screenshots
 */
@RunWith(AndroidJUnit4.class)
public class IssueActivityRepNoticeTest extends MainActivityBaseTest {
    private static final String TAG = "RepNoticeTest";
    private static final String ISSUE_NAME = "Condemn a US Takeover of Gaza";
    private static final String NOTICE_TEXT = "New members of Congress take office in January 2027. "
            + "Until then, these are your current representatives.";

    private void setupMockResponses(boolean withNotice) throws JSONException {
        mHttpStack.clearUrlPatternResponses();
        // The fixture predates permalinks, which IssueActivity needs for analytics.
        JSONArray issues = FakeJSONData.getIssueJSON();
        for (int i = 0; i < issues.length(); i++) {
            JSONObject issue = issues.getJSONObject(i);
            if (!issue.has("permalink")) {
                issue.put("permalink", "/issue/" + issue.optString("slug", "test-" + i) + "/");
            }
        }
        mHttpStack.setResponseForUrlPattern("issues", new HttpResponse(200, new ArrayList<>(),
                issues.toString().getBytes()));

        JSONObject reps = FakeJSONData.getRepsJSON(/*isSplit=*/false);
        if (withNotice) {
            reps.put("notice", new JSONObject()
                    .put("id", 1)
                    .put("text", NOTICE_TEXT)
                    .put("expiresAt", "2099-01-04T00:00:00Z"));
        }
        mHttpStack.setResponseForUrlPattern("reps", new HttpResponse(200, new ArrayList<>(),
                reps.toString().getBytes()));
        mHttpStack.setResponseForUrlPattern("report", new HttpResponse(200, new ArrayList<>(),
                FakeJSONData.getReportJSON().toString().getBytes()));
        mHttpStack.setResponseToReturn(new HttpResponse(200, new ArrayList<>(), "{}".getBytes()));
    }

    private void openIssueAndExpandReps() throws InterruptedException {
        launchMainActivity(1500);
        onView(withText(ISSUE_NAME)).perform(click());
        Thread.sleep(1000);
        // Expand the "contacts to call" sheet so the reps are on screen.
        onView(withId(R.id.rep_prompt)).perform(click());
        Thread.sleep(1000);
    }

    @Test
    public void testNoticeShownAboveReps() throws Exception {
        setupMockResponses(/*withNotice=*/true);
        setupMockRequestQueue();

        openIssueAndExpandReps();

        onView(withId(R.id.rep_notice)).check(matches(isDisplayed()));
        onView(withId(R.id.rep_notice_text)).check(matches(withText(NOTICE_TEXT)));
        saveScreenshot("rep-notice-shown");
    }

    @Test
    public void testNoNoticeWhenServerSendsNone() throws Exception {
        setupMockResponses(/*withNotice=*/false);
        setupMockRequestQueue();

        openIssueAndExpandReps();

        onView(withId(R.id.rep_notice)).check(matches(
                withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        saveScreenshot("rep-notice-hidden");
    }

    private void saveScreenshot(String name) {
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .takeScreenshot();
        if (bitmap == null) {
            Log.w(TAG, "couldn't take screenshot " + name);
            return;
        }
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File dir = new File(context.getExternalFilesDir(null), "screenshots");
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "couldn't create " + dir);
            return;
        }
        File file = new File(dir, name + ".png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            Log.i(TAG, "saved screenshot " + file);
        } catch (IOException e) {
            Log.w(TAG, "couldn't save screenshot " + name, e);
        }
    }
}
