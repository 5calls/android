package org.a5calls.android.a5calls;

import android.content.Context;

/**
 * A singleton for referencing the database and JsonController.
 */
public class AppSingleton {

    private static AppSingleton sSingleton;
    private final Context mContext;
    private DatabaseController mDatabaseController;
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

    public DatabaseController getDatabaseController() {
        if (mDatabaseController == null) {
            mDatabaseController = new DatabaseController();
        }
        return mDatabaseController;
    }

    public JsonController getJsonController() {
        if (mJsonController == null) {
            mJsonController = new JsonController(mContext);
        }
        return mJsonController;
    }
}
