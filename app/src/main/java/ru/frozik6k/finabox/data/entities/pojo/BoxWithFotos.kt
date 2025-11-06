package ru.frozik6k.finabox.data.entities.pojo

import androidx.room.Embedded
import androidx.room.Relation
import ru.frozik6k.finabox.data.entities.BoxDb
import ru.frozik6k.finabox.data.entities.FotoBoxDb

data class BoxWithFotos(
    @Embedded
    val box: BoxDb,
    @Relation(
        parentColumn = "id",
        entityColumn = "boxId"
    )
    val fotos: List<FotoBoxDb>
)