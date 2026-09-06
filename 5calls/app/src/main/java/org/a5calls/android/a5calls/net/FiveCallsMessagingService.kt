package org.a5calls.android.a5calls.net

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.a5calls.android.a5calls.R
import org.a5calls.android.a5calls.controller.MainActivity

/**
 * Receives push notifications from FCM and shows them. OneSignal's SDK used to
 * do both the token handling and the display; this does the same work against
 * our own Firebase project and our own API.
 */
class FiveCallsMessagingService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "FiveCallsMessaging"

        /**
         * Separate from the call reminder channel so people can silence vote
         * alerts without losing their own reminders.
         */
        const val CHANNEL_ID = "5calls_push_channel"

        private const val NOTIFICATION_ID = 8288

        /** Custom data the API sends so a tap can open the right message. */
        const val MESSAGE_ID_KEY = "messageid"
    }

    /**
     * FCM hands us a new token on install, reinstall, restore, and whenever it
     * decides to rotate one. Passing it straight to the API is what keeps our
     * token list from going stale.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushRegistration.register(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // notifications from our API carry a notification block; fall back to
        // the data payload so a data-only send still shows something
        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]

        if (title.isNullOrEmpty() && body.isNullOrEmpty()) {
            Log.w(TAG, "push with no title or body, nothing to show")
            return
        }

        showNotification(title, body, message.data[MESSAGE_ID_KEY])
    }

    private fun showNotification(title: String?, body: String?, messageId: String?) {
        createChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_FROM_NOTIFICATION, true)
            if (!messageId.isNullOrEmpty()) {
                putExtra(MESSAGE_ID_KEY, messageId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            MainActivity.NOTIFICATION_REQUEST,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = Notification.Builder(this)
            .setContentTitle(title ?: getString(R.string.app_name))
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.app_icon_bw)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID)
        }

        builder.setColor(ContextCompat.getColor(this, R.color.colorPrimary))

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.push_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )

        manager.createNotificationChannel(channel)
    }
}
