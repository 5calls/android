package org.a5calls.android.a5calls;

import android.content.Context;
import android.util.Log;

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
import java.util.List;

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
        void onIssuesReceived(List<Issue> issues);
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
        String url = GET_REQUEST + code;
        // Request a JSON Object response from the provided URL.
        JsonObjectRequest statusRequest = new JsonObjectRequest(
                com.android.volley.Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
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
                        /*} catch (JSONException e) {
                            mStatusListener.onJsonError();
                            e.printStackTrace();
                        }*/
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
