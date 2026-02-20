package com.hesham0_0.marassel.di

import androidx.hilt.work.HiltWorkerFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Wires HiltWorkerFactory into the DI graph.
 *
 * WorkManager is initialized manually in ChatApplication using this factory
 * (see Configuration.Provider implementation), which allows Workers annotated
 * with @HiltWorker to receive injected dependencies via @AssistedInject.
 *
 * Without this, WorkManager would use its own default factory and Hilt
 * injection inside Workers would silently fail.
 */
@Module
@InstallIn(SingletonComponent::class)
interface WorkerModule {

    @Binds
    fun bindHiltWorkerFactory(
        factory: HiltWorkerFactory
    ): HiltWorkerFactory
}