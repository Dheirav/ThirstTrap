package dev.dheirav.thirsttrap.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.dheirav.thirsttrap.data.ExportRepositoryImpl
import dev.dheirav.thirsttrap.data.MaintenanceRepository
import dev.dheirav.thirsttrap.data.PhotoRepositoryImpl
import dev.dheirav.thirsttrap.data.PhotoStore
import dev.dheirav.thirsttrap.data.PlantRepositoryImpl
import dev.dheirav.thirsttrap.data.ReminderRepositoryImpl
import dev.dheirav.thirsttrap.data.SettingsRepositoryImpl
import dev.dheirav.thirsttrap.data.ThirstTrapDatabase
import dev.dheirav.thirsttrap.data.dao.CareEventDao
import dev.dheirav.thirsttrap.data.dao.ReminderDao
import dev.dheirav.thirsttrap.data.dao.PhotoDao
import dev.dheirav.thirsttrap.data.dao.PlantDao
import dev.dheirav.thirsttrap.domain.PhotoRepository
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.ReminderRepository
import dev.dheirav.thirsttrap.domain.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ThirstTrapDatabase =
        Room.databaseBuilder(context, ThirstTrapDatabase::class.java, ThirstTrapDatabase.NAME)
            // Deliberately no fallbackToDestructiveMigration. If a migration is
            // missing the app must fail loudly rather than silently wipe a diary.
            .build()

    @Provides fun providePlantDao(db: ThirstTrapDatabase): PlantDao = db.plantDao()

    @Provides fun provideCareEventDao(db: ThirstTrapDatabase): CareEventDao = db.careEventDao()

    @Provides fun provideReminderDao(db: ThirstTrapDatabase): ReminderDao = db.reminderDao()

    @Provides fun providePhotoDao(db: ThirstTrapDatabase): PhotoDao = db.photoDao()

    @Provides
    @Singleton
    fun providePhotoStore(@ApplicationContext context: Context): PhotoStore = PhotoStore(context)

    @Provides
    @Singleton
    fun provideExportRepository(
        @ApplicationContext context: Context,
        plantDao: PlantDao,
        eventDao: CareEventDao,
        photoDao: PhotoDao,
        reminderDao: ReminderDao,
        store: PhotoStore,
    ): ExportRepositoryImpl = ExportRepositoryImpl(context, plantDao, eventDao, photoDao, reminderDao, store)

    @Provides
    @Singleton
    fun provideMaintenanceRepository(
        photoDao: PhotoDao,
        store: PhotoStore,
        db: ThirstTrapDatabase,
        @ApplicationContext context: Context,
    ): MaintenanceRepository = MaintenanceRepository(photoDao, store, db, context)

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepositoryImpl(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindPlantRepository(impl: PlantRepositoryImpl): PlantRepository

    @Binds
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds
    abstract fun bindPhotoRepository(impl: PhotoRepositoryImpl): PhotoRepository
}
