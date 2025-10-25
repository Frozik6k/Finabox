package ru.frozik6k.finabox.repository

import kotlinx.coroutines.flow.Flow
import ru.frozik6k.finabox.db.AppDatabase
import ru.frozik6k.finabox.entity.Thing
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThingRepository @Inject constructor(private val db: AppDatabase) {
    val things: Flow<List<Thing>> = db.thingDao().observeAll()
}