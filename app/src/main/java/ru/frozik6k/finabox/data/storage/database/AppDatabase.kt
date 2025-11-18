package ru.frozik6k.finabox.data.storage.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.frozik6k.finabox.converter.InstantConverters
import ru.frozik6k.finabox.data.storage.dao.BoxDao
import ru.frozik6k.finabox.data.storage.dao.ThingDao
import ru.frozik6k.finabox.data.entities.BoxDb
import ru.frozik6k.finabox.data.entities.FotoBoxDb
import ru.frozik6k.finabox.data.entities.FotoThingDb
import ru.frozik6k.finabox.data.entities.ThingDb

@Database(entities = [ThingDb::class, BoxDb::class, FotoBoxDb::class, FotoThingDb::class], version = 2)
@TypeConverters(InstantConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun thingDao(): ThingDao
    abstract fun boxDao(): BoxDao
}