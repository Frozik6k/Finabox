package ru.frozik6k.finabox.repository.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.frozik6k.finabox.entity.Foto
import ru.frozik6k.finabox.entity.Thing

@Dao
interface FotoDao {
    @Query("SELECT * FROM foto")
    fun getAll(): List<Foto>

    @Query("SELECT * FROM foto WHERE path LIMIT 1")
    fun findByPath(name: String): Foto

    @Insert
    fun insertAll(vararg fotos: Foto)

    @Delete
    fun delete(foto: Foto)

    @Query("SELECT * FROM foto ORDER BY id DESC")
    fun observeAll(): Flow<List<Foto>>
}