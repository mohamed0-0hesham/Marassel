package com.hesham0_0.marassel.di


import com.hesham0_0.marassel.data.repository.MessageRepositoryImpl
import com.hesham0_0.marassel.data.repository.UserRepositoryImpl
import com.hesham0_0.marassel.domain.repository.MessageRepository
import com.hesham0_0.marassel.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds domain repository interfaces to their concrete data-layer implementations.
 *
 * Using @Binds (instead of @Provides) is preferred here because:
 * - It generates less code (no extra wrapper function body)
 * - It clearly communicates intent: "this interface = that class"
 * - The implementations are injected via their own @Inject constructors
 *
 * Both repositories are @Singleton because they hold shared state:
 * - UserRepository wraps DataStore (single file, must not be duplicated)
 * - MessageRepository holds the in-memory pending message cache
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(
        impl: MessageRepositoryImpl
    ): MessageRepository
}