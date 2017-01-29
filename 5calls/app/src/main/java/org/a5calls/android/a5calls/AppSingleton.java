package org.a5calls.android.a5calls;

import android.content.Context;

/**
 * A singleton for referencing the database and JsonController.
 */
public class AppSingleton {

    private static AppSingleton sSingleton;
    private final Context mContext;
    private DatabaseHelper mDatabaseHelper;
    private JsonController mJsonController;

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

    public JsonController getJsonController() {
        if (mJsonController == null) {
            mJsonController = new JsonController(mContext);
        }
        return mJsonController;
    }
}
