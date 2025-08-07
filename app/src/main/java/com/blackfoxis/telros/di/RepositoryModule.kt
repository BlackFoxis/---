package com.blackfoxis.telros.di

import com.blackfoxis.telros.data.repository.PasswordRepositoryImpl
import com.blackfoxis.telros.domain.repository.PasswordRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule { // Абстрактный класс для @Binds

    @Binds
    @Singleton
    abstract fun bindPasswordRepository(
        passwordRepositoryImpl: PasswordRepositoryImpl
    ): PasswordRepository
}