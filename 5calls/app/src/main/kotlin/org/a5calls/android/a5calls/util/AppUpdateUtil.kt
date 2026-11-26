package org.a5calls.android.a5calls.util

import android.app.Activity.RESULT_OK
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import org.a5calls.android.a5calls.AppSingleton
import org.a5calls.android.a5calls.R

class AppUpdateUtil(
    private val activity: AppCompatActivity,
) {
    private val applicationContext = activity.applicationContext
    private var installStateUpdatedListener: InstallStateUpdatedListener? = null
    private val appUpdateManager = getAppUpdateManager()

    private fun getAppUpdateManager(): AppUpdateManager {
        val aum = AppSingleton.getInstance(applicationContext).getAppUpdateManager(activity)
        if (APP_UPDATE_DEBUG_MODE && aum is FakeAppUpdateManager) {
            val fakeAUM: FakeAppUpdateManager = aum
            val packageInfo = applicationContext.packageManager.getPackageInfo(
                applicationContext.packageName,
                PackageManager.GET_META_DATA
            )
            // Uncomment if you want to fake having the next version available
            // val currentVersion =
            //     androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(packageInfo)

            // Uncomment if you want the update priority to be IMMEDIATE
            // fakeAUM.setUpdateAvailable((currentVersion + 1).toInt(), AppUpdateType.IMMEDIATE)
            // fakeAUM.setUpdatePriority(5)

            // Uncomment if you want the update priority to be FLEXIBLE
            // fakeAUM.setUpdateAvailable((currentVersion + 1).toInt(), AppUpdateType.FLEXIBLE)
            // fakeAUM.setUpdatePriority(3)
            // fakeAUM.setClientVersionStalenessDays(DAYS_FOR_FLEXIBLE_UPDATE+1)
        }
        return aum
    }

    private val activityResultLauncher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result: ActivityResult ->
            Log.i(TAG, "ActivityResultCallback result == $result")
            // handle callback
            if (result.resultCode != RESULT_OK) {
                Log.e(TAG, "Update flow failed! Result code: ${result.resultCode}")
                // If the update is canceled or fails,
                // you can request to start the update again.
            }
        }

    fun checkInAppUpdate() {
        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(activity) { appUpdateInfo ->
            onAppUpdateSuccess(appUpdateInfo)
        }.addOnFailureListener(activity) { e ->
            Log.w(TAG, "appUpdateInfoTask Failed: $e")
        }.addOnCanceledListener(activity) {
            Log.w(TAG, "appUpdateInfoTask Cancelled")
        }.addOnCompleteListener(activity) {
            Log.i(TAG, "appUpdateInfoTask Completed")
        }
    }

    @VisibleForTesting
    fun onAppUpdateSuccess(
        appUpdateInfo: AppUpdateInfo
    ) {
        Log.i(
            TAG,
            "appUpdateInfoTask Succeeded: " +
                    "appUpdateInfo.availableVersionCode == ${appUpdateInfo.availableVersionCode()}, " +
                    "appUpdateInfo.updateAvailability() == ${appUpdateInfo.updateAvailability()}, " +
                    "appUpdateInfo.updatePriority() == ${appUpdateInfo.updatePriority()}, " +
                    "appUpdateInfo.clientVersionStalenessDays() == ${appUpdateInfo.clientVersionStalenessDays()}"
        )
        if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
            popupSnackbarForCompleteUpdate()
            return
        }
        var appUpdateType: Int? = null
        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
            if ((appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE))
                && (appUpdateInfo.updatePriority() >= IMMEDIATE_UPDATE_PRIORITY)
            ) {
                appUpdateType = AppUpdateType.IMMEDIATE
            } else if ((appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE))
                && (appUpdateInfo.updatePriority() >= FLEXIBLE_UPDATE_PRIORITY)
                && ((appUpdateInfo.clientVersionStalenessDays() ?: -1) >= DAYS_FOR_FLEXIBLE_UPDATE)
            ) {
                appUpdateType = AppUpdateType.FLEXIBLE
            }
            appUpdateType?.let {
                startUpdate(appUpdateInfo, appUpdateType)
            }
        }
    }

    private fun startUpdate(
        appUpdateInfo: AppUpdateInfo,
        appUpdateType: Int
    ) {
        Log.i(TAG, "startUpdate($appUpdateType")
        // Before starting an update, register a listener for updates.
        val listener = installStateUpdatedListener()
        installStateUpdatedListener = listener
        appUpdateManager.registerListener(listener)
        // Start an update.
        try {
            Log.d(TAG, "calling startUpdateFlowForResult")
            if (appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activityResultLauncher,
                    AppUpdateOptions.newBuilder(appUpdateType).build()
                )
            ) {
                // This is only useful for faking FLEXIBLE flow
                if (APP_UPDATE_DEBUG_MODE && appUpdateManager is FakeAppUpdateManager) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.d(
                            TAG,
                            "Debugging update flow:"
                                    + " ${appUpdateManager.isImmediateFlowVisible}"
                                    + "/${appUpdateManager.isConfirmationDialogVisible}"
                        )
                        appUpdateManager.userAcceptsUpdate()
                        appUpdateManager.downloadStarts()
                        appUpdateManager.downloadCompletes()
                        Log.d(TAG, "${appUpdateManager.isInstallSplashScreenVisible}")
                    }, 5000L)
                }
            } else {
                Log.w(TAG, "startUpdateFlowForResult failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "startUpdateFlowForResult: Failed to start update flow", e)
        }
    }

    private fun installStateUpdatedListener(): InstallStateUpdatedListener =
        InstallStateUpdatedListener { state ->
            Log.d(TAG, "InstallStateUpdatedListener state = $state")
            // (Optional) Provide a download progress bar.
            if (state.installStatus() == InstallStatus.DOWNLOADING) {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytesToDownload = state.totalBytesToDownload()
                Log.d(
                    TAG,
                    "InstallStateUpdatedListener bytesDownloaded = $bytesDownloaded, totalBytesToDownload = $totalBytesToDownload"
                )
                // Could show update progress bar.
            } else {
                val parameters = HashMap<String, String>()
                parameters["state"] = state.toString()
                Log.i(TAG, "StateUpdatedListener $parameters")
                if (state.installStatus() == InstallStatus.DOWNLOADED) {
                    // After the update is downloaded, show a notification
                    // and request user confirmation to restart the app.
                    popupSnackbarForCompleteUpdate()
                } else if (state.installStatus() == InstallStatus.FAILED) {
                    popupSnackbarForRetryUpdate()
                }
            }
        }

    private fun popupSnackbarForCompleteUpdate() {
        Snackbar.make(
            activity.findViewById(R.id.activity_main),
            applicationContext.getString(R.string.an_update_has_just_been_downloaded),
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction(applicationContext.getString(R.string.app_update_download_restart_action))
            {
                appUpdateManager.completeUpdate()
            }
            show()
        }
    }

    private fun popupSnackbarForRetryUpdate() {
        Snackbar.make(
            activity.findViewById(R.id.activity_main),
            applicationContext.getString(R.string.unable_to_download_update),
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction(context.getString(R.string.app_update_download_failed_retry_action))
            {
                checkInAppUpdate()
            }
            show()
        }
    }

    fun onResume() {
        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener(activity) { appUpdateInfo ->
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    popupSnackbarForCompleteUpdate()
                } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            activityResultLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to resume update flow", e)
                    }
                }
            }
    }

    fun onStop() {
        installStateUpdatedListener?.let {
            appUpdateManager.unregisterListener(it)
        }
    }

    companion object {
        private const val TAG = "AppUpdateUtil"
        private const val DAYS_FOR_FLEXIBLE_UPDATE = 7
        private const val FLEXIBLE_UPDATE_PRIORITY = 2
        private const val IMMEDIATE_UPDATE_PRIORITY = 4

        // Set this to true if you want to debug App Updating
        private const val APP_UPDATE_DEBUG_MODE = false
    }
}
