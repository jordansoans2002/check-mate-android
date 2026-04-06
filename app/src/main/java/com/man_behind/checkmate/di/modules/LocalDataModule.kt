package com.man_behind.checkmate.di.modules

import android.content.Context
import androidx.room3.Room
import com.man_behind.checkmate.data.local.db.DatabaseService
import com.man_behind.checkmate.di.qualifier.DatabaseInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalDataModule {

    @Provides
    @Singleton
    @DatabaseInfo
    fun provideDatabaseName(): String {
        return "checkmate-db"
    }

    @Provides
    @Singleton
    fun provideDatabaseService(
        @ApplicationContext context: Context,
        @DatabaseInfo dbName: String
    ): DatabaseService = Room.databaseBuilder(
        context,
        DatabaseService::class.java,
        dbName
    ).build()

}