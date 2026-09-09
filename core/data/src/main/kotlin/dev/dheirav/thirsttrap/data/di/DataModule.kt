package dev.dheirav.thirsttrap.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.dheirav.thirsttrap.data.AmbientRepositoryImpl
import dev.dheirav.thirsttrap.data.FertilizerRepositoryImpl
import dev.dheirav.thirsttrap.data.LocationRepositoryImpl
import dev.dheirav.thirsttrap.data.ExportRepositoryImpl
import dev.dheirav.thirsttrap.data.GbifSpeciesLookupService
import dev.dheirav.thirsttrap.data.MaintenanceRepository
import dev.dheirav.thirsttrap.data.PhotoRepositoryImpl
import dev.dheirav.thirsttrap.data.WeightRepositoryImpl
import dev.dheirav.thirsttrap.data.PhotoStore
import dev.dheirav.thirsttrap.data.PlantRepositoryImpl
import dev.dheirav.thirsttrap.data.ReminderRepositoryImpl
import dev.dheirav.thirsttrap.data.SettingsRepositoryImpl
import dev.dheirav.thirsttrap.data.ThirstTrapDatabase
import dev.dheirav.thirsttrap.data.dao.AmbientDao
import dev.dheirav.thirsttrap.data.dao.FertilizerDao
import dev.dheirav.thirsttrap.data.dao.LocationDao
import dev.dheirav.thirsttrap.data.dao.CareEventDao
import dev.dheirav.thirsttrap.data.dao.ReminderDao
import dev.dheirav.thirsttrap.data.dao.PhotoDao
import dev.dheirav.thirsttrap.data.dao.WeightDao
import dev.dheirav.thirsttrap.data.dao.PlantDao
import dev.dheirav.thirsttrap.domain.AmbientRepository
import dev.dheirav.thirsttrap.domain.FertilizerRepository
import dev.dheirav.thirsttrap.domain.LocationRepository
import dev.dheirav.thirsttrap.domain.PhotoRepository
import dev.dheirav.thirsttrap.domain.WeightRepository
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.ReminderRepository
import dev.dheirav.thirsttrap.domain.SettingsRepository
import dev.dheirav.thirsttrap.domain.SpeciesLookupService
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

    @Provides fun provideWeightDao(db: ThirstTrapDatabase): WeightDao = db.weightDao()

    @Provides fun provideAmbientDao(db: ThirstTrapDatabase): AmbientDao = db.ambientDao()

    @Provides fun provideLocationDao(db: ThirstTrapDatabase): LocationDao = db.locationDao()

    @Provides fun provideFertilizerDao(db: ThirstTrapDatabase): FertilizerDao = db.fertilizerDao()

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
        weightDao: WeightDao,
        ambientDao: AmbientDao,
        fertilizerDao: FertilizerDao,
        store: PhotoStore,
    ): ExportRepositoryImpl =
        ExportRepositoryImpl(
            context, plantDao, eventDao, photoDao, reminderDao, weightDao, ambientDao,
            fertilizerDao, store,
        )

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

    @Provides
    @Singleton
    fun provideSpeciesLookupService(
        @ApplicationContext context: Context,
        settings: SettingsRepository,
    ): SpeciesLookupService = GbifSpeciesLookupService(context, settings)
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

    @Binds
    abstract fun bindWeightRepository(impl: WeightRepositoryImpl): WeightRepository

    @Binds
    abstract fun bindAmbientRepository(impl: AmbientRepositoryImpl): AmbientRepository

    @Binds
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    abstract fun bindFertilizerRepository(impl: FertilizerRepositoryImpl): FertilizerRepository
}
