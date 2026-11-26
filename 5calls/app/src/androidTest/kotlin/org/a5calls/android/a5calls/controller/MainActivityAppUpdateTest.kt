package org.a5calls.android.a5calls.controller

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import org.a5calls.android.a5calls.AppSingleton
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for MainActivity and AppUpdateUtil integration.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityAppUpdateTest : MainActivityBaseTest() {
    private lateinit var mFakeAppUpdateManager: FakeAppUpdateManager
    private lateinit var mLifecycleCallback: Application.ActivityLifecycleCallbacks

    @Before
    override fun setUp() {
        super.setUp()
        setupMockRequestQueue()
    }

    @After
    override fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (::mLifecycleCallback.isInitialized) {
            (context.applicationContext as Application).unregisterActivityLifecycleCallbacks(mLifecycleCallback)
        }
        AppSingleton.getInstance(context).setAppUpdateManager(null)
        super.tearDown()
    }

    private fun registerInterceptor(onIntercept: (MainActivity) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as Application
        mLifecycleCallback = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is MainActivity) {
                    mFakeAppUpdateManager = FakeAppUpdateManager(activity)
                    // Intercept between onCreate and onResume to insert FakeAppUpdateManager
                    AppSingleton.getInstance(context).setAppUpdateManager(mFakeAppUpdateManager)
                    onIntercept(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        }
        app.registerActivityLifecycleCallbacks(mLifecycleCallback)
    }

    @Test
    fun testMainActivityLaunch_noUpdate_Available() {
        registerInterceptor {
            mFakeAppUpdateManager.setUpdateNotAvailable()
        }
        launchMainActivity(1000)
        // Validate that there is no update prompt to the user.
        assertFalse(mFakeAppUpdateManager.isConfirmationDialogVisible)
        assertFalse(mFakeAppUpdateManager.isImmediateFlowVisible)
    }

    @Test
    fun testMainActivityLaunch_flexibleUpdate_Available() {
        registerInterceptor {
            mFakeAppUpdateManager.setUpdateAvailable(999, AppUpdateType.FLEXIBLE)
            mFakeAppUpdateManager.setClientVersionStalenessDays(100)
            mFakeAppUpdateManager.setUpdatePriority(3)
        }
        launchMainActivity(1000)
        // Validate that flexible update is prompted to the user.
        assertTrue(mFakeAppUpdateManager.isConfirmationDialogVisible)
    }

    @Test
    fun testMainActivityLaunch_flexibleUpdate_Completes() {
        registerInterceptor {
            mFakeAppUpdateManager.setUpdateAvailable(999, AppUpdateType.FLEXIBLE)
            mFakeAppUpdateManager.setClientVersionStalenessDays(100)
            mFakeAppUpdateManager.setUpdatePriority(3)
        }
        launchMainActivity(1000)
        // Validate that flexible update is prompted to the user.
        assertTrue(mFakeAppUpdateManager.isConfirmationDialogVisible)

        // Simulate user's and download behavior.
        mFakeAppUpdateManager.userAcceptsUpdate()
        mFakeAppUpdateManager.downloadStarts()
        mFakeAppUpdateManager.downloadCompletes()

        // Complete the update process.
        mFakeAppUpdateManager.completeUpdate()

        // Validate that update is completed and app is restarted.
        assertTrue(mFakeAppUpdateManager.isInstallSplashScreenVisible)

        mFakeAppUpdateManager.installCompletes()
    }

    @Test
    fun testMainActivityLaunch_immediateUpdate_Available() {
        registerInterceptor {
            mFakeAppUpdateManager.setUpdateAvailable(999, AppUpdateType.IMMEDIATE)
            mFakeAppUpdateManager.setUpdatePriority(5)
        }
        launchMainActivity(1000)
        // Validate that immediate update is prompted to the user.
        assertTrue(mFakeAppUpdateManager.isImmediateFlowVisible)
    }

    @Test
    fun testMainActivityLaunch_immediateUpdate_Completes() {
        // Setup immediate update.
        registerInterceptor {
            mFakeAppUpdateManager.setUpdateAvailable(999, AppUpdateType.IMMEDIATE)
            mFakeAppUpdateManager.setUpdatePriority(5)
        }
        launchMainActivity(1000)
        // Validate that immediate update is prompted to the user.
        assertTrue(mFakeAppUpdateManager.isImmediateFlowVisible)

        // Simulate user's and download behavior.
        mFakeAppUpdateManager.userAcceptsUpdate()
        mFakeAppUpdateManager.downloadStarts()
        mFakeAppUpdateManager.downloadCompletes()

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        // Validate that update is completed and app is restarted.
        assertTrue(mFakeAppUpdateManager.isInstallSplashScreenVisible)
    }

    @Test
    fun testMainActivityLaunch_flexibleUpdate_DownloadFails() {
        registerInterceptor {
            mFakeAppUpdateManager.setUpdateAvailable(999, AppUpdateType.FLEXIBLE)
            mFakeAppUpdateManager.setClientVersionStalenessDays(100)
            mFakeAppUpdateManager.setUpdatePriority(3)
        }
        launchMainActivity(1000)
        // Validate that flexible update is prompted to the user.
        assertTrue(mFakeAppUpdateManager.isConfirmationDialogVisible)

        // Simulate user's and download behavior.
        mFakeAppUpdateManager.userAcceptsUpdate()
        mFakeAppUpdateManager.downloadStarts()
        mFakeAppUpdateManager.downloadFails()

        // Validate that update is not completed and app is not restarted.
        assertFalse(mFakeAppUpdateManager.isInstallSplashScreenVisible)
        assertFalse(mFakeAppUpdateManager.isConfirmationDialogVisible)
    }
}
