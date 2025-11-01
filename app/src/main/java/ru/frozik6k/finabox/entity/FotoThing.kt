package ru.frozik6k.finabox.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "foto_thing",
    foreignKeys = [
        ForeignKey(
            entity = Thing::class,
            parentColumns = ["id"],
            childColumns = ["thingId"],
            onDelete = ForeignKey.CASCADE,   // удалит записи foto при удалении user
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("thingId")] // ускоряет JOIN
)
data class FotoThing(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val path: String,
    val thingId: Long
)
