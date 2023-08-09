package com.nikhiljain.notificationreader.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.nikhiljain.notificationreader.R
import com.nikhiljain.notificationreader.databinding.FragmentHomeBinding
import com.nikhiljain.notificationreader.model.NotificationContent
import com.nikhiljain.notificationreader.ui.services.NotificationListenerExampleService

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var binding : FragmentHomeBinding? = null
    private var interceptedNotificationImageView: ImageView? = null
    private var notificationContentBroadcastReceiver: NotificationContentBroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationContentBroadcastReceiver = NotificationContentBroadcastReceiver()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentHomeBinding.bind(view)
        val context = context ?: return
        val intentFilter = IntentFilter()
        intentFilter.addAction(NotificationListenerExampleService.NOTIFICATION_RECEIVED_ACTION)
        ContextCompat.registerReceiver(
            context,
            notificationContentBroadcastReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        context?.unregisterReceiver(notificationContentBroadcastReceiver)
    }

    inner class NotificationContentBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val notification = intent.getParcelableExtra<NotificationContent>(
                NotificationListenerExampleService.EXTRA_NOTIFICATION_CONTENT
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

    companion object {
        private const val TAG = "HomeFragment"
    }
}