package org.a5calls.android.a5calls.net

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.google.firebase.messaging.FirebaseMessaging
import org.a5calls.android.a5calls.AppSingleton
import org.a5calls.android.a5calls.model.AccountManager
import org.json.JSONObject

/**
 * Tells the 5calls API about this device's FCM token so it can send us
 * notifications directly, which OneSignal used to do.
 *
 * The token is issued by our own Firebase project, so it keeps working no
 * matter who sends to it. Registration upserts on the token, so calling this
 * more than once is harmless and is how a token stays fresh.
 */
object PushRegistration {
    private const val TAG = "PushRegistration"
    private const val REGISTER_URL = "https://api.5calls.org/v1/push/register"

    private const val PREFS_NAME = "org.a5calls.android.a5calls.push"
    private const val KEY_TOKEN = "pushToken"

    /** The token we last sent, so a district change can re-send it. */
    fun getToken(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)

    private fun setToken(context: Context, token: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    /**
     * Asks FCM for the current token and registers it. Used when someone turns
     * notifications on, since onNewToken only fires when the token changes.
     */
    fun refreshToken(context: Context) {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> register(context, token) }
            .addOnFailureListener { error -> Log.w(TAG, "couldn't get an fcm token: $error") }
    }

    /** Sends a token to the API along with the caller's district, if we know it. */
    fun register(context: Context, token: String) {
        if (TextUtils.isEmpty(token)) {
            return
        }

        setToken(context, token)

        val body = JSONObject().apply {
            put("token", token)
            put("platform", "android")
            put("district", district(context))
        }

        send(context, Request.Method.POST, body)
    }

    /**
     * Re-sends the token we already have after the district changes, so
     * notifications go to the right people. No-op before we have a token.
     */
    fun updateDistrict(context: Context) {
        val token = getToken(context) ?: return
        register(context, token)
    }

    /** Drops this device's token when someone turns notifications off. */
    fun unregister(context: Context) {
        val token = getToken(context) ?: return

        val body = JSONObject().apply {
            put("token", token)
        }

        send(context, Request.Method.DELETE, body)
        setToken(context, null)
    }

    /**
     * The district in the form the API wants, e.g. CA-12. Empty when we don't
     * know it yet: the API keeps the token and just can't target it by
     * district until the next registration.
     */
    private fun district(context: Context): String {
        val state = AccountManager.Instance.getState(context)
        val district = AccountManager.Instance.getDistrict(context)

        if (TextUtils.isEmpty(state) || TextUtils.isEmpty(district)) {
            return ""
        }

        return "$state-$district"
    }

    private fun send(context: Context, method: Int, body: JSONObject) {
        val callerId = AccountManager.Instance.getCallerID(context)
        if (TextUtils.isEmpty(callerId)) {
            Log.w(TAG, "no caller id yet, skipping push registration")
            return
        }

        val request = object : JsonObjectRequest(
            method,
            REGISTER_URL,
            body,
            { Log.d(TAG, "push registration updated") },
            { error -> Log.w(TAG, "push registration failed: $error") }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> = mapOf(
                "Content-Type" to "application/json",
                "X-Caller-ID" to callerId
            )
        }

        request.tag = TAG
        AppSingleton.getInstance(context).requestQueue.add(request)
    }
}
