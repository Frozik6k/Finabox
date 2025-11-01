package ru.frozik6k.finabox.entity.pojo

import androidx.room.Embedded
import androidx.room.Relation
import ru.frozik6k.finabox.entity.Box
import ru.frozik6k.finabox.entity.FotoBox

data class BoxWithFotos(
    @Embedded
    val box: Box,
    @Relation(
        parentColumn = "id",
        entityColumn = "boxId"
    )
    val fotos: List<FotoBox>
)
