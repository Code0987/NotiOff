package com.ilusons.notioff.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

object NotificationChannels {
    const val STATUS_CHANNEL_ID = "notioff_status"
    private const val STATUS_CHANNEL_NAME = "NotiOff status"

    fun ensureCreated(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val existing = manager.getNotificationChannel(STATUS_CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            STATUS_CHANNEL_ID,
            STATUS_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows when NotiOff is actively silencing notifications"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }
}
