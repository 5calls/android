package org.a5calls.android.a5calls;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to handle server gets and posts.
 *
 * TODO: This class needs a better name!
 */
public class JsonController {
    private static final String TAG = "JsonController";

    // DO NOT SUBMIT with DEBUG = true. This only works on an emulator running on a machine that is
    // running a local version of the 5calls go server. For more on the go server, check out
    // github.com/5calls/5calls.
    private static final boolean DEBUG = false;

    private static final String GET_ISSUES_REQUEST = "https://5calls.org/issues/?address=";

    private static final String GET_REPORT = "https://5calls.org/report";

    // This is for local testing only and shouldn't be part of prod.
    private static final String GET_REPORT_DEBUG = "http://10.0.2.2:8090/report";

    public interface RequestStatusListener {
        void onRequestError();
        void onJsonError();
        void onIssuesReceived(List<Issue> issues);
        void onCallCount(int count);
        void onCallReported();
    }

    private RequestQueue mRequestQueue;
    private RequestStatusListener mStatusListener;

    public JsonController(Context context, RequestStatusListener statusListener) {
        mRequestQueue = Volley.newRequestQueue(context);
        mStatusListener = statusListener;
    }

    public void onDestroy() {
        mRequestQueue.cancelAll(TAG);
        mRequestQueue.stop();
        mRequestQueue = null;
    }

    public void getIssuesForZip(String code) {
        String url = GET_ISSUES_REQUEST + code;
        // Request a JSON Object response from the provided URL.
        JsonObjectRequest statusRequest = new JsonObjectRequest(
                Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                //try {
                if (response != null) {
                    // TODO - populate some UI programmatically with this data.
                                /*
                                Gson gson = new Gson();
                                JSONArray jsonArray = response.optJSONArray("issues");
                                Issue[] issues = gson.fromJson(jsonArray, Issue[].class);*/

                    JSONArray jsonArray = response.optJSONArray("issues");
                    Type listType = new TypeToken<ArrayList<Issue>>(){}.getType();
                    List<Issue> issues = new Gson().fromJson(jsonArray.toString(),
                            listType);
                    mStatusListener.onIssuesReceived(issues);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mStatusListener.onRequestError();
                Log.d("Error", error.getMessage());
            }
        });
        statusRequest.setTag(TAG);
        // Add the request to the RequestQueue.
        mRequestQueue.add(statusRequest);
    }

    public void getCallCount() {
        JsonObjectRequest reportRequest = new JsonObjectRequest(
                Request.Method.GET, DEBUG ? GET_REPORT_DEBUG : GET_REPORT, null,
                new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    int count = response.getInt("count");
                    mStatusListener.onCallCount(count);
                } catch (JSONException e) {
                    mStatusListener.onJsonError();
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mStatusListener.onRequestError();
                Log.d("Error", error.getMessage());
            }
        });
        reportRequest.setTag(TAG); // TODO: same tag OK?
        // Add the request to the RequestQueue.
        mRequestQueue.add(reportRequest);
    }

    // Result is "vm", "unavailable", or "contacted"
    // https://github.com/5calls/5calls/blob/master/static/js/main.js#L221
    public void reportCall(final String issueId, final String contactId, final String result,
                           final String zip) {
        StringRequest request = new StringRequest(Request.Method.POST,
                DEBUG ? GET_REPORT_DEBUG : GET_REPORT, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                mStatusListener.onCallReported();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mStatusListener.onRequestError();
                Log.d("Error", error.getMessage());
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
}
