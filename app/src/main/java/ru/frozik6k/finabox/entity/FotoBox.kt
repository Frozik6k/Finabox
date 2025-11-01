package ru.frozik6k.finabox.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "foto_box",
    foreignKeys = [
        ForeignKey(
            entity = Box::class,
            parentColumns = ["id"],
            childColumns = ["boxId"],
            onDelete = ForeignKey.CASCADE,   // удалит записи foto при удалении user
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("boxId")] // ускоряет JOIN
)
data class FotoBox(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val path: String,
    val boxId: Long
)
