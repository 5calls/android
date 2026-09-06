package org.a5calls.android.a5calls.controller;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.BaseIntegrationTest;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.net.FakeRequestQueue;
import org.a5calls.android.a5calls.net.FiveCallsApi;
import org.a5calls.android.a5calls.net.MockHttpStack;
import org.junit.After;
import org.junit.Before;

/**
 * Base class for MainActivity integration tests that contains shared setup and utility methods.
 */
public abstract class MainActivityBaseTest extends BaseIntegrationTest {

    protected RequestQueue mOriginalRequestQueue;
    protected FiveCallsApi mOriginalApi;
    protected String mOriginalAddress;
    protected ActivityScenario<MainActivity> scenario;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        // Save original state
        mOriginalRequestQueue = AppSingleton.getInstance(mContext).getRequestQueue();
        mOriginalApi = AppSingleton.getInstance(mContext).getJsonController();

        // Save original location
        mOriginalAddress = AccountManager.Instance.getAddress(mContext);

        // Set a mock location to avoid location prompts
        AccountManager.Instance.setAddress(mContext, "90210");

        // Mark tutorial as seen to bypass onboarding screen
        AccountManager.Instance.setTutorialSeen(mContext, true);
    }

    @After
    @Override
    public void tearDown() {
        // Restore original state
        AppSingleton.getInstance(mContext).setRequestQueue(mOriginalRequestQueue);
        AppSingleton.getInstance(mContext).setFiveCallsApi(mOriginalApi);

        // Restore original location
        AccountManager.Instance.setAddress(mContext, mOriginalAddress);

        // Close the activity scenario if it's open
        if (scenario != null) {
            scenario.close();
        }
        super.tearDown();
    }

    /**
     * Sets up the mock request queue and API
     * (Deprecated: functionality moved to BaseIntegrationTest.setUp)
     */
    protected void setupMockRequestQueue() {
        // This is now redundant but kept for backward compatibility with existing tests
    }

    /**
     * Launches the MainActivity and waits for it to load
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