package com.nikhiljain.notificationreader.ui.services

import android.app.Notification
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.nikhiljain.notificationreader.model.NotificationContent

class NotificationListenerExampleService : NotificationListenerService() {
    // TODO:: Implement - Apps can be selected from Settings screen of our app
    private object ApplicationPackageNames {
        const val WHATSAPP_PACKAGE_NAME = "com.whatsapp"
    }

    companion object {
        private const val TAG = "NotificationListener"
        const val NOTIFICATION_RECEIVED_ACTION = "com.nikhiljain.notification_received"
        const val EXTRA_NOTIFICATION_CONTENT = "notificationContent"
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isNotificationFromWhatsApp(sbn)) {
            return
        }

        val title = sbn.notification.extras.getString(Notification.EXTRA_TITLE)
        val text = sbn.notification.extras.getString(Notification.EXTRA_TEXT)
        val bigText = sbn.notification.extras.getString(Notification.EXTRA_BIG_TEXT)
        val largeIcon = sbn.notification.extras.getParcelable<Icon>(Notification.EXTRA_LARGE_ICON)

        // Since we also receive notifications when Android groups the conversations
        // so we are using workaround to check if largeIcon is present,
        // then the notification should be shown
        if (largeIcon != null) {
            Log.e(TAG, "onReceive: title : $title")
            Log.e(TAG, "onReceive: text : $text")
            Log.e(TAG, "onReceive: bigText : $bigText")
            Log.e(TAG, "onReceive: extras : ${sbn.notification.extras}")

            // broadcast the notification
            val intent = Intent(NOTIFICATION_RECEIVED_ACTION)
            intent.putExtra(
                EXTRA_NOTIFICATION_CONTENT, NotificationContent(
                    largeIcon, title, text, bigText
                )
            )
            sendBroadcast(intent)
//            cancelNotification(sbn.key) // don't cancel notifications, maybe add a toggle on Settings Screen
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // ignore for now
    }

    private fun isNotificationFromWhatsApp(sbn: StatusBarNotification) =
        (sbn.packageName == ApplicationPackageNames.WHATSAPP_PACKAGE_NAME)
}