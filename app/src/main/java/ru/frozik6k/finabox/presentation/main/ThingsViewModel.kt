package ru.frozik6k.finabox.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.frozik6k.finabox.data.entities.pojo.ThingWithFotos
import ru.frozik6k.finabox.data.storage.dao.ThingDao
import ru.frozik6k.finabox.dto.ThingDto
import javax.inject.Inject

@HiltViewModel
class ThingsViewModel @Inject constructor(
    thingDao: ThingDao,
) : ViewModel() {

    val things: StateFlow<List<ThingDto>> = thingDao
        .getAllThingsWithFotos()
        .map { things -> things.map { it.toDto() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}

private fun ThingWithFotos.toDto(): ThingDto {
    val name = thing.name
    val letter = name.firstOrNull()?.uppercaseChar()?.toString() ?: ""
    return ThingDto(
        letter = letter,
        name = name,
    )
}