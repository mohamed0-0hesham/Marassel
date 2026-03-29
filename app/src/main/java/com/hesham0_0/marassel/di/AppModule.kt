package com.hesham0_0.marassel.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.hesham0_0.marassel.core.network.ConnectivityNetworkMonitor
import com.hesham0_0.marassel.core.network.NetworkMonitor
import com.hesham0_0.marassel.domain.usecase.user.UsernameValidator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserProfileStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MessageQueueStore

private val Context.userProfileStore: DataStore<Preferences>
        by preferencesDataStore(name = "user_profiles")

private val Context.messageQueueStore: DataStore<Preferences>
        by preferencesDataStore(name = "message_queue")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @UserProfileStore
    fun provideUserProfileStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.userProfileStore

    @Provides
    @Singleton
    @MessageQueueStore
    fun provideMessageQueueStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.messageQueueStore

    @Provides
    fun provideUsernameValidator(): UsernameValidator = UsernameValidator
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(
        impl: ConnectivityNetworkMonitor,
    ): NetworkMonitor
}
