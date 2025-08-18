package org.a5calls.android.a5calls.net;

import android.os.SystemClock;

import com.android.volley.Header;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HttpResponse;

import org.a5calls.android.a5calls.model.Contact;
import org.a5calls.android.a5calls.model.Issue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.a5calls.android.a5calls.FakeJSONData.ISSUE_DATA;
import static org.a5calls.android.a5calls.FakeJSONData.REPORT_DATA;
import static org.a5calls.android.a5calls.FakeJSONData.REPS_DATA_SUFFIX;
import static org.a5calls.android.a5calls.FakeJSONData.REPS_DATA_NOT_SPLIT_PREFIX;
import static org.a5calls.android.a5calls.FakeJSONData.REPS_DATA_SPLIT_PREFIX;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FiveCallsApiTest {

    static class TestCallListener implements FiveCallsApi.CallRequestListener {
        protected int mCallError = 0;
        protected int mCallJsonError = 0;
        protected int mCallReported = 0;
        protected int mCallCount = 0;
        protected boolean mDonateOn = false;

        @Override
        public void onRequestError() {
            mCallError++;
        }

        @Override
        public void onJsonError() {
            mCallJsonError++;
        }

        @Override
        public void onReportReceived(int count, boolean donateOn) {
            mCallCount = count;
            mDonateOn = donateOn;
        }

        @Override
        public void onCallReported() {
            mCallReported++;
        }
    }

    static class TestIssuesListener implements FiveCallsApi.IssuesRequestListener {
        protected int mIssueError = 0;
        protected int mIssueJsonError = 0;
        protected List<Issue> mIssues = null;

        @Override
        public void onRequestError() {
            mIssueError++;
        }

        @Override
        public void onJsonError() {
            mIssueJsonError++;
        }

        @Override
        public void onIssuesReceived(List<Issue> issues) {
            mIssues = issues;
        }
    }

    static class TestContactsListener implements FiveCallsApi.ContactsRequestListener {
        protected int mContactsError = 0;
        protected int mContactsJsonError = 0;
        protected int mAddressError = 0;
        protected List<Contact> mContacts = null;
        protected boolean mLowAccuracy = false;
        protected String mLocationName = null;
        protected String mLocationCode = null;

        @Override
        public void onRequestError() {
            mContactsError++;
        }

        @Override
        public void onJsonError() {
            mContactsJsonError++;
        }

        @Override
        public void onAddressError() {
            mAddressError++;
        }

        @Override
        public void onContactsReceived(String locationName, String locationCode,
                                       boolean isLowAccuracy, List<Contact> contacts) {
            mLocationName = locationName;
            mLocationCode = locationCode;
            mLowAccuracy = isLowAccuracy;
            mContacts = contacts;
        }
    }

    static class TestNewsletterListener implements FiveCallsApi.NewsletterSubscribeCallback {
        protected boolean mSubscribeSuccess = false;
        protected boolean mSubscribeError = false;

        @Override
        public void onSuccess() {
            mSubscribeSuccess = true;
        }

        @Override
        public void onError() {
            mSubscribeError = true;
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
        mApi = new FiveCallsApi("itMe", mRequestQueue);
    }

    @After
    public void tearDown() {
        mRequestQueue.mRequest = null;
    }

    @Test
    public void testGetCallCount() {
        byte[] bytes = REPORT_DATA.getBytes();
        ArrayList<Header> headers = new ArrayList<>();
        headers.add(new Header("Content-Type", "text/json"));
        HttpResponse response = new HttpResponse(200, headers, bytes);
        mHttpStack.setResponseToReturn(response);

        TestCallListener testCallListener = new TestCallListener();
        mApi.registerCallRequestListener(testCallListener);
        mApi.getReport();
        waitForHttpRequestComplete();

        assertEquals(0, testCallListener.mCallError);
        assertEquals(0, testCallListener.mCallJsonError);
        assertEquals(0, testCallListener.mCallReported);
        assertEquals(4627301, testCallListener.mCallCount);
        assertTrue(testCallListener.mDonateOn);

        mApi.unregisterCallRequestListener(testCallListener);
    }

    @Test
    public void testGetCallCount_serverError() {
        mHttpStack.setExceptionToThrow(new IOException("HTTP Stack exception"));

        TestCallListener testCallListener = new TestCallListener();
        mApi.registerCallRequestListener(testCallListener);
        mApi.getReport();
        waitForHttpRequestComplete();

        assertEquals(1, testCallListener.mCallError);
        assertEquals(0, testCallListener.mCallJsonError);
        assertEquals(0, testCallListener.mCallReported);
        assertEquals(0, testCallListener.mCallCount);
        assertFalse(testCallListener.mDonateOn);

        mApi.unregisterCallRequestListener(testCallListener);
    }

    @Test
    public void testGetCallCount_malformedJson() {
        byte[] bytes = "{null}".getBytes(); // Break the JSON.
        ArrayList<Header> headers = new ArrayList<>();
        headers.add(new Header("Content-Type", "text/json"));
        HttpResponse response = new HttpResponse(200, headers, bytes);
        mHttpStack.setResponseToReturn(response);

        TestCallListener testCallListener = new TestCallListener();
        mApi.registerCallRequestListener(testCallListener);
        mApi.getReport();
        waitForHttpRequestComplete();

        assertEquals(1, testCallListener.mCallError);
        assertEquals(0, testCallListener.mCallJsonError);
        assertEquals(0, testCallListener.mCallReported);
        assertEquals(0, testCallListener.mCallCount);
        assertFalse(testCallListener.mDonateOn);

        mApi.unregisterCallRequestListener(testCallListener);
    }

    @Test
    public void testGetIssues() {
        byte[] bytes = ISSUE_DATA.getBytes();
        ArrayList<Header> headers = new ArrayList<>();
        headers.add(new Header("Content-Type", "text/json"));
        HttpResponse response = new HttpResponse(200, headers, bytes);
        mHttpStack.setResponseToReturn(response);

        TestIssuesListener testIssueListener = new TestIssuesListener();
        mApi.registerIssuesRequestListener(testIssueListener);
        mApi.getIssues();
        waitForHttpRequestComplete();

        assertEquals(0, testIssueListener.mIssueError);
        assertEquals(0, testIssueListener.mIssueJsonError);
        assertNotNull(testIssueListener.mIssues);
        assertFalse(testIssueListener.mIssues.isEmpty());
        assertEquals(14, testIssueListener.mIssues.size());

        mApi.unregisterIssuesRequestListener(testIssueListener);
    }

    @Test
    public void testGetIssues_serverError() {
        mHttpStack.setExceptionToThrow(new IOException("HTTP Stack exception"));

        TestIssuesListener testIssueListener = new TestIssuesListener();
        mApi.registerIssuesRequestListener(testIssueListener);
        mApi.getIssues();

        waitForHttpRequestComplete();

        assertEquals(1, testIssueListener.mIssueError);
        assertEquals(0, testIssueListener.mIssueJsonError);
        assertNull(testIssueListener.mIssues);

        mApi.unregisterIssuesRequestListener(testIssueListener);
    }

    @Test
    public void testGetIssues_malformedJson() {
        byte[] bytes = ISSUE_DATA.substring(0, 250).getBytes();
        ArrayList<Header> headers = new ArrayList<>();
        headers.add(new Header("Content-Type", "text/json"));
        HttpResponse response = new HttpResponse(200, headers, bytes);
        mHttpStack.setResponseToReturn(response);

        TestIssuesListener testIssueListener = new TestIssuesListener();
        mApi.registerIssuesRequestListener(testIssueListener);
        mApi.getIssues();
        waitForHttpRequestComplete();

        assertEquals(1, testIssueListener.mIssueError);
        assertEquals(0, testIssueListener.mIssueJsonError);
        assertNull(testIssueListener.mIssues);

        mApi.unregisterIssuesRequestListener(testIssueListener);
    }

    @Test
    public void testGetContacts() {
        byte[] bytes = (REPS_DATA_NOT_SPLIT_PREFIX + REPS_DATA_SUFFIX).getBytes();
        ArrayList<Header> headers = new ArrayList<>();
        headers.add(new Header("Content-Type", "text/json"));
        HttpResponse response = new HttpResponse(200, headers, bytes);
        mHttpStack.setResponseToReturn(response);

        TestContactsListener testContactsListener = new TestContactsListener();
        mApi.registerContactsRequestListener(testContactsListener);
        mApi.getContacts("New York, New York");
        waitForHttpRequestComplete();

        assertEquals(0, testContactsListener.mContactsError);
        assertEquals(0, testContactsListener.mAddressError);
        assertEquals(0, testContactsListener.mContactsJsonError);
        assertNotNull(testContactsListener.mContacts);
        assertFalse(testContactsListener.mContacts.isEmpty());
        assertEquals(6, testContactsListener.mContacts.size());
        assertEquals("BOWLING GREEN", testContactsListener.mLocationName);
        assertEquals("NY-10", testContactsListener.mLocationCode);
    }

    @Test
    public void testGetContactsSplit() {
        byte[] bytes = (REPS_DATA_SPLIT_PREFIX + REPS_DATA_SUFFIX).getBytes();
        ArrayList<Header> headers = new ArrayList<>();
        headers.add(new Header("Content-Type", "text/json"));
        HttpResponse response = new HttpResponse(200, headers, bytes);
        mHttpStack.setResponseToReturn(response);

        TestContactsListener testContactsListener = new TestContactsListener();
        mApi.registerContactsRequestListener(testContactsListener);
        mApi.getContacts("New York, New York");
        waitForHttpRequestComplete();

        assertEquals(0, testContactsListener.mContactsError);
        assertEquals(0, testContactsListener.mAddressError);
        assertEquals(0, testContactsListener.mContactsJsonError);
        assertTrue(testContactsListener.mLowAccuracy);

    }

    @Test
    public void testGetContacts_serverError() {
        mHttpStack.setExceptionToThrow(new IOException("HTTP Stack exception"));

        TestContactsListener testContactsListener = new TestContactsListener();
        mApi.registerContactsRequestListener(testContactsListener);
        mApi.getContacts("Washington, DC");
        waitForHttpRequestComplete();

        assertEquals(1, testContactsListener.mContactsError);
        assertEquals(0, testContactsListener.mAddressError);
        assertEquals(0, testContactsListener.mContactsJsonError);
        assertNull(testContactsListener.mContacts);
        assertNull(testContactsListener.mLocationName);
        assertNull(testContactsListener.mLocationCode);
    }

    @Test
    public void testGetContacts_malformedJson() {
        byte[] bytes = "{Malformed".getBytes();
        ArrayList<Header> headers = new ArrayList<>();
        headers.add(new Header("Content-Type", "text/json"));
        HttpResponse response = new HttpResponse(200, headers, bytes);
        mHttpStack.setResponseToReturn(response);

        TestContactsListener testContactsListener = new TestContactsListener();
        mApi.registerContactsRequestListener(testContactsListener);
        mApi.getContacts("Washington, DC");
        waitForHttpRequestComplete();

        assertEquals(1, testContactsListener.mContactsError);
        assertEquals(0, testContactsListener.mAddressError);
        assertEquals(0, testContactsListener.mContactsJsonError);
        assertNull(testContactsListener.mContacts);
        assertNull(testContactsListener.mLocationName);
        assertNull(testContactsListener.mLocationCode);
    }

    @Test
    public void testReportCall() {
        byte[] bytes = "{\"ok\":true}".getBytes();
        ArrayList<Header> headers = new ArrayList<>();
        headers.add(new Header("Content-Type", "text/json"));
        HttpResponse response = new HttpResponse(200, headers);
        mHttpStack.setResponseToReturn(response);

        TestCallListener testCallListener = new TestCallListener();
        mApi.registerCallRequestListener(testCallListener);

        mApi.reportCall("myIssue", "myRep", "unavailable", "myLocation");
        waitForHttpRequestComplete();

        assertEquals(1, testCallListener.mCallReported);
        assertEquals(0, testCallListener.mCallError);
        assertEquals(0, testCallListener.mCallJsonError);

        assertEquals(new String(mHttpStack.getLastPostBody()),
                "result=unavailable&issueid=myIssue&contactid=myRep&callerid=itMe&via=" +
                        (FiveCallsApi.TESTING ? "test&" : "android&"));

        mApi.unregisterCallRequestListener(testCallListener);
    }

    @Test
    public void testReportCall_serverError() {
        mHttpStack.setExceptionToThrow(new IOException("Kids these days"));

        TestCallListener testCallListener = new TestCallListener();
        mApi.registerCallRequestListener(testCallListener);

        mApi.reportCall("myIssue", "myRep", "unavailable", "myLocation");
        waitForHttpRequestComplete();

        assertEquals(0, testCallListener.mCallReported);
        assertEquals(1, testCallListener.mCallError);
        assertEquals(0, testCallListener.mCallJsonError);

        mApi.unregisterCallRequestListener(testCallListener);
    }

    @Test
    public void newsletterSubscribe() {
        byte[] bytes = "<html><body>Good job</body></html>".getBytes();
        ArrayList<Header> headers = new ArrayList<>();
        headers.add(new Header("Content-Type", "text/html"));
        HttpResponse response = new HttpResponse(200, headers);
        mHttpStack.setResponseToReturn(response);

        TestNewsletterListener testNewsletterListener = new TestNewsletterListener();

        mApi.newsletterSubscribe("my@email.com", testNewsletterListener);
        waitForHttpRequestComplete();

        assertFalse(testNewsletterListener.mSubscribeError);
        assertTrue(testNewsletterListener.mSubscribeSuccess);

        assertEquals(new String(mHttpStack.getLastPostBody()), "tag=android&email=my%40email.com&");
    }

    @Test
    public void newsletterSubscribe_serverError() {
        mHttpStack.setExceptionToThrow(new IOException("Call your reps!"));

        TestNewsletterListener testNewsletterListener = new TestNewsletterListener();

        mApi.newsletterSubscribe("my@email.com", testNewsletterListener);
        waitForHttpRequestComplete();

        assertTrue(testNewsletterListener.mSubscribeError);
        assertFalse(testNewsletterListener.mSubscribeSuccess);
    }

    private void waitForHttpRequestComplete() {
        assertNotNull(mRequestQueue.mRequest);
        mRequestQueue.start();

        // Wait for the async stuff.
        // TODO: I'm sure there's a better way to do this...
        SystemClock.sleep(200);
    }
}