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

    @Transaction
    suspend fun createBoxWithFotos(
        box: BoxDb,
        fotoPaths: List<String>
    ): Long {
        val id = insertBox(box)
        insertFotos(fotoPaths.map { path -> FotoBoxDb(path = path, boxId = id) })
        return id
    }

    @Transaction
    suspend fun updateBoxWithFotos(
        box: BoxDb,
        fotoPaths: List<String>
    ) {
        updateBox(box)
        deleteFotosForBox(box.id)
        insertFotos(fotoPaths.map { path -> FotoBoxDb(path = path, boxId = box.id) })
    }

    @Insert
    suspend fun insertBox(box: BoxDb): Long
    @Insert
    suspend fun insertFotos(fotos: List<FotoBoxDb>)
    @Update
    suspend fun updateBox(box: BoxDb)
    @Delete
    suspend fun deleteBox(box: BoxDb)

    @Query("DELETE FROM foto_box WHERE boxId = :boxId")
    suspend fun deleteFotosForBox(boxId: Long)


    @Transaction
    @Query("SELECT * FROM boxes WHERE id = :id")
    suspend fun getBoxWithFotos(id: Long): BoxWithFotos

    @Transaction
    @Query(
        "SELECT * FROM boxes WHERE (:boxName IS NULL AND box IS NULL) OR box = :boxName ORDER BY created_at DESC"
    )
    fun observeBoxesInBox(boxName: String?): Flow<List<BoxWithFotos>>

    @Query("SELECT * FROM boxes WHERE name = :name LIMIT 1")
    fun findByName(name: String): BoxWithFotos

    @Query("SELECT * FROM boxes WHERE box = :boxName")
    suspend fun getBoxesByParent(boxName: String): List<BoxDb>


}