package ru.frozik6k.finabox.entity.pojo

import androidx.room.Embedded
import androidx.room.Relation
import ru.frozik6k.finabox.entity.FotoThing
import ru.frozik6k.finabox.entity.Thing

data class ThingWithFotos(
    @Embedded
    val thing: Thing,
    @Relation(
        parentColumn = "id",
        entityColumn = "thingId"
    )
    val fotos: List<FotoThing>
)
