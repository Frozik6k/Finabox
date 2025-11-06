package ru.frozik6k.finabox.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.frozik6k.finabox.data.entities.ThingDb

@Entity(
    tableName = "foto_thing",
    foreignKeys = [
        ForeignKey(
            entity = ThingDb::class,
            parentColumns = ["id"],
            childColumns = ["thingId"],
            onDelete = ForeignKey.Companion.CASCADE,   // удалит записи foto при удалении user
            onUpdate = ForeignKey.Companion.NO_ACTION
        )
    ],
    indices = [Index("thingId")] // ускоряет JOIN
)
data class FotoThingDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val path: String,
    val thingId: Long
)