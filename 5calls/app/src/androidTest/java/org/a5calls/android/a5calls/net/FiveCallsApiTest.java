package org.a5calls.android.a5calls.net;

import android.os.SystemClock;

import com.android.volley.Header;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HttpResponse;
import com.android.volley.toolbox.NoCache;

import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.a5calls.android.a5calls.FakeJSONData.REPORT_DATA;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FiveCallsApiTest {

    public static class TestCallListener implements FiveCallsApi.CallRequestListener {
        protected int mCallError = 0;
        protected int mCallJsonError = 0;
        protected int mCallReported = 0;
        protected int mCallCount = 0;

        @Override
        public void onRequestError() {
            mCallError++;
        }

        @Override
        public void onJsonError() {
            mCallJsonError++;
        }

        @Override
        public void onCallCount(int count) {
            mCallCount = count;
        }

        @Override
        public void onCallReported() {
            mCallReported++;
        }
    }

    private FiveCallsApi mApi;
    private FakeRequestQueue mRequestQueue;
    private MockHttpStack mHttpStack;

    private BasicNetwork mBasicNetwork;


    @Before
    public void setUp() throws Exception {
        mHttpStack = new MockHttpStack();
        mBasicNetwork = new BasicNetwork(mHttpStack);
        mRequestQueue = new FakeRequestQueue(mBasicNetwork);
        mApi = new FiveCallsApi("itMe", mRequestQueue);
    }

    @Test
    public void getCallCountTest() {
        byte[] bytes = REPORT_DATA.getBytes();
        ArrayList<Header> headers = new ArrayList<>();
        headers.add(new Header("Content-Type", "text/json"));
        HttpResponse response = new HttpResponse(200, headers, bytes);
        mHttpStack.setResponseToReturn(response);

        TestCallListener testCallListener = new TestCallListener();
        mApi.registerCallRequestListener(testCallListener);
        mApi.getCallCount();
        assertNotNull(mRequestQueue.mRequest);
        mRequestQueue.start();

        // Wait for the async stuff. I'm sure there's a better way to do this but
        // this doesn't really need to scale.
        SystemClock.sleep(200);

        assertEquals(0, testCallListener.mCallError);
        assertEquals(0, testCallListener.mCallJsonError);
        assertEquals(0, testCallListener.mCallReported);
        assertEquals(4627301, testCallListener.mCallCount);
    }

    @Test
    public void testGetCallCount_ServerError() {
        mHttpStack.setExceptionToThrow(new IOException("HTTP Stack exception"));

        TestCallListener testCallListener = new TestCallListener();
        mApi.registerCallRequestListener(testCallListener);
        mApi.getCallCount();
        assertNotNull(mRequestQueue.mRequest);
        mRequestQueue.start();

        // Wait for the async stuff.
        SystemClock.sleep(200);

        assertEquals(1, testCallListener.mCallError);
        assertEquals(0, testCallListener.mCallJsonError);
        assertEquals(0, testCallListener.mCallReported);
        assertEquals(0, testCallListener.mCallCount);
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
        mApi.getCallCount();
        assertNotNull(mRequestQueue.mRequest);
        mRequestQueue.start();

        // Wait for the async stuff.
        SystemClock.sleep(200);

        assertEquals(1, testCallListener.mCallError);
        assertEquals(0, testCallListener.mCallReported);
        assertEquals(0, testCallListener.mCallCount);
    }
    
    // TODO: Add tests for other API calls.
}