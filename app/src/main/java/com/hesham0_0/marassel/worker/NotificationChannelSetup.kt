package com.hesham0_0.marassel.worker

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

object NotificationChannelSetup {

    fun createChannels(context: Context) {
        val manager = NotificationManagerCompat.from(context)

        // Channel 1: Ongoing send/upload progress (silent)
        val sendingChannel = NotificationChannelCompat
            .Builder(
                WorkerKeys.CHANNEL_MESSAGE_SENDING,
                NotificationManagerCompat.IMPORTANCE_LOW,
            )
            .setName("Sending Messages")
            .setDescription("Shown while messages are being sent or media is being uploaded")
            .setVibrationEnabled(false)
            .setShowBadge(false)
            .build()

        // Channel 2: Failed message alerts (with sound)
        val failedChannel = NotificationChannelCompat
            .Builder(
                WorkerKeys.CHANNEL_MESSAGE_FAILED,
                NotificationManagerCompat.IMPORTANCE_DEFAULT,
            )
            .setName("Failed Messages")
            .setDescription("Shown when a message fails to send after all retries")
            .setVibrationEnabled(true)
            .setShowBadge(true)
            .build()

        manager.createNotificationChannelsCompat(listOf(sendingChannel, failedChannel))
    }
}