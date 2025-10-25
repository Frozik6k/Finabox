package ru.frozik6k.finabox.repository

import kotlinx.coroutines.flow.Flow
import ru.frozik6k.finabox.db.AppDatabase
import ru.frozik6k.finabox.entity.Box
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BoxRepository @Inject constructor(private val db: AppDatabase) {
    val boxes: Flow<List<Box>> = db.boxDao().observeAll()
}