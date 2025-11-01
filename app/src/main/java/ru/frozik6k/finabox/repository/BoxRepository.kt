package ru.frozik6k.finabox.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.frozik6k.finabox.db.dao.BoxDao
import ru.frozik6k.finabox.entity.Box
import ru.frozik6k.finabox.entity.FotoBox
import ru.frozik6k.finabox.entity.pojo.BoxWithFotos
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BoxRepository @Inject constructor(
    private val boxDao: BoxDao
) {
    val boxesWithFotos: Flow<List<BoxWithFotos>> = boxDao.getAllBoxesWithFotos()

    suspend fun insertBox(box: Box) = withContext(Dispatchers.IO) {
        boxDao.insertBox(box)
    }

    suspend fun insertFoto(fotos: List<FotoBox>) = withContext(Dispatchers.IO) {
        boxDao.insertFotos(fotos)
    }
}