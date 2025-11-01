package ru.frozik6k.finabox.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.frozik6k.finabox.entity.Box
import ru.frozik6k.finabox.entity.FotoBox
import ru.frozik6k.finabox.entity.FotoThing
import ru.frozik6k.finabox.entity.Thing
import ru.frozik6k.finabox.entity.pojo.BoxWithFotos
import ru.frozik6k.finabox.entity.pojo.ThingWithFotos

@Dao
interface BoxDao {

    @Insert suspend fun insertBox(box: Box): Long
    @Insert suspend fun insertFotos(fotos: List<FotoBox>)
    @Update suspend fun updateBox(box: Box)
    @Delete suspend fun deleteBox(box: Box)

    @Transaction
    @Query("SELECT * FROM boxes WHERE id = :id")
    suspend fun getBoxWithFotos(id: Long): BoxWithFotos

    @Transaction
    @Query("SELECT * FROM boxes ORDER BY created_at DESC")
    fun getAllBoxesWithFotos(): Flow<List<BoxWithFotos>>

    @Query("SELECT * FROM boxes WHERE name = :name LIMIT 1")
    fun findByName(name: String): BoxWithFotos

}
