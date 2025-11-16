package ru.frozik6k.finabox.data.storage.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.frozik6k.finabox.data.entities.FotoThingDb
import ru.frozik6k.finabox.data.entities.ThingDb
import ru.frozik6k.finabox.data.entities.pojo.ThingWithFotos

@Dao
interface ThingDao {

    @Transaction
    suspend fun createThingWithFotos(
        thing: ThingDb,
        fotoPaths: List<String>
    ): Long {
        val id = insertThing(thing)
        insertFotos(fotoPaths.map { path -> FotoThingDb(path = path, thingId = id) })
        return id
    }

    @Transaction
    suspend fun updateThingWithFotos(
        thing: ThingDb,
        fotoPaths: List<String>
    ) {
        updateThing(thing)
        deleteFotosForThing(thing.id)
        insertFotos(fotoPaths.map { path -> FotoThingDb(path = path, thingId = thing.id) })
    }


    @Insert
    suspend fun insertThing(thing: ThingDb): Long

    @Insert
    suspend fun insertFotos(fotos: List<FotoThingDb>)

    @Update
    suspend fun updateThing(thing: ThingDb)

    @Delete
    suspend fun deleteThing(thing: ThingDb)

    @Query("DELETE FROM foto_thing WHERE thingId = :thingId")
    suspend fun deleteFotosForThing(thingId: Long)

    @Transaction
    @Query("SELECT * FROM things WHERE id = :id")
    suspend fun getThingWithFotos(id: Long): ThingWithFotos

    @Transaction
    @Query(
        "SELECT * FROM things WHERE (:boxName IS NULL AND box IS NULL) OR box = :boxName ORDER BY created_at DESC"
    )
    fun observeThingsInBox(boxName: String?): Flow<List<ThingWithFotos>>

    @Query("SELECT * FROM things WHERE name = :name LIMIT 1")
    fun findByName(name: String): ThingWithFotos

    @Query("SELECT * FROM things WHERE box = :boxName")
    suspend fun getThingsByParent(boxName: String): List<ThingDb>


}