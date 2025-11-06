package ru.frozik6k.finabox.data.entities.pojo

import androidx.room.Embedded
import androidx.room.Relation
import ru.frozik6k.finabox.data.entities.FotoThingDb
import ru.frozik6k.finabox.data.entities.ThingDb

data class ThingWithFotos(
    @Embedded
    val thing: ThingDb,
    @Relation(
        parentColumn = "id",
        entityColumn = "thingId"
    )
    val fotos: List<FotoThingDb>
)