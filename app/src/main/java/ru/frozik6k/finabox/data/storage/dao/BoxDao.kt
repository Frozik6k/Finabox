package ru.frozik6k.finabox.data.storage.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.frozik6k.finabox.data.entities.BoxDb
import ru.frozik6k.finabox.data.entities.FotoBoxDb
import ru.frozik6k.finabox.data.entities.pojo.BoxWithFotos

@Dao
interface BoxDao {

    @Insert
    suspend fun insertBox(box: BoxDb): Long
    @Insert
    suspend fun insertFotos(fotos: List<FotoBoxDb>)
    @Update
    suspend fun updateBox(box: BoxDb)
    @Delete
    suspend fun deleteBox(box: BoxDb)

    @Transaction
    @Query("SELECT * FROM boxes WHERE id = :id")
    suspend fun getBoxWithFotos(id: Long): BoxWithFotos

    @Transaction
    @Query("SELECT * FROM boxes ORDER BY created_at DESC")
    fun getAllBoxesWithFotos(): Flow<List<BoxWithFotos>>

    @Query("SELECT * FROM boxes WHERE name = :name LIMIT 1")
    fun findByName(name: String): BoxWithFotos

}