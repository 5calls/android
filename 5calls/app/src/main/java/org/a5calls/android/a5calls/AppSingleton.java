package org.a5calls.android.a5calls;

import android.content.Context;

import com.android.volley.toolbox.Volley;

import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.DatabaseHelper;
import org.a5calls.android.a5calls.net.FiveCallsApi;

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
            mFiveCallsApi = new FiveCallsApi(
                    AccountManager.Instance.getCallerID(mContext),
                    Volley.newRequestQueue(mContext));
        }
        return mFiveCallsApi;
    }
}
