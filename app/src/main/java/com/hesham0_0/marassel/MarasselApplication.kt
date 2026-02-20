package com.hesham0_0.marassel

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import jakarta.inject.Inject

class MarasselApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .apply {
                setMinimumLoggingLevel(android.util.Log.DEBUG)
            }
            .build()
}