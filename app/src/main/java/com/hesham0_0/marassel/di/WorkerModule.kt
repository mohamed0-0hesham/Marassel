package com.hesham0_0.marassel.di

import android.content.Context
import androidx.work.WorkManager
import com.hesham0_0.marassel.worker.WorkInfoMessageBridge
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideWorkInfoBridge(
        workManager: WorkManager,
    ): WorkInfoMessageBridge = WorkInfoMessageBridge(workManager)
}