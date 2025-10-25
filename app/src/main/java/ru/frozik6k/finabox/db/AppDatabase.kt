package ru.frozik6k.finabox.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.frozik6k.finabox.entity.Box
import ru.frozik6k.finabox.entity.Foto
import ru.frozik6k.finabox.entity.Thing
import ru.frozik6k.finabox.db.dao.BoxDao
import ru.frozik6k.finabox.db.dao.FotoDao
import ru.frozik6k.finabox.db.dao.ThingDao

@Database(entities = [Thing::class, Box::class, Foto::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun thingDao(): ThingDao
    abstract fun boxDao(): BoxDao
    abstract fun fotoDao(): FotoDao
}