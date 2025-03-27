package org.a5calls.android.a5calls;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.DatabaseHelper;
import org.a5calls.android.a5calls.net.FiveCallsApi;

/**
 * A singleton for referencing the database and FiveCallsApi.
 */
public class AppSingleton {

    private static AppSingleton sSingleton;
    private final Context mAppContext;
    private DatabaseHelper mDatabaseHelper;
    private FiveCallsApi mFiveCallsApi;
    private RequestQueue mRequestQueue;

    public static AppSingleton getInstance() {
        if (sSingleton == null) {
            throw new IllegalStateException("AppSingleton not initialized. Call getInstance(Context) first.");
        }
        return sSingleton;
    }

    public static AppSingleton getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new AppSingleton(context.getApplicationContext());
        }
        return sSingleton;
    }

    private AppSingleton(Context context) {
        mAppContext = context;
    }

    public DatabaseHelper getDatabaseHelper() {
        if (mDatabaseHelper == null) {
            mDatabaseHelper = new DatabaseHelper(mAppContext);
        }
        return mDatabaseHelper;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mAppContext);
        }
        return mRequestQueue;
    }

    public void setRequestQueue(RequestQueue requestQueue) {
        mRequestQueue = requestQueue;
    }

    public FiveCallsApi getJsonController() {
        if (mFiveCallsApi == null) {
            mFiveCallsApi = new FiveCallsApi(
                    AccountManager.Instance.getCallerID(mAppContext),
                    getRequestQueue());
        }
        return mFiveCallsApi;
    }

    public void setFiveCallsApi(FiveCallsApi api) {
        mFiveCallsApi = api;
    }
}
