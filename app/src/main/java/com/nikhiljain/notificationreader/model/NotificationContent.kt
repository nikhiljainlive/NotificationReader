package com.nikhiljain.notificationreader.model

import android.graphics.drawable.Icon
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NotificationContent(
    val icon: Icon?,
    val title: String?,
    val text: String?,
    val bigText: String?
) : Parcelable