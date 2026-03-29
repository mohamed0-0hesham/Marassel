package com.hesham0_0.marassel

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.database.FirebaseDatabase
import com.hesham0_0.marassel.worker.NotificationChannelSetup
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MarasselApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: IllegalStateException) {
            // Firebase is not initialized, likely running in a test environment.
        }
        NotificationChannelSetup.createChannels(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .apply {
                setMinimumLoggingLevel(android.util.Log.DEBUG)
            }
            .build()
}