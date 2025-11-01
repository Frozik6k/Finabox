package ru.frozik6k.finabox.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.frozik6k.finabox.db.AppDatabase
import ru.frozik6k.finabox.db.dao.BoxDao
import ru.frozik6k.finabox.db.dao.ThingDao
import ru.frozik6k.finabox.entity.Box
import ru.frozik6k.finabox.entity.FotoBox
import ru.frozik6k.finabox.entity.FotoThing
import ru.frozik6k.finabox.entity.Thing
import ru.frozik6k.finabox.entity.pojo.BoxWithFotos
import ru.frozik6k.finabox.entity.pojo.ThingWithFotos
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThingRepository @Inject constructor(
    private val thingDao: ThingDao
) {
    val thingsWithFotos: Flow<List<ThingWithFotos>> = thingDao.getAllThingsWithFotos()

    suspend fun insertThing(thing: Thing) = withContext(Dispatchers.IO) {
        thingDao.insertThing(thing)
    }

    suspend fun insertFoto(fotos: List<FotoThing>) = withContext(Dispatchers.IO) {
        thingDao.insertFotos(fotos)
    }
}