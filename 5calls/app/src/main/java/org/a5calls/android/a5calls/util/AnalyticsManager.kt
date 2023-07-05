package org.a5calls.android.a5calls.util

import com.wbrawner.plausible.android.Plausible
import org.a5calls.android.a5calls.BuildConfig

class AnalyticsManager {
    private val staticProps: Map<String, String> = mapOf("isAndroidApp" to "true")

    fun trackPageview(path: String) {
        if (!BuildConfig.DEBUG) {
            Plausible.pageView(path, props = staticProps)
        }
    }
}