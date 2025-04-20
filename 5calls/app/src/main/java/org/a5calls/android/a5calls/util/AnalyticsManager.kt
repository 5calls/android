package org.a5calls.android.a5calls.util

import android.content.Context
import com.onebusaway.plausible.android.Plausible
import org.a5calls.android.a5calls.BuildConfig
import org.a5calls.android.a5calls.model.AccountManager


class AnalyticsManager {
    private lateinit var plausible: Plausible
    private val staticProps: Map<String, String> = mapOf(
        "isAndroidApp" to "true",
        "androidAppVersion" to BuildConfig.VERSION_CODE.toString())
    
    @Synchronized
    private fun getPlausible(context: Context): Plausible {
        if (!::plausible.isInitialized) {
            plausible = Plausible(context, "5calls.org")
        }
        return plausible
    }

    fun trackPageview(path: String, context: Context) {
        if (!BuildConfig.DEBUG && AccountManager.Instance.allowAnalytics(context)) {
            getPlausible(context).pageView(path, props = staticProps)
        }
    }

    fun trackPageviewWithProps(path: String, context: Context, extraProps: Map<String, String>) {
        if (!BuildConfig.DEBUG && AccountManager.Instance.allowAnalytics(context)) {
            getPlausible(context).pageView(path, path, props = extraProps + staticProps)
        }
    }
}
