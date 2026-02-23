package com.hesham0_0.marassel.worker

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hesham0_0.marassel.ui.MainActivity

object NotificationHelper {

    fun buildUploadProgressNotification(
        context: Context,
        localId: String,
        progressPct: Int,
    ): Notification {
        val cancelIntent = CancelUploadReceiver.buildIntent(context, localId)
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            localId.hashCode(),
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(context, WorkerKeys.CHANNEL_MESSAGE_SENDING)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Sending media message")
            .setContentText("Uploading… ($progressPct%)")
            .setProgress(100, progressPct, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_delete,
                "Cancel",
                cancelPendingIntent,
            )
            .build()
    }

    /**
     * Convenience overload with indeterminate progress bar (shown at worker start
     * before the first progress callback fires).
     */
    fun buildUploadIndeterminateNotification(
        context: Context,
        localId: String,
    ): Notification = NotificationCompat.Builder(context, WorkerKeys.CHANNEL_MESSAGE_SENDING)
        .setSmallIcon(android.R.drawable.stat_sys_upload)
        .setContentTitle("Sending media message")
        .setContentText("Preparing upload…")
        .setProgress(0, 0, true)   // indeterminate
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .addAction(
            android.R.drawable.ic_delete,
            "Cancel",
            PendingIntent.getBroadcast(
                context,
                localId.hashCode(),
                CancelUploadReceiver.buildIntent(context, localId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        .build()

    fun buildFailedMessageNotification(
        context: Context,
        localId: String,
        previewText: String,
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            localId.hashCode() + 1,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val retryPendingIntent = PendingIntent.getBroadcast(
            context,
            localId.hashCode() + 2,
            RetryMessageReceiver.buildIntent(context, localId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val truncated = previewText.take(40) + if (previewText.length > 40) "…" else ""

        return NotificationCompat.Builder(context, WorkerKeys.CHANNEL_MESSAGE_FAILED)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Message failed to send")
            .setContentText(truncated)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_send,
                "Retry",
                retryPendingIntent,
            )
            .setGroup(FAILED_MESSAGES_GROUP)
            .build()
    }

    /**
     * Shows (or updates) the group summary notification for failed messages.
     * Required so Android groups individual failure notifications together.
     */
    fun showFailedMessageSummary(context: Context, failedCount: Int) {
        val summary = NotificationCompat.Builder(context, WorkerKeys.CHANNEL_MESSAGE_FAILED)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("$failedCount messages failed to send")
            .setGroup(FAILED_MESSAGES_GROUP)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_FAILED_SUMMARY, summary)
        }
    }

    fun cancelUploadNotification(context: Context) {
        NotificationManagerCompat.from(context)
            .cancel(WorkerKeys.NOTIFICATION_ID_UPLOAD)
    }

    private const val FAILED_MESSAGES_GROUP      = "FAILED_MESSAGES"
    private const val NOTIFICATION_ID_FAILED_SUMMARY = 1003
}