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

        // Re-enqueue using the same unique work name so WorkManager
        // replaces any lingering FAILED work with a fresh request.
        // The ViewModel's WorkInfo bridge will pick up the new state.
        //
        // Note: we can't access injected use cases from a plain BroadcastReceiver.
        // The simplest reliable approach is to start a foreground service or
        // use a HiltWorker that is immediately re-queued here. For now we
        // broadcast a sticky Intent the ViewModel can observe, but in practice
        // the preferred pattern is to make the ViewModel own retry logic and
        // expose it via a DeepLink / notification tap → Activity intent extra.
        //
        // For the notification retry path we start the app with the localId:
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