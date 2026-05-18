package com.example.passagenexpress.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.passagenexpress.core.datastore.TotemConfigDataStore
import com.example.passagenexpress.core.domain.repository.TotemConfigRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

private const val DATA_STORE_FILE = "totem_config.preferences_pb"

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { context.preferencesDataStoreFile(DATA_STORE_FILE) },
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreBindModule {

    @Binds
    @Singleton
    abstract fun bindTotemConfigRepository(
        impl: TotemConfigDataStore,
    ): TotemConfigRepository
}
