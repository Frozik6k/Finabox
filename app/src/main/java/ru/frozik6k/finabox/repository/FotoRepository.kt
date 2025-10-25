package ru.frozik6k.finabox.repository

import kotlinx.coroutines.flow.Flow
import ru.frozik6k.finabox.db.AppDatabase
import ru.frozik6k.finabox.entity.Foto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FotoRepository @Inject constructor(private val db: AppDatabase) {
    val fotos: Flow<List<Foto>> = db.fotoDao().observeAll()
}