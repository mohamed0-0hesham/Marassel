package com.hesham0_0.marassel.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager

class CancelUploadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val localId = intent.getStringExtra(EXTRA_LOCAL_ID) ?: return
        WorkManager.getInstance(context).cancelUniqueWork(localId)
    }

    companion object {
        private const val ACTION_CANCEL_UPLOAD =
            "com.hesham0_0.marassel.ACTION_CANCEL_UPLOAD"
        private const val EXTRA_LOCAL_ID = "extra_local_id"

        fun buildIntent(context: Context, localId: String): Intent =
            Intent(context, CancelUploadReceiver::class.java).apply {
                action = ACTION_CANCEL_UPLOAD
                putExtra(EXTRA_LOCAL_ID, localId)
            }
    }
}

class RetryMessageReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val localId = intent.getStringExtra(EXTRA_LOCAL_ID) ?: return
        val launchIntent = Intent(context, com.hesham0_0.marassel.ui.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_LOCAL_ID, localId)
            action = ACTION_RETRY_MESSAGE
        }
        context.startActivity(launchIntent)
    }

    companion object {
        const val ACTION_RETRY_MESSAGE =
            "com.hesham0_0.marassel.ACTION_RETRY_MESSAGE"
        const val EXTRA_LOCAL_ID = "extra_local_id"

        fun buildIntent(context: Context, localId: String): Intent =
            Intent(context, RetryMessageReceiver::class.java).apply {
                action = ACTION_RETRY_MESSAGE
                putExtra(EXTRA_LOCAL_ID, localId)
            }
    }
}