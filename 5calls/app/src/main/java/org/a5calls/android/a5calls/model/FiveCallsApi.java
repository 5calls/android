package org.a5calls.android.a5calls.model;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.a5calls.android.a5calls.BuildConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to handle server gets and posts.
 */
public class FiveCallsApi {
    private static final String TAG = "FiveCallsApi";

    // This only works on an emulator running on a machine that is running a local version of the
    // 5calls go server. For more on the go server, check out github.com/5calls/5calls.
    // Recommended setting this to "true" for testing, though, to avoid adding fake calls to the
    // server.
    private static final boolean USE_LOCAL_DEBUG_REPORT = false;

    private static final String GET_ISSUES_REQUEST = "https://5calls.org/issues/?address=";

    private static final String GET_REPORT = "https://5calls.org/report";

    // This is for local testing only and shouldn't be part of prod.
    private static final String GET_REPORT_DEBUG = "http://10.0.2.2:8090/report";

    public interface CallRequestListener {
        void onRequestError();
        void onJsonError();
        void onCallCount(int count);
        void onCallReported();
    }

    public interface IssuesRequestListener {
        void onRequestError();
        void onJsonError();
        void onAddressError();
        void onIssuesReceived(String locationName, List<Issue> issues);
    }

    private RequestQueue mRequestQueue;
    private List<CallRequestListener> mCallRequestListeners = new ArrayList<>();
    private List<IssuesRequestListener> mIssuesRequestListeners = new ArrayList<>();

    public FiveCallsApi(Context context) {
        mRequestQueue = Volley.newRequestQueue(context);
    }

    public void registerCallRequestListener(CallRequestListener callRequestListener) {
        mCallRequestListeners.add(callRequestListener);
    }

    public void unregisterCallRequestListener(CallRequestListener callRequestListener) {
        if (mCallRequestListeners.contains(callRequestListener)) {
            mCallRequestListeners.remove(callRequestListener);
        }
    }

    public void registerIssuesRequestListener(IssuesRequestListener issuesRequestListener) {
        mIssuesRequestListeners.add(issuesRequestListener);
    }

    public void unregisterIssuesRequestListener(IssuesRequestListener issuesRequestListener) {
        if (mIssuesRequestListeners.contains(issuesRequestListener)) {
            mIssuesRequestListeners.remove(issuesRequestListener);
        }
    }

    public void onDestroy() {
        mRequestQueue.cancelAll(TAG);
        mRequestQueue.stop();
        mRequestQueue = null;
    }

    public void getIssuesForLocation(String address) {
        String url = GET_ISSUES_REQUEST + URLEncoder.encode(address);
        // Request a JSON Object response from the provided URL.
        JsonObjectRequest statusRequest = new JsonObjectRequest(
                Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if (response != null) {
                    String locationName = "";
                    try {
                        if (response.getBoolean("invalidAddress")) {
                            for (IssuesRequestListener listener : mIssuesRequestListeners) {
                                listener.onAddressError();
                            }
                            return;
                        }
                        locationName = response.getString("normalizedLocation");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    JSONArray jsonArray = response.optJSONArray("issues");
                    if (jsonArray == null) {
                        for (IssuesRequestListener listener : mIssuesRequestListeners) {
                            listener.onJsonError();
                        }
                        return;
                    }
                    Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
                    List<Issue> issues = new Gson().fromJson(jsonArray.toString(),
                            listType);
                    // TODO: Sanitize contact IDs here
                    for (IssuesRequestListener listener : mIssuesRequestListeners) {
                        listener.onIssuesReceived(locationName, issues);
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                for (IssuesRequestListener listener : mIssuesRequestListeners) {
                    listener.onRequestError();
                }
            }
        });
        statusRequest.setTag(TAG);
        // Add the request to the RequestQueue.
        mRequestQueue.add(statusRequest);
    }

    public void getCallCount() {
        String getReport = GET_REPORT;
        if (BuildConfig.DEBUG && USE_LOCAL_DEBUG_REPORT) {
            getReport = GET_REPORT_DEBUG;
        }
        JsonObjectRequest reportRequest = new JsonObjectRequest(
                Request.Method.GET, getReport, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    int count = response.getInt("count");
                    for (CallRequestListener listener : mCallRequestListeners) {
                        listener.onCallCount(count);
                    }
                } catch (JSONException e) {
                    for (CallRequestListener listener : mCallRequestListeners) {
                        listener.onJsonError();
                    }
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                onRequestError(error);
            }
        });
        reportRequest.setTag(TAG); // TODO: same tag OK?
        // Add the request to the RequestQueue.
        mRequestQueue.add(reportRequest);
    }

    // Result is "VOICEMAIL", "unavailable", or "contacted"
    // https://github.com/5calls/5calls/blob/master/static/js/main.js#L221
    public void reportCall(final String issueId, final String contactId, final String result,
                           final String zip) {
        String getReport = GET_REPORT;
        if (BuildConfig.DEBUG && USE_LOCAL_DEBUG_REPORT) {
            getReport = GET_REPORT_DEBUG;
        }
        StringRequest request = new StringRequest(Request.Method.POST, getReport,
                new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                for (CallRequestListener listener : mCallRequestListeners) {
                    listener.onCallReported();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                onRequestError(error);
            }
        }){
            @Override
            protected Map<String,String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("issueid", issueId);
                params.put("result", result);
                params.put("contactid", contactId);
                params.put("location", zip);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/x-www-form-urlencoded");
                return params;
            }
        };
        request.setTag(TAG); // TODO: same tag OK?
        // Add the request to the RequestQueue.
        mRequestQueue.add(request);
    }

    private void onRequestError(VolleyError error) {
        for (CallRequestListener listener : mCallRequestListeners) {
            listener.onRequestError();
        }
        if (error.getMessage() == null) {
            Log.d("Error", "no message");
        } else {
            Log.d("Error", error.getMessage());
        }
    }
}
