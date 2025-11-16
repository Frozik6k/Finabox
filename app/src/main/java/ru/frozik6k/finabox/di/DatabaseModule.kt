package ru.frozik6k.finabox.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.frozik6k.finabox.data.entities.ThingDb
import ru.frozik6k.finabox.data.storage.dao.ThingDao
import ru.frozik6k.finabox.data.storage.database.AppDatabase
import java.time.Instant
import java.time.temporal.ChronoUnit

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        appScope: CoroutineScope
    ): AppDatabase {
        lateinit var database: AppDatabase
        val builder = Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    appScope.launch {
                        createSampleThings().forEach { thing ->
                            database.thingDao().insertThing(thing)
                        }
                    }
                }
            })
        database = builder.build()

        return database
    }

    private fun createSampleThings(): List<ThingDb> {
        val now = Instant.now()
        return (1..15).map { index ->
            ThingDb(
                name = "Событие $index",
                description = "Описание события $index",
                box = "Box ${(index % 3) + 1}",
                expirationDate = now.plus(index.toLong() * 7, ChronoUnit.DAYS)
            )
        }
    }

    @Provides
    fun provideThingDao(database: AppDatabase): ThingDao = database.thingDao()

    @Provides
    @Singleton
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob())



}