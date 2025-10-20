package ru.frozik6k.finabox.repository.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.frozik6k.finabox.entity.Box
import ru.frozik6k.finabox.entity.Thing

@Dao
interface BoxDao {
    @Query("SELECT * FROM box")
    fun getAll(): List<Box>

    @Query("SELECT * FROM box WHERE name LIMIT 1")
    fun findByName(name: String): Box

    @Insert
    fun insertAll(vararg boxes: Box)

    @Delete
    fun delete(box: Box)

    @Query("SELECT * FROM box ORDER BY id DESC")
    fun observeAll(): Flow<List<Box>>
}