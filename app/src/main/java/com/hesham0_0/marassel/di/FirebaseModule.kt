package com.hesham0_0.marassel.di

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides Firebase service singletons.
 *
 * Both FirebaseDatabase and FirebaseStorage are thread-safe singletons
 * provided by the Firebase SDK — we wrap them here to make them injectable
 * and easily replaceable with fakes in tests.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /**
     * Provides the FirebaseDatabase instance pointing to the root of the
     * Realtime Database. Persistence is enabled so the app can read cached
     * data while offline and sync when connectivity is restored.
     *
     * NOTE: setPersistenceEnabled must be called before any other Database
     * usage — doing it here in the singleton provider guarantees it runs once.
     */
    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return Firebase.database.apply {
            setPersistenceEnabled(true)
        }
    }

    /**
     * Provides the FirebaseStorage instance.
     * Used for uploading and downloading media (images/videos).
     */
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return Firebase.storage
    }
}