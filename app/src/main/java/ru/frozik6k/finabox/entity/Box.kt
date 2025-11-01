package ru.frozik6k.finabox.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.Instant

@Entity(tableName = "boxes")
data class Box(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val description: String,
    val box: String,
    @ColumnInfo(name = "created_at")
    val date: Instant = Instant.now()
)
