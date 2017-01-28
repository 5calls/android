package org.a5calls.android.a5calls;

import android.content.Context;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to handle server gets and posts.
 *
 * TODO: This class needs a better name!
 */
public class JsonController {
    private static final String TAG = "JsonController";
    private static final String GET_REQUEST = "https://5calls.org/issues/?address=";

    public interface RequestStatusListener {
        void onRequestError();
        void onJsonError();
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

    public void getZip(String code) {
        String url = GET_REQUEST + code;
        // Request a JSON Object response from the provided URL.
        JsonObjectRequest statusRequest = new JsonObjectRequest(
                com.android.volley.Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // TODO - populate some UI programmatically with this data.
                            JSONArray issues = response.getJSONArray("issues");
                            JSONObject issue = issues.getJSONObject(0);
                            String name = issue.getString("name");
                            Log.d(TAG, "Got issue named " + name);
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
        statusRequest.setTag(TAG);
        // Add the request to the RequestQueue.
        mRequestQueue.add(statusRequest);
    }
}
