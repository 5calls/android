package org.a5calls.android.a5calls.net;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.onesignal.OneSignal;

import org.a5calls.android.a5calls.BuildConfig;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.Contact;
import org.a5calls.android.a5calls.model.Issue;
import org.a5calls.android.a5calls.model.Outcome;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
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
    protected static final boolean TESTING = true;

    private static final String REQUEST_URL_ENCODING_CHARSET = "UTF-8";

    private static final String GET_ISSUES_REQUEST = "https://api.5calls.org/v1/issues";
    private static final String GET_ISSUES_REQUEST_PARAM_STATE = "state";

    private static final String GET_CONTACTS_REQUEST = "https://api.5calls.org/v1/reps?location=";

    private static final String GET_REPORT = "https://api.5calls.org/v1/report";

    private static final String NEWSLETTER_SUBSCRIBE = "https://buttondown.com/api/emails/embed-subscribe/5calls";

    private static final String SEARCH_TRACKING = "https://api.5calls.org/v1/users/search";

    public interface CallRequestListener {
        void onRequestError();

        void onJsonError();

        void onReportReceived(int count, boolean donateOn);

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

        void onContactsReceived(String locationName, String districtId, boolean isDistrictSplit,
                                boolean isLowAccuracy, List<Contact> contacts, boolean stateChanged);
    }

    public interface NewsletterSubscribeCallback {
        void onSuccess();
        void onError();
    }

    private RequestQueue mRequestQueue;
    private Gson mGson;
    private List<CallRequestListener> mCallRequestListeners = new ArrayList<>();
    private List<IssuesRequestListener> mIssuesRequestListeners = new ArrayList<>();
    private List<ContactsRequestListener> mContactsRequestListeners = new ArrayList<>();

    private final String mCallerId;
    private final Context mContext;

    public FiveCallsApi(String callerId, RequestQueue requestQueue, Context context) {
        // TODO: Using OkHttpClient and OkHttpStack cause failures on multiple types of Samsung
        // Galaxy devices.
        mCallerId = callerId;
        mContext = context;
        //mRequestQueue = Volley.newRequestQueue(context, new OkHttpStack(new OkHttpClient()));
        mRequestQueue = requestQueue;
        mGson = new GsonBuilder()
                .serializeNulls()
                .registerTypeAdapter(Outcome.Status.class, new OutcomeStatusTypeAdapter())
                .create();
    }

    public void registerCallRequestListener(CallRequestListener callRequestListener) {
        mCallRequestListeners.add(callRequestListener);
    }

    public void unregisterCallRequestListener(CallRequestListener callRequestListener) {
        mCallRequestListeners.remove(callRequestListener);
    }

    public void registerIssuesRequestListener(IssuesRequestListener issuesRequestListener) {
        mIssuesRequestListeners.add(issuesRequestListener);
    }

    public void unregisterIssuesRequestListener(IssuesRequestListener issuesRequestListener) {
        mIssuesRequestListeners.remove(issuesRequestListener);
    }

    public void registerContactsRequestListener(ContactsRequestListener contactsRequestListener) {
        mContactsRequestListeners.add(contactsRequestListener);
    }

    public void unregisterContactsRequestListener(ContactsRequestListener contactsRequestListener) {
        mContactsRequestListeners.remove(contactsRequestListener);
    }

    public void getIssues() {
        Uri.Builder urlBuilder = Uri.parse(GET_ISSUES_REQUEST).buildUpon();

        // Include state parameter if we have it stored
        String state = AccountManager.Instance.getState(mContext);
        if (!TextUtils.isEmpty(state)) {
            urlBuilder.appendQueryParameter(GET_ISSUES_REQUEST_PARAM_STATE, state);
        }
        
        String url = urlBuilder.build().toString();
        buildIssuesRequest(url, mIssuesRequestListeners);
    }

    public void getContacts(String address) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                buildContactsRequest(GET_CONTACTS_REQUEST + URLEncoder.encode(address, REQUEST_URL_ENCODING_CHARSET), mContactsRequestListeners);
            } else {
                // Older SDK versions.
                buildContactsRequest(GET_CONTACTS_REQUEST + address, mContactsRequestListeners);
            }
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is always supported, this should never happen but fall back anyway
            buildContactsRequest(GET_CONTACTS_REQUEST + address, mContactsRequestListeners);
        }
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
                        boolean isDistrictSplit = false;
                        boolean isLowAccuracy = false;
                        try {
                            locationName = response.getString("location");
                            if (response.has("isSplit")) {
                                isDistrictSplit = response.getBoolean("isSplit");
                            }
                            if (response.has("lowAccuracy")) {
                                isLowAccuracy = response.getBoolean("lowAccuracy");
                            }
                        } catch (JSONException e) {
                            for (ContactsRequestListener listener : listeners) {
                                listener.onJsonError();
                            }
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

                        String districtId = "";
                        boolean stateChanged = false;
                        try {
                            String state = response.getString("state");
                            String district = response.getString("district");
                            if (!TextUtils.isEmpty(state) && !TextUtils.isEmpty(district)) {
                                // Check if state has changed
                                String currentState = AccountManager.Instance.getState(mContext);
                                stateChanged = !state.equals(currentState);
                                
                                // Store state and district separately
                                AccountManager.Instance.setState(mContext, state);
                                AccountManager.Instance.setDistrict(mContext, district);
                                
                                districtId = state + "-" + district;
                                if (OneSignal.isInitialized()) {
                                    OneSignal.getUser().addTag("districtID", districtId);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        for (ContactsRequestListener listener : listeners) {
                            listener.onContactsReceived(locationName, districtId, isDistrictSplit,
                                    isLowAccuracy, contacts, stateChanged);
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (error.networkResponse != null && error.networkResponse.statusCode == 400) {
                        // Address error. We reached the server but it couldn't create a response.
                        for (ContactsRequestListener listener : listeners) {
                            listener.onAddressError();
                        }
                        return;
                    }
                    for (ContactsRequestListener listener : listeners) {
                        listener.onRequestError();
                    }
                }
            });
            contactsRequest.setTag(TAG);
            // Add the request to the RequestQueue.
            mRequestQueue.add(contactsRequest);
    }

    public void getReport() {
        JsonObjectRequest reportRequest = new JsonObjectRequest(
                Request.Method.GET, GET_REPORT, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    int count = response.getInt("count");
                    boolean donateOn = response.getBoolean("donateOn");
                    for (CallRequestListener listener : mCallRequestListeners) {
                        listener.onReportReceived(count, donateOn);
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
                params.put("via", (BuildConfig.DEBUG && TESTING) ? "test" : "android");
                params.put("callerid", mCallerId);
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

    public void newsletterSubscribe(String email, NewsletterSubscribeCallback callback) {
        StringRequest request = new StringRequest(Request.Method.POST, NEWSLETTER_SUBSCRIBE,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        callback.onSuccess();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callback.onError();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("email", email);
                params.put("tag", "android");
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

    public void reportSearch(String searchTerm) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("query", searchTerm);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, SEARCH_TRACKING, jsonBody, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    // Search report successful - no action needed
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.w(TAG, "Search tracking failed: " + error.getMessage());
                }
            });
            request.setTag(TAG);
            // Add the request to the RequestQueue.
            mRequestQueue.add(request);
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create search tracking JSON: " + e.getMessage());
        }
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
