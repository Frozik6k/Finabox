package ru.frozik6k.finabox.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "foto_box",
    foreignKeys = [
        ForeignKey(
            entity = BoxDb::class,
            parentColumns = ["id"],
            childColumns = ["boxId"],
            onDelete = ForeignKey.Companion.CASCADE,   // удалит записи foto при удалении user
            onUpdate = ForeignKey.Companion.NO_ACTION
        )
    ],
    indices = [Index("boxId")] // ускоряет JOIN
)
data class FotoBoxDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val path: String,
    val boxId: Long
)