/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Based on android's /src/test/java/com/android/volley/mock/MockHttpStack.java,
// but upgraded to use BaseHttpStack.

package org.a5calls.android.a5calls.net;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.BaseHttpStack;
import com.android.volley.toolbox.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MockHttpStack extends BaseHttpStack {
    private HttpResponse mResponseToReturn;
    private IOException mExceptionToThrow;
    private String mLastUrl;
    private Map<String, String> mLastHeaders;
    private byte[] mLastPostBody;

    // Add a map to store responses for different URL patterns
    private Map<String, HttpResponse> mUrlPatternResponses = new HashMap<>();

    public String getLastUrl() {
        return mLastUrl;
    }

    public Map<String, String> getLastHeaders() {
        return mLastHeaders;
    }

    public byte[] getLastPostBody() {
        return mLastPostBody;
    }

    public void setResponseToReturn(com.android.volley.toolbox.HttpResponse response) {
        mResponseToReturn = response;
    }

    public void setExceptionToThrow(IOException exception) {
        mExceptionToThrow = exception;
    }

    // Add a method to set responses for specific URL patterns
    public void setResponseForUrlPattern(String urlPattern, HttpResponse response) {
        mUrlPatternResponses.put(urlPattern, response);
    }

    // Add a method to clear all URL pattern responses
    public void clearUrlPatternResponses() {
        mUrlPatternResponses.clear();
    }

    @Override
    public com.android.volley.toolbox.HttpResponse executeRequest(Request<?> request, Map<String, String> additionalHeaders) throws IOException, AuthFailureError {
        if (mExceptionToThrow != null) {
            throw mExceptionToThrow;
        }
        mLastUrl = request.getUrl();
        mLastHeaders = new HashMap<String, String>();
        if (request.getHeaders() != null) {
            mLastHeaders.putAll(request.getHeaders());
        }
        if (additionalHeaders != null) {
            mLastHeaders.putAll(additionalHeaders);
        }
        try {
            mLastPostBody = request.getBody();
        } catch (AuthFailureError e) {
            mLastPostBody = null;
        }

        // Check if we have a response for this URL pattern
        for (Map.Entry<String, HttpResponse> entry : mUrlPatternResponses.entrySet()) {
            if (mLastUrl.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Fall back to the default response
        return mResponseToReturn;
    }
}
