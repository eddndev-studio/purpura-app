package com.eddndev.purpura.di

import android.content.Context
import androidx.room.Room
import com.eddndev.purpura.data.local.PurpuraDatabase
import com.eddndev.purpura.data.local.dao.EventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Provee Room (06-app-architecture §7.1). SingletonComponent: una sola instancia de BD
// para toda la app.
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PurpuraDatabase =
        Room.databaseBuilder(context, PurpuraDatabase::class.java, PurpuraDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideEventDao(database: PurpuraDatabase): EventDao = database.eventDao()
}
