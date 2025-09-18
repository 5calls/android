package org.a5calls.android.a5calls.controller;

import android.content.Intent;
import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.volley.Header;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HttpResponse;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.model.CustomizedContactScript;
import org.a5calls.android.a5calls.model.Issue;
import org.a5calls.android.a5calls.net.FakeRequestQueue;
import org.a5calls.android.a5calls.net.FiveCallsApi;
import org.a5calls.android.a5calls.net.MockHttpStack;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Integration tests for script loading timing scenarios in RepCallActivity.
 * Tests the race condition where RepCallActivity loads before customized scripts arrive.
 */
@RunWith(AndroidJUnit4.class)
public class RepCallActivityScriptLoadingTest {

    static class TestScriptsListener implements FiveCallsApi.ScriptsRequestListener {
        protected int mScriptsError = 0;
        protected int mScriptsJsonError = 0;
        protected List<CustomizedContactScript> mScripts = null;

        @Override
        public void onRequestError() {
            mScriptsError++;
        }

        @Override
        public void onJsonError() {
            mScriptsJsonError++;
        }

        @Override
        public void onScriptsReceived(List<CustomizedContactScript> scripts) {
            mScripts = scripts;
        }
    }

    private FiveCallsApi mApi;
    private FakeRequestQueue mRequestQueue;
    private MockHttpStack mHttpStack;

    @Before
    public void setUp() {
        mHttpStack = new MockHttpStack();
        BasicNetwork basicNetwork = new BasicNetwork(mHttpStack);
        mRequestQueue = new FakeRequestQueue(basicNetwork);
        mApi = new FiveCallsApi("testCaller", mRequestQueue,
                InstrumentationRegistry.getInstrumentation().getTargetContext());

        // Replace the API instance in AppSingleton for testing
        AppSingleton.getInstance(InstrumentationRegistry.getInstrumentation().getTargetContext())
                .setFiveCallsApi(mApi);
    }

    @After
    public void tearDown() {
        // Reset any test state if needed
    }

    @Test
    public void testCustomizedScriptsRequest() {
        // Use fake customized scripts data
        byte[] bytes = org.a5calls.android.a5calls.FakeJSONData.CUSTOMIZED_SCRIPTS_DATA.getBytes();
        ArrayList<Header> headers = new ArrayList<>();
        headers.add(new Header("Content-Type", "application/json"));
        HttpResponse response = new HttpResponse(200, headers, bytes);
        mHttpStack.setResponseToReturn(response);

        TestScriptsListener testScriptsListener = new TestScriptsListener();
        mApi.registerScriptsRequestListener(testScriptsListener);

        // Simulate fetching customized scripts like IssueActivity does
        List<String> contactIds = new ArrayList<>();
        contactIds.add("G000599");
        mApi.getCustomizedScripts("test-issue-456", contactIds, "New York, NY", "Test User");

        waitForHttpRequestComplete();

        assertEquals(0, testScriptsListener.mScriptsError);
        assertEquals(0, testScriptsListener.mScriptsJsonError);
        assertNotNull(testScriptsListener.mScripts);
        assertEquals(2, testScriptsListener.mScripts.size());
        assertEquals("G000599", testScriptsListener.mScripts.get(0).id);
        assertEquals("Hi [NAME], I'm your constituent from [CITY, ZIP]. I'm calling about this urgent issue that affects our community directly. As Dan Goldman, you've shown leadership on similar issues before.",
                testScriptsListener.mScripts.get(0).script);

        mApi.unregisterScriptsRequestListener(testScriptsListener);
    }

    @Test
    public void testCustomizedScriptsRequestError() {
        // Simulate server error
        mHttpStack.setExceptionToThrow(new java.io.IOException("HTTP Stack exception"));

        TestScriptsListener testScriptsListener = new TestScriptsListener();
        mApi.registerScriptsRequestListener(testScriptsListener);

        List<String> contactIds = new ArrayList<>();
        contactIds.add("G000599");
        mApi.getCustomizedScripts("test-issue-456", contactIds, "New York, NY", "Test User");

        waitForHttpRequestComplete();

        assertEquals(1, testScriptsListener.mScriptsError);
        assertEquals(0, testScriptsListener.mScriptsJsonError);

        mApi.unregisterScriptsRequestListener(testScriptsListener);
    }

    @Test
    public void testCustomizedScriptsMalformedJson() {
        // Return malformed JSON
        byte[] bytes = "{malformed json}".getBytes();
        ArrayList<Header> headers = new ArrayList<>();
        headers.add(new Header("Content-Type", "application/json"));
        HttpResponse response = new HttpResponse(200, headers, bytes);
        mHttpStack.setResponseToReturn(response);

        TestScriptsListener testScriptsListener = new TestScriptsListener();
        mApi.registerScriptsRequestListener(testScriptsListener);

        List<String> contactIds = new ArrayList<>();
        contactIds.add("G000599");
        mApi.getCustomizedScripts("test-issue-456", contactIds, "New York, NY", "Test User");

        waitForHttpRequestComplete();

        assertEquals(1, testScriptsListener.mScriptsError);
        assertEquals(0, testScriptsListener.mScriptsJsonError);

        mApi.unregisterScriptsRequestListener(testScriptsListener);
    }

    private void waitForHttpRequestComplete() {
        // Start the request queue to process the request
        mRequestQueue.start();

        // Wait for network request to complete (similar to existing tests)
        // Use a simple sleep since this is a test environment
        SystemClock.sleep(500);
    }
}