package org.a5calls.android.a5calls;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.a5calls.android.a5calls.model.DatabaseHelper;
import org.a5calls.android.a5calls.model.Outcome;
import org.a5calls.android.a5calls.net.FiveCallsApi;
import org.a5calls.android.a5calls.net.OkHttpStack;
import org.a5calls.android.a5calls.net.OutcomeStatusTypeAdapter;

import okhttp3.OkHttpClient;

/**
 * A singleton for referencing the database and FiveCallsApi.
 */
public class AppSingleton {

    private static AppSingleton sSingleton;
    private final Context mContext;
    private DatabaseHelper mDatabaseHelper;
    private FiveCallsApi mFiveCallsApi;

    public static final AppSingleton getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new AppSingleton(context);
        }
        return sSingleton;
    }

    private AppSingleton(Context context) {
        mContext = context;
    }

    public DatabaseHelper getDatabaseHelper() {
        if (mDatabaseHelper == null) {
            mDatabaseHelper = new DatabaseHelper(mContext);
        }
        return mDatabaseHelper;
    }

    public FiveCallsApi getJsonController() {
        if (mFiveCallsApi == null) {
            mFiveCallsApi = new FiveCallsApi(getRequestQueue(mContext), getGson());
        }
        return mFiveCallsApi;
    }

    private RequestQueue getRequestQueue(Context context) {
        return Volley.newRequestQueue(context, new OkHttpStack(new OkHttpClient()));
    }

    private Gson getGson() {
        return new GsonBuilder()
                .serializeNulls()
                .registerTypeAdapter(Outcome.Status.class, new OutcomeStatusTypeAdapter())
                .create();
    }
}
