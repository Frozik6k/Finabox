package ru.frozik6k.finabox.repository.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.frozik6k.finabox.entity.Thing

@Dao
interface ThingDao {
    @Query("SELECT * FROM thing")
    fun getAll(): List<Thing>

    @Query("SELECT * FROM thing WHERE name LIMIT 1")
    fun findByName(name: String): Thing

    @Insert
    fun insertAll(vararg things: Thing)

    @Delete
    fun delete(thing: Thing)

    @Query("SELECT * FROM thing ORDER BY id DESC")
    fun observeAll(): Flow<List<Thing>>
}