package com.blackfoxis.telros.di

import android.app.Application
import androidx.room.Room
import com.blackfoxis.telros.data.local.AppDatabase
import com.blackfoxis.telros.data.local.PasswordDao
import com.blackfoxis.telros.data.repository.PasswordRepositoryImpl
import com.blackfoxis.telros.domain.repository.PasswordRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(app, AppDatabase::class.java, "password_gen_db").build()
    }

    @Provides
    @Singleton
    fun providePasswordDao(db: AppDatabase): PasswordDao = db.passwordDao()

}