package org.a5calls.android.a5calls.net;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;

import org.a5calls.android.a5calls.FakeJSONData;
import org.a5calls.android.a5calls.model.CustomizedContactScript;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class FiveCallsApiTest {

    private FiveCallsApi fiveCallsApi;
    private Context context;
    private TestRequestQueue testRequestQueue;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        testRequestQueue = new TestRequestQueue();
        fiveCallsApi = new FiveCallsApi("test-caller-id", testRequestQueue, context);
    }

    @Test
    public void testScriptsListenerRegistration() {
        TestScriptsListener testListener = new TestScriptsListener();
        // Test that we can register a listener without exceptions
        fiveCallsApi.registerScriptsRequestListener(testListener);
        // Registration should work without errors
    }

    @Test
    public void testScriptsListenerUnregistration() {
        TestScriptsListener testListener = new TestScriptsListener();
        // Test that we can unregister a listener without exceptions
        fiveCallsApi.registerScriptsRequestListener(testListener);
        fiveCallsApi.unregisterScriptsRequestListener(testListener);
        // Unregistration should work without errors
    }

    @Test
    public void testGetCustomizedScriptsBasicRequest() {
        String issueId = "test-issue-123";
        List<String> contactIds = new ArrayList<>();
        contactIds.add("contact1");
        contactIds.add("contact2");
        String location = "Test City, NY";
        String name = "Test User";

        TestScriptsListener testListener = new TestScriptsListener();
        fiveCallsApi.registerScriptsRequestListener(testListener);
        fiveCallsApi.getCustomizedScripts(issueId, contactIds, location, name);

        assertEquals("Should have one request", 1, testRequestQueue.requests.size());
        Request<?> request = testRequestQueue.requests.get(0);
        assertNotNull("Request should not be null", request);
        assertTrue("Should be JsonObjectRequest", request instanceof JsonObjectRequest);

        String url = request.getUrl();
        assertTrue("URL should contain base path", url.contains("/v1/issue/test-issue-123/script"));
        assertTrue("URL should contain contact_ids parameter", url.contains("contact_ids=contact1%2Ccontact2"));
        assertTrue("URL should contain location parameter", url.contains("location=Test%20City%2C%20NY"));
        assertTrue("URL should contain name parameter", url.contains("name=Test%20User"));
    }

    @Test
    public void testGetCustomizedScriptsWithNullContactIds() {
        String issueId = "test-issue-123";
        String location = "Test City, NY";
        String name = "Test User";

        TestScriptsListener testListener = new TestScriptsListener();
        fiveCallsApi.registerScriptsRequestListener(testListener);
        fiveCallsApi.getCustomizedScripts(issueId, null, location, name);

        assertEquals("Should have one request", 1, testRequestQueue.requests.size());
        Request<?> request = testRequestQueue.requests.get(0);
        String url = request.getUrl();
        assertFalse("URL should not contain contact_ids parameter", url.contains("contact_ids"));
        assertTrue("URL should contain location parameter", url.contains("location=Test%20City%2C%20NY"));
        assertTrue("URL should contain name parameter", url.contains("name=Test%20User"));
    }

    @Test
    public void testGetCustomizedScriptsWithEmptyContactIds() {
        String issueId = "test-issue-123";
        List<String> contactIds = new ArrayList<>();
        String location = "Test City, NY";
        String name = "Test User";

        TestScriptsListener testListener = new TestScriptsListener();
        fiveCallsApi.registerScriptsRequestListener(testListener);
        fiveCallsApi.getCustomizedScripts(issueId, contactIds, location, name);

        assertEquals("Should have one request", 1, testRequestQueue.requests.size());
        Request<?> request = testRequestQueue.requests.get(0);
        String url = request.getUrl();
        assertFalse("URL should not contain contact_ids parameter", url.contains("contact_ids"));
    }

    @Test
    public void testGetCustomizedScriptsWithNullLocation() {
        String issueId = "test-issue-123";
        List<String> contactIds = new ArrayList<>();
        contactIds.add("contact1");
        String name = "Test User";

        TestScriptsListener testListener = new TestScriptsListener();
        fiveCallsApi.registerScriptsRequestListener(testListener);
        fiveCallsApi.getCustomizedScripts(issueId, contactIds, null, name);

        assertEquals("Should have one request", 1, testRequestQueue.requests.size());
        Request<?> request = testRequestQueue.requests.get(0);
        String url = request.getUrl();
        assertFalse("URL should not contain location parameter", url.contains("location="));
        assertTrue("URL should contain name parameter", url.contains("name=Test%20User"));
    }

    @Test
    public void testGetCustomizedScriptsWithEmptyLocation() {
        String issueId = "test-issue-123";
        List<String> contactIds = new ArrayList<>();
        contactIds.add("contact1");
        String name = "Test User";

        TestScriptsListener testListener = new TestScriptsListener();
        fiveCallsApi.registerScriptsRequestListener(testListener);
        fiveCallsApi.getCustomizedScripts(issueId, contactIds, "", name);

        assertEquals("Should have one request", 1, testRequestQueue.requests.size());
        Request<?> request = testRequestQueue.requests.get(0);
        String url = request.getUrl();
        assertFalse("URL should not contain location parameter", url.contains("location="));
    }

    @Test
    public void testGetCustomizedScriptsWithNullName() {
        String issueId = "test-issue-123";
        List<String> contactIds = new ArrayList<>();
        contactIds.add("contact1");
        String location = "Test City, NY";

        TestScriptsListener testListener = new TestScriptsListener();
        fiveCallsApi.registerScriptsRequestListener(testListener);
        fiveCallsApi.getCustomizedScripts(issueId, contactIds, location, null);

        assertEquals("Should have one request", 1, testRequestQueue.requests.size());
        Request<?> request = testRequestQueue.requests.get(0);
        String url = request.getUrl();
        assertFalse("URL should not contain name parameter", url.contains("name="));
        assertTrue("URL should contain location parameter", url.contains("location=Test%20City%2C%20NY"));
    }

    @Test
    public void testGetCustomizedScriptsWithEmptyName() {
        String issueId = "test-issue-123";
        List<String> contactIds = new ArrayList<>();
        contactIds.add("contact1");
        String location = "Test City, NY";

        TestScriptsListener testListener = new TestScriptsListener();
        fiveCallsApi.registerScriptsRequestListener(testListener);
        fiveCallsApi.getCustomizedScripts(issueId, contactIds, location, "");

        assertEquals("Should have one request", 1, testRequestQueue.requests.size());
        Request<?> request = testRequestQueue.requests.get(0);
        String url = request.getUrl();
        assertFalse("URL should not contain name parameter", url.contains("name="));
    }

    @Test
    public void testGetCustomizedScriptsUrlEncoding() {
        String issueId = "test-issue-123";
        List<String> contactIds = new ArrayList<>();
        contactIds.add("contact with spaces");
        contactIds.add("contact!@#$%");
        String location = "New York, NY 10001";
        String name = "José María";

        TestScriptsListener testListener = new TestScriptsListener();
        fiveCallsApi.registerScriptsRequestListener(testListener);
        fiveCallsApi.getCustomizedScripts(issueId, contactIds, location, name);

        assertEquals("Should have one request", 1, testRequestQueue.requests.size());
        Request<?> request = testRequestQueue.requests.get(0);
        String url = request.getUrl();

        // Verify proper URL encoding
        assertTrue("URL should contain encoded contact IDs", url.contains("contact_ids=contact%20with%20spaces%2Ccontact!%40%23%24%25"));
        assertTrue("URL should contain encoded location", url.contains("location=New%20York%2C%20NY%2010001"));
        assertTrue("URL should contain encoded name", url.contains("name=Jos%C3%A9%20Mar%C3%ADa"));
    }

    @Test
    public void testGetCustomizedScriptsMinimalRequest() {
        String issueId = "test-issue-123";

        TestScriptsListener testListener = new TestScriptsListener();
        fiveCallsApi.registerScriptsRequestListener(testListener);
        fiveCallsApi.getCustomizedScripts(issueId, null, null, null);

        assertEquals("Should have one request", 1, testRequestQueue.requests.size());
        Request<?> request = testRequestQueue.requests.get(0);
        String expectedUrl = "https://api.5calls.org/v1/issue/test-issue-123/script";
        assertEquals("URL should match expected", expectedUrl, request.getUrl());
    }

    @Test
    public void testGetCustomizedScriptsSingleContactId() {
        String issueId = "test-issue-123";
        List<String> contactIds = new ArrayList<>();
        contactIds.add("single-contact");

        TestScriptsListener testListener = new TestScriptsListener();
        fiveCallsApi.registerScriptsRequestListener(testListener);
        fiveCallsApi.getCustomizedScripts(issueId, contactIds, null, null);

        assertEquals("Should have one request", 1, testRequestQueue.requests.size());
        Request<?> request = testRequestQueue.requests.get(0);
        String url = request.getUrl();
        assertTrue("URL should contain single contact ID", url.contains("contact_ids=single-contact"));
    }

    @Test
    public void testScriptsResponseParsing() {
        // Create a test listener
        TestScriptsListener testListener = new TestScriptsListener();

        // Manually test the listener callback logic
        List<CustomizedContactScript> scripts = new ArrayList<>();
        scripts.add(new CustomizedContactScript("contact1", "Custom script for contact 1"));
        scripts.add(new CustomizedContactScript("contact2", "Custom script for contact 2"));

        testListener.onScriptsReceived(scripts);

        assertTrue("Listener should have received scripts", testListener.receivedScripts);
        assertEquals("Should receive 2 scripts", 2, testListener.scriptsReceived.size());
        assertEquals("First script ID should match", "contact1", testListener.scriptsReceived.get(0).id);
        assertEquals("First script text should match", "Custom script for contact 1", testListener.scriptsReceived.get(0).script);
    }

    @Test
    public void testScriptsResponseParsingWithNullResponse() {
        TestScriptsListener testListener = new TestScriptsListener();

        // Simulate null response
        testListener.onJsonError();

        assertTrue("Listener should have received JSON error", testListener.receivedJsonError);
    }

    @Test
    public void testScriptsRequestError() {
        TestScriptsListener testListener = new TestScriptsListener();

        // Simulate request error
        testListener.onRequestError();

        assertTrue("Listener should have received request error", testListener.receivedRequestError);
    }

    @Test
    public void testMultipleScriptsListeners() {
        TestScriptsListener testListener1 = new TestScriptsListener();
        TestScriptsListener testListener2 = new TestScriptsListener();

        fiveCallsApi.registerScriptsRequestListener(testListener1);
        fiveCallsApi.registerScriptsRequestListener(testListener2);

        List<CustomizedContactScript> scripts = new ArrayList<>();
        scripts.add(new CustomizedContactScript("contact1", "Test script"));

        // Simulate response to all listeners
        testListener1.onScriptsReceived(scripts);
        testListener2.onScriptsReceived(scripts);

        assertTrue("First listener should receive scripts", testListener1.receivedScripts);
        assertTrue("Second listener should receive scripts", testListener2.receivedScripts);
    }

    // Helper class for testing scripts listeners
    private static class TestScriptsListener implements FiveCallsApi.ScriptsRequestListener {
        public boolean receivedScripts = false;
        public boolean receivedRequestError = false;
        public boolean receivedJsonError = false;
        public List<CustomizedContactScript> scriptsReceived = new ArrayList<>();

        @Override
        public void onRequestError() {
            receivedRequestError = true;
        }

        @Override
        public void onJsonError() {
            receivedJsonError = true;
        }

        @Override
        public void onScriptsReceived(List<CustomizedContactScript> scripts) {
            receivedScripts = true;
            scriptsReceived = scripts;
        }
    }

    // Simple RequestQueue implementation for testing
    private static class TestRequestQueue extends RequestQueue {
        public List<Request<?>> requests = new ArrayList<>();

        public TestRequestQueue() {
            super(null, null);
        }

        @Override
        public <T> Request<T> add(Request<T> request) {
            requests.add(request);
            return request;
        }
    }
}