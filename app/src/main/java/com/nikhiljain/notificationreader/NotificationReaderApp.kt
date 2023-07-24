package com.nikhiljain.notificationreader

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.nikhiljain.notificationreader.ui.services.NotificationReaderService

class NotificationReaderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NotificationReaderService.CHANNEL_ID,
                NOTIFICATION_READER_SERVICE,
                NotificationManager.IMPORTANCE_LOW,
            )
        )
    }

    companion object {
        private const val NOTIFICATION_READER_SERVICE = "notification_reader_service"
    }
}