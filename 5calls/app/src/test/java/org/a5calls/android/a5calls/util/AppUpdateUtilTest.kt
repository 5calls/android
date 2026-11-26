package org.a5calls.android.a5calls.util

import android.content.Context
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import org.a5calls.android.a5calls.AppSingleton
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class AppUpdateUtilTest {

    private lateinit var activity: AppCompatActivity
    private lateinit var fakeAppUpdateManager: FakeAppUpdateManager
    private lateinit var appUpdateUtil: AppUpdateUtil

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().get()
        fakeAppUpdateManager = FakeAppUpdateManager(context)
        AppSingleton.getInstance(context).setAppUpdateManager(fakeAppUpdateManager)
        appUpdateUtil = AppUpdateUtil(activity)
    }

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppSingleton.getInstance(context).setAppUpdateManager(null)
    }

    @Test
    fun testCheck_whenNoUpdateAvailable() {
        fakeAppUpdateManager.setUpdateNotAvailable()
        appUpdateUtil.checkInAppUpdate()
        shadowOf(Looper.getMainLooper()).idle()

        assertFalse(fakeAppUpdateManager.isConfirmationDialogVisible)
        assertFalse(fakeAppUpdateManager.isImmediateFlowVisible)
    }

    @Test
    fun testCheck_whenImmediateUpdateAvailable() {
        fakeAppUpdateManager.setUpdateAvailable(2, AppUpdateType.IMMEDIATE)
        fakeAppUpdateManager.setUpdatePriority(5)

        val task = fakeAppUpdateManager.appUpdateInfo
        var updateTriggered = false
        task.addOnSuccessListener { info ->
            assertEquals(UpdateAvailability.UPDATE_AVAILABLE, info.updateAvailability())
            assertEquals(5, info.updatePriority())
            assertTrue(info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE))
            appUpdateUtil.onAppUpdateSuccess(info)
            updateTriggered = true
        }
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(updateTriggered)
    }

    @Test
    fun testOnAppUpdateSuccess_immediateUpdatePriorityTooLow() {
        fakeAppUpdateManager.setUpdateAvailable(2, AppUpdateType.IMMEDIATE)
        fakeAppUpdateManager.setUpdatePriority(1) // Less than IMMEDIATE_UPDATE_PRIORITY (4)

        val task = fakeAppUpdateManager.appUpdateInfo
        task.addOnSuccessListener { info ->
            appUpdateUtil.onAppUpdateSuccess(info)
            assertFalse(fakeAppUpdateManager.isImmediateFlowVisible)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun testOnAppUpdateSuccess_flexibleUpdateAvailable() {
        fakeAppUpdateManager.setUpdateAvailable(2, AppUpdateType.FLEXIBLE)
        fakeAppUpdateManager.setUpdatePriority(3) // >= FLEXIBLE_UPDATE_PRIORITY (2)
        fakeAppUpdateManager.setClientVersionStalenessDays(10) // >= DAYS_FOR_FLEXIBLE_UPDATE (7)

        val task = fakeAppUpdateManager.appUpdateInfo
        task.addOnSuccessListener { info ->
            appUpdateUtil.onAppUpdateSuccess(info)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun testOnAppUpdateSuccess_flexibleUpdateStalenessDaysTooLow() {
        fakeAppUpdateManager.setUpdateAvailable(2, AppUpdateType.FLEXIBLE)
        fakeAppUpdateManager.setUpdatePriority(3)
        fakeAppUpdateManager.setClientVersionStalenessDays(3) // Less than 7

        val task = fakeAppUpdateManager.appUpdateInfo
        task.addOnSuccessListener { info ->
            appUpdateUtil.onAppUpdateSuccess(info)
            assertFalse(fakeAppUpdateManager.isConfirmationDialogVisible)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun testOnResume_executesWithoutException() {
        fakeAppUpdateManager.setUpdateNotAvailable()
        appUpdateUtil.onResume()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun testOnStop_executesWithoutException() {
        appUpdateUtil.onStop()
    }
}
