package ru.frozik6k.finabox.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.frozik6k.finabox.entity.FotoThing
import ru.frozik6k.finabox.entity.Thing
import ru.frozik6k.finabox.entity.pojo.ThingWithFotos

@Dao
interface ThingDao {

    @Transaction
    suspend fun createThingWithFotos(
        thing: Thing,
        fotoPaths: List<String>
    ): Long {
        val id = insertThing(thing)
        insertFotos(fotoPaths.map { path -> FotoThing(path = path, thingId = id) })
        return id
    }

    @Insert suspend fun insertThing(thing: Thing): Long
    @Insert suspend fun insertFotos(fotos: List<FotoThing>)
    @Update suspend fun updateThing(thing: Thing)
    @Delete suspend fun deleteThing(thing: Thing)

    @Transaction
    @Query("SELECT * FROM things WHERE id = :id")
    suspend fun getThingWithFotos(id: Long): ThingWithFotos

    @Transaction
    @Query("SELECT * FROM things ORDER BY created_at DESC")
    fun getAllThingsWithFotos(): Flow<List<ThingWithFotos>>

    @Query("SELECT * FROM things WHERE name = :name LIMIT 1")
    fun findByName(name: String): ThingWithFotos

}
