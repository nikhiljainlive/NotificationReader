package com.nikhiljain.notificationreader.ui

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.nikhiljain.notificationreader.model.NotificationContent
import com.nikhiljain.notificationreader.ui.services.NotificationListenerExampleService
import com.nikhiljain.notificationreader.ui.services.NotificationListenerExampleService.Companion.EXTRA_NOTIFICATION_CONTENT
import com.nikhiljain.notificationreader.ui.services.NotificationReaderService
import com.nikhiljain.notificationreader.R

class MainActivity : AppCompatActivity() {
    private var interceptedNotificationImageView: ImageView? = null
    private var notificationContentBroadcastReceiver: NotificationContentBroadcastReceiver? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Here we get a reference to the image we will modify when a notification is received
        interceptedNotificationImageView =
            findViewById<View>(R.id.intercepted_notification_logo) as ImageView

        findViewById<Button>(R.id.btn_start_service).setOnClickListener {
            NotificationReaderService.startService(this)
        }

        findViewById<Button>(R.id.btn_stop_service).setOnClickListener {
            runCatching { NotificationReaderService.stopService(this) }
                .onFailure { it.printStackTrace() }
        }
        // If the user did not turn the notification listener service on we prompt him to do so
        if (!isNotificationServiceEnabled) {
            val enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog()
            enableNotificationListenerAlertDialog.show()
        }

        // Finally we register a receiver to tell the MainActivity when a notification has been received
        notificationContentBroadcastReceiver = NotificationContentBroadcastReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(NotificationListenerExampleService.NOTIFICATION_RECEIVED_ACTION)
        registerReceiver(notificationContentBroadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(notificationContentBroadcastReceiver)
    }

    /**
     * Is Notification Service Enabled.
     * Verifies if the notification listener service is enabled.
     * Got it from: https://github.com/kpbird/NotificationListenerService-Example/blob/master/NLSExample/src/main/java/com/kpbird/nlsexample/NLService.java
     *
     * @return True if enabled, false otherwise.
     */
    private val isNotificationServiceEnabled: Boolean
        get() {
            val pkgName = packageName
            val flat = Settings.Secure.getString(
                contentResolver,
                ENABLED_NOTIFICATION_LISTENERS
            )
            if (!TextUtils.isEmpty(flat)) {
                val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (i in names.indices) {
                    val cn = ComponentName.unflattenFromString(names[i])
                    if (cn != null) {
                        if (TextUtils.equals(pkgName, cn.packageName)) {
                            return true
                        }
                    }
                }
            }
            return false
        }

    /**
     * Image Change Broadcast Receiver.
     * We use this Broadcast Receiver to notify the Main Activity when
     * a new notification has arrived, so it can properly change the
     * notification image
     */
    inner class NotificationContentBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val notification = intent.getParcelableExtra<NotificationContent>(
                EXTRA_NOTIFICATION_CONTENT
            ) ?: run {
                Log.e(TAG, "onReceive: notification is null")
                return
            }

            // TODO :: unused for now
            val title = notification.title
            val text = notification.text
            val bigText = notification.bigText
            val largeIcon = notification.icon

            interceptedNotificationImageView?.setImageIcon(largeIcon)
        }
    }

    /**
     * Build Notification Listener Alert Dialog.
     * Builds the alert dialog that pops up if the user has not turned
     * the Notification Listener Service on yet.
     *
     * @return An alert dialog which leads to the notification enabling screen
     */
    private fun buildNotificationServiceAlertDialog(): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(R.string.notification_listener_service)
        alertDialogBuilder.setMessage(R.string.notification_listener_service_explanation)
        alertDialogBuilder.setPositiveButton(
            R.string.yes
        ) { _, id -> startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        alertDialogBuilder.setNegativeButton(
            R.string.no
        ) { _, id ->
            // If you choose to not enable the notification listener
            // the app. will not work as expected
        }
        return alertDialogBuilder.create()
    }

    companion object {
        private const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
        private const val ACTION_NOTIFICATION_LISTENER_SETTINGS =
            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"

        private const val TAG = "MainActivity"
    }
}