package ru.frozik6k.finabox

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.frozik6k.finabox.entity.Box
import ru.frozik6k.finabox.entity.Foto
import ru.frozik6k.finabox.entity.Thing
import ru.frozik6k.finabox.repository.dao.BoxDao
import ru.frozik6k.finabox.repository.dao.FotoDao
import ru.frozik6k.finabox.repository.dao.ThingDao

@Database(entities = [Thing::class, Box::class, Foto::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun thingDao(): ThingDao
    abstract fun boxDao(): BoxDao
    abstract fun fotoDao(): FotoDao
}