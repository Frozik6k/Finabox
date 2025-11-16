package ru.frozik6k.finabox.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import ru.frozik6k.finabox.data.entities.pojo.BoxWithFotos
import ru.frozik6k.finabox.data.entities.pojo.ThingWithFotos
import ru.frozik6k.finabox.data.storage.dao.BoxDao
import ru.frozik6k.finabox.data.storage.dao.ThingDao
import ru.frozik6k.finabox.dto.CatalogDto
import ru.frozik6k.finabox.dto.CatalogType

import javax.inject.Inject

@HiltViewModel
class ThingsViewModel @Inject constructor(
    private val thingDao: ThingDao,
    private val boxDao: BoxDao,
) : ViewModel() {

    private val _path = MutableStateFlow(listOf<String?>(null))
    val path: StateFlow<List<String?>> = _path.asStateFlow()

    val currentCatalogName: String?
        get() = _path.value.lastOrNull()

    val entries: StateFlow<List<CatalogDto>> = _path
        .map { it.lastOrNull() }
        .flatMapLatest { boxName ->
            combine(
                boxDao.observeBoxesInBox(boxName),
                thingDao.observeThingsInBox(boxName)
            ) { boxes, things ->
                val boxEntries = boxes.map { it.toDto() }
                val thingEntries = things.map { it.toDto() }
                (boxEntries + thingEntries)
                    .sortedWith(compareBy({ it.type.ordinal }, { it.name.lowercase() }))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun enterCatalog(entry: CatalogDto) {
        if (entry.type != CatalogType.BOX) return
        _path.update { current -> current + entry.name }
    }

    fun navigateUp(): Boolean {
        val current = _path.value
        return if (current.size > 1) {
            _path.value = current.dropLast(1)
            true
        } else {
            false
        }
    }
}

private fun ThingWithFotos.toDto(): CatalogDto {
    val name = thing.name
    val letter = name.firstOrNull()?.uppercaseChar()?.toString() ?: ""
    return CatalogDto(
        id = thing.id,
        letter = letter,
        name = name,
        type = CatalogType.THING,
        parentBox = thing.box,
    )
}

private fun BoxWithFotos.toDto(): CatalogDto {
    val name = box.name
    val letter = name.firstOrNull()?.uppercaseChar()?.toString() ?: ""
    return CatalogDto(
        id = box.id,
        letter = letter,
        name = name,
        type = CatalogType.BOX,
        parentBox = box.box,
    )
}