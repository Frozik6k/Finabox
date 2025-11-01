package ru.frozik6k.finabox.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.frozik6k.finabox.converter.InstantConverters
import ru.frozik6k.finabox.entity.Box
import ru.frozik6k.finabox.entity.Thing
import ru.frozik6k.finabox.db.dao.BoxDao
import ru.frozik6k.finabox.db.dao.ThingDao
import ru.frozik6k.finabox.entity.FotoBox
import ru.frozik6k.finabox.entity.FotoThing

@Database(entities = [Thing::class, Box::class, FotoBox::class, FotoThing::class], version = 1)
@TypeConverters(InstantConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun thingDao(): ThingDao
    abstract fun boxDao(): BoxDao
}