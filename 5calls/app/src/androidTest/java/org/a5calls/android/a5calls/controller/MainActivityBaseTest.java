package org.a5calls.android.a5calls.controller;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.DatabaseHelper;
import org.a5calls.android.a5calls.net.FakeRequestQueue;
import org.a5calls.android.a5calls.net.FiveCallsApi;
import org.a5calls.android.a5calls.net.MockHttpStack;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

/**
 * Base class for MainActivity integration tests that contains shared setup and utility methods.
 */
@RunWith(AndroidJUnit4.class)
public abstract class MainActivityBaseTest {

    protected MockHttpStack mHttpStack;
    protected RequestQueue mOriginalRequestQueue;
    protected FiveCallsApi mOriginalApi;
    protected String mOriginalAddress;
    protected ActivityScenario<MainActivity> scenario;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Save original state
        mOriginalRequestQueue = AppSingleton.getInstance(context).getRequestQueue();
        mOriginalApi = AppSingleton.getInstance(context).getJsonController();

        // Save original location
        mOriginalAddress = AccountManager.Instance.getAddress(context);

        // Set a mock location to avoid location prompts
        AccountManager.Instance.setAddress(context, "90210");

        // Mark tutorial as seen to bypass onboarding screen
        AccountManager.Instance.setTutorialSeen(context, true);

        // Create mock HTTP stack
        mHttpStack = new MockHttpStack();

        // Clear all database tables
        DatabaseHelper databaseHelper = AppSingleton.getInstance(context).getDatabaseHelper();
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete("UserCallsDatabase", null, null);
        db.delete("UserIssuesTable", null, null);
        db.delete("UserContactsTable", null, null);
        db.delete("BookmarkedIssues", null, null);

        // Reset placeholder issue state.
        AccountManager.Instance.setPlaceholderIssueCalled(context, false);
        AccountManager.Instance.setShowPlaceholderIssue(context, false);
    }


    @After
    public void tearDown() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Restore original state
        AppSingleton.getInstance(context).setRequestQueue(mOriginalRequestQueue);
        AppSingleton.getInstance(context).setFiveCallsApi(mOriginalApi);

        // Restore original location
        AccountManager.Instance.setAddress(context, mOriginalAddress);

        // Close the activity scenario if it's open
        if (scenario != null) {
            scenario.close();
        }
    }

    /**
     * Sets up the mock request queue and API
     */
    protected void setupMockRequestQueue() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Create a custom RequestQueue with our mock HTTP stack
        BasicNetwork basicNetwork = new BasicNetwork(mHttpStack);
        FakeRequestQueue requestQueue = new FakeRequestQueue(basicNetwork);
        requestQueue.start();

        // Replace the app's RequestQueue with our mock
        AppSingleton.getInstance(context).setRequestQueue(requestQueue);

        // Create a new FiveCallsApi with our mock RequestQueue
        String callerId = AccountManager.Instance.getCallerID(context);
        FiveCallsApi api = new FiveCallsApi(callerId, requestQueue, context);
        AppSingleton.getInstance(context).setFiveCallsApi(api);
    }

    /**
     * Launches the MainActivity and waits for it to load
     *
     * @param waitTimeMs time to wait for the activity to load
     */
    protected void launchMainActivity(int waitTimeMs) {
        // Launch the activity
        scenario = ActivityScenario.launch(MainActivity.class);

        // Wait for processing and UI to update
        try {
            Thread.sleep(waitTimeMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}