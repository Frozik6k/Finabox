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
import ru.frozik6k.finabox.data.entities.BoxDb
import ru.frozik6k.finabox.data.entities.ThingDb
import ru.frozik6k.finabox.data.storage.dao.BoxDao
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
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    appScope.launch {
                        createSampleCatalogs().forEach { catalog ->
                            database.boxDao().insertBox(catalog.box)
                            catalog.things.forEach { thing ->
                                database.thingDao().insertThing(thing)
                            }
                        }
                    }
                }
            })
        database = builder.build()

        return database
    }

    private fun createSampleCatalogs(): List<SampleCatalog> {
        val now = Instant.now()

        fun List<String>.toThings(boxName: String): List<ThingDb> = mapIndexed { index, title ->
            ThingDb(
                name = title,
                description = "${title} — проверенный предмет из каталога \"$boxName\"",
                box = boxName,
                expirationDate = now.plus(((index + 1) * 10).toLong(), ChronoUnit.DAYS)
            )
        }

        return listOf(
            SampleCatalog(
                box = BoxDb(
                    name = "Путешествия",
                    description = "Снаряжение и вещи, которые всегда беру с собой в поездки",
                    box = null
                ),
                things = listOf(
                    "Дорожный чемодан",
                    "Универсальный адаптер",
                    "Надувная подушка"
                ).toThings("Путешествия")
            ),
            SampleCatalog(
                box = BoxDb(
                    name = "Хобби",
                    description = "Инструменты и материалы для творчества",
                    box = null
                ),
                things = listOf(
                    "Акварельные краски",
                    "Альбом для скетчей",
                    "Набор кистей"
                ).toThings("Хобби")
            ),
            SampleCatalog(
                box = BoxDb(
                    name = "Дом",
                    description = "Любимые предметы для уюта и порядка",
                    box = null
                ),
                things = listOf(
                    "Тёплый плед",
                    "Ароматическая свеча",
                    "Короб для хранения"
                ).toThings("Дом")
            )
        )
    }

    @Provides
    fun provideThingDao(database: AppDatabase): ThingDao = database.thingDao()

    @Provides
    fun provideBoxDao(database: AppDatabase): BoxDao = database.boxDao()

    @Provides
    @Singleton
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob())

    private data class SampleCatalog(
        val box: BoxDb,
        val things: List<ThingDb>
    )

}