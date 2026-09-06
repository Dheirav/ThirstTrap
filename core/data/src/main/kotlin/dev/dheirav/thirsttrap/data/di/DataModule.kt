package dev.dheirav.thirsttrap.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.dheirav.thirsttrap.data.PlantRepositoryImpl
import dev.dheirav.thirsttrap.data.ReminderRepositoryImpl
import dev.dheirav.thirsttrap.data.ThirstTrapDatabase
import dev.dheirav.thirsttrap.data.dao.CareEventDao
import dev.dheirav.thirsttrap.data.dao.PlantDao
import dev.dheirav.thirsttrap.data.dao.ReminderDao
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.ReminderRepository
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
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindPlantRepository(impl: PlantRepositoryImpl): PlantRepository

    @Binds
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository
}
