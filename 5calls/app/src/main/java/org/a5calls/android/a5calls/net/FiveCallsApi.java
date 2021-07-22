package org.a5calls.android.a5calls.net;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.a5calls.android.a5calls.BuildConfig;
import org.a5calls.android.a5calls.model.Contact;
import org.a5calls.android.a5calls.model.Issue;
import org.a5calls.android.a5calls.model.Outcome;
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

    // Set TESTING "true" to set a parameter to the count call request which marks it as a test
    // request on the server. This will only work on debug builds.
    private static final boolean TESTING = true;

    private static final String GET_ISSUES_REQUEST = "https://api.5calls.org/v1/issues"; //"https://5calls.org/issues/?address=";

    private static final String GET_CONTACTS_REQUEST = "https://api.5calls.org/v1/reps?location=";

    private static final String GET_REPORT = "https://api.5calls.org/v1/report";

    public interface CallRequestListener {
        void onRequestError();

        void onJsonError();

        void onCallCount(int count);

        void onCallReported();
    }

    public interface IssuesRequestListener {
        void onRequestError();

        void onJsonError();

        void onIssuesReceived(List<Issue> issues);
    }

    public interface ContactsRequestListener {
        void onRequestError();

        void onJsonError();

        void onAddressError();

        void onContactsReceived(String locationName, List<Contact> contacts);
    }

    private RequestQueue mRequestQueue;
    private Gson mGson;
    private List<CallRequestListener> mCallRequestListeners = new ArrayList<>();
    private List<IssuesRequestListener> mIssuesRequestListeners = new ArrayList<>();
    private List<ContactsRequestListener> mContactsRequestListeners = new ArrayList<>();

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

    public void registerContactsRequestListener(ContactsRequestListener contactsRequestListener) {
        mContactsRequestListeners.add(contactsRequestListener);
    }

    public void unregisterContactsRequestListener(ContactsRequestListener contactsRequestListener) {
        if (mContactsRequestListeners.contains(contactsRequestListener)) {
            mContactsRequestListeners.remove(contactsRequestListener);
        }
    }

    public void onDestroy() {
        mRequestQueue.cancelAll(TAG);
        mRequestQueue.stop();
        mRequestQueue = null;
    }

    public void getIssues() {
        buildIssuesRequest(GET_ISSUES_REQUEST, mIssuesRequestListeners);
    }

    public void getContacts(String address) {
        buildContactsRequest(GET_CONTACTS_REQUEST + URLEncoder.encode(address), mContactsRequestListeners);
    }

    private void buildIssuesRequest(String url, final List<IssuesRequestListener> listeners) {
        // Request a JSON Object response from the provided URL.
        JsonArrayRequest issuesRequest = new JsonArrayRequest(
                Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                if (response == null) {
                    for (IssuesRequestListener listener : listeners) {
                        listener.onJsonError();
                    }
                    return;
                }
                Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
                List<Issue> issues = mGson.fromJson(response.toString(),
                        listType);

                issues = Outcome.filterSkipOutcomes(issues);

                // TODO: Sanitize contact IDs here
                for (IssuesRequestListener listener : listeners) {
                    listener.onIssuesReceived(issues);
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
        issuesRequest.setTag(TAG);
        // Add the request to the RequestQueue.
        mRequestQueue.add(issuesRequest);
    }

    private void buildContactsRequest(String url, final List<ContactsRequestListener> listeners) {
            // Request a JSON Object response from the provided URL.
            JsonObjectRequest contactsRequest = new JsonObjectRequest(
                    Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    if (response != null) {
                        String locationName = "";
                        try {
                            locationName = response.getString("location");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        JSONArray jsonArray = response.optJSONArray("representatives");
                        if (jsonArray == null) {
                            for (ContactsRequestListener listener : listeners) {
                                listener.onJsonError();
                            }
                            return;
                        }
                        Type listType = new TypeToken<ArrayList<Contact>>(){}.getType();
                        List<Contact> contacts = mGson.fromJson(jsonArray.toString(), listType);
                        for (ContactsRequestListener listener : listeners) {
                            listener.onContactsReceived(locationName, contacts);
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    for (ContactsRequestListener listener : listeners) {
                        listener.onRequestError();
                    }
                }
            });
            contactsRequest.setTag(TAG);
            // Add the request to the RequestQueue.
            mRequestQueue.add(contactsRequest);
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
    public void reportCall(final String issueId, final String contactId, final String result,
                           final String zip) {
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
