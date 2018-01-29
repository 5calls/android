package org.a5calls.android.a5calls.net;

import android.content.Context;
import android.text.TextUtils;
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
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.a5calls.android.a5calls.BuildConfig;
import org.a5calls.android.a5calls.model.Issue;
import org.a5calls.android.a5calls.model.Outcome;
import org.a5calls.android.a5calls.model.Stat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to handle server gets and posts.
 */
public class FiveCallsApi {
    private static final String TAG = "FiveCallsApi";

    // Set TESTING "true" to set a parameter to the count call request which marks it as a test
    // request on the server. This will only work on debug builds.
    private static final boolean TESTING = true;

    private static final String GET_ISSUES_REQUEST = "https://5calls.org/issues/?address=";

    private static final String GET_REPORT = "https://5calls.org/report";

    private static final String GET_STATS = "https://api.5calls.org/v1/users/stats";

    private static final String POST_STATS = GET_STATS;

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

        void onIssuesReceived(String locationName, boolean splitDistrict, List<Issue> issues);
    }

    public interface StatsRequestListener {
        void onRequestError();

        void onJsonError();

        void onStatsReceived(List<Stat> stats);

        // The stats which were updated in the server.
        void onStatsUpdated(List<Stat> stats);
    }

    private RequestQueue mRequestQueue;
    private Gson mGson;
    private List<CallRequestListener> mCallRequestListeners = new ArrayList<>();
    private List<IssuesRequestListener> mIssuesRequestListeners = new ArrayList<>();
    private List<StatsRequestListener> mStatsRequestListeners = new ArrayList<>();

    public FiveCallsApi(Context context) {
        // TODO: Using OkHttpClient and OkHttpStack cause failures on multiple types of Samsung
        // Galaxy devices.
        //mRequestQueue = Volley.newRequestQueue(context, new OkHttpStack(new OkHttpClient()));
        mRequestQueue = Volley.newRequestQueue(context);
        mGson = new GsonBuilder()
                .serializeNulls()
                .registerTypeAdapter(Outcome.Status.class, new OutcomeStatusTypeAdapter())
                .create();
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

    public void registerStatsRequestListener(StatsRequestListener statsRequestListener) {
        mStatsRequestListeners.add(statsRequestListener);
    }

    public void unregisterStatsRequestListener(StatsRequestListener statsRequestListener) {
        if (mStatsRequestListeners.contains(statsRequestListener)) {
            mStatsRequestListeners.remove(statsRequestListener);
        }
    }

    public void onDestroy() {
        mRequestQueue.cancelAll(TAG);
        mRequestQueue.stop();
        mRequestQueue = null;
    }

    public void getIssuesForLocation(String address) {
        String url = GET_ISSUES_REQUEST + URLEncoder.encode(address) + "&all=true";
        buildIssuesRequest(url, mIssuesRequestListeners);
    }

    public void getInactiveIssuesForLocation(String address, IssuesRequestListener listener) {
        String url = GET_ISSUES_REQUEST + URLEncoder.encode(address) + "&inactive=true";
        List<IssuesRequestListener> list = Collections.singletonList(listener);
        buildIssuesRequest(url, list);
    }

    private void buildIssuesRequest(String url, final List<IssuesRequestListener> listeners) {
        // Request a JSON Object response from the provided URL.
        JsonObjectRequest statusRequest = new JsonObjectRequest(
                Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if (response != null) {
                    String locationName = "";
                    boolean splitDistrict = false;
                    try {
                        if (response.getBoolean("invalidAddress")) {
                            for (IssuesRequestListener listener : listeners) {
                                listener.onAddressError();
                            }
                            return;
                        }
                        locationName = response.getString("normalizedLocation");
                        splitDistrict = response.getBoolean("splitDistrict");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    JSONArray jsonArray = response.optJSONArray("issues");
                    if (jsonArray == null) {
                        for (IssuesRequestListener listener : listeners) {
                            listener.onJsonError();
                        }
                        return;
                    }
                    Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
                    List<Issue> issues = mGson.fromJson(jsonArray.toString(),
                            listType);

                    issues = Outcome.filterSkipOutcomes(issues);

                    // TODO: Sanitize contact IDs here
                    for (IssuesRequestListener listener : listeners) {
                        listener.onIssuesReceived(locationName, splitDistrict, issues);
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                for (IssuesRequestListener listener : listeners) {
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
    // User ID from Auth0 is omitted if no one is logged in.
    public void reportCall(final String issueId, final String contactId, final String result,
                           final String zip, final String userId) {
        String getReport = GET_REPORT;
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
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("issueid", issueId);
                params.put("result", result);
                params.put("contactid", contactId);
                params.put("location", zip);
                params.put("via", (BuildConfig.DEBUG && TESTING) ? "test" : "android");
                if (!TextUtils.isEmpty(userId)) {
                    params.put("userid", userId);
                }
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/x-www-form-urlencoded");
                return params;
            }
        };
        request.setTag(TAG);
        // Add the request to the RequestQueue.
        mRequestQueue.add(request);
    }

    private void uploadUserStats(final String jwt, final List<Stat> stats) {
        String postStats = POST_STATS;
        StringRequest request = new StringRequest(Request.Method.POST, postStats,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        for (StatsRequestListener listener : mStatsRequestListeners) {
                            listener.onStatsUpdated(stats);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                for (StatsRequestListener listener : mStatsRequestListeners) {
                    listener.onRequestError();
                }
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("stats", Stat.toJsonString(stats)); // TODO: Can we use gson?
                params.put("via", (BuildConfig.DEBUG && TESTING) ? "test" : "android");
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/x-www-form-urlencoded");
                return params;
            }
        };
        request.setTag(TAG);
        // Add the request to the RequestQueue.
        mRequestQueue.add(request);
        /*

        -H 'Content-Type: application/json; charset=utf-8' \
     -H 'Authorization: Bearer <JWT TOKEN HERE>' \
     -d $'[
  {
    "issueID": "34",
    "contactID": "contactid",
    "result": "contact",
    "time": "1483841102"
  }

         */
    }

    // The jwt is credentials id_token.
    private void getUserStats(final String jwt) {
        String getStats = GET_STATS;
        JsonObjectRequest statsRequest = new JsonObjectRequest(
                Request.Method.GET, getStats, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                // TODO: Get stats properly...
                JSONArray jsonArray = response.optJSONArray("stats");
                if (jsonArray == null) {
                    for (StatsRequestListener listener : mStatsRequestListeners) {
                        listener.onJsonError();
                    }
                    return;
                }
                Type listType = new TypeToken<ArrayList<Stat>>(){}.getType();
                List<Stat> stats = mGson.fromJson(jsonArray.toString(), listType);
                for (StatsRequestListener listener : mStatsRequestListeners) {
                    listener.onStatsReceived(stats);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                for (StatsRequestListener listener : mStatsRequestListeners) {
                    listener.onRequestError();
                }
                if (error.getMessage() == null) {
                    Log.d("Error", "no message");
                } else {
                    Log.d("Error", error.getMessage());
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Cache-Control:", "max-age=0"); // TODO: Use cached results.
                params.put("Authorization:", "Bearer " + jwt);
                return params;
            }
        };
        statsRequest.setTag(TAG); // TODO: same tag OK?
        // Add the request to the RequestQueue.
        mRequestQueue.add(statsRequest);
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
