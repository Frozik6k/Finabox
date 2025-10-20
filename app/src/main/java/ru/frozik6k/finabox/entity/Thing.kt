package ru.frozik6k.finabox.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity
data class Thing(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val description: String,
    val box: String,
    val fotos: List<Foto>,
    @ColumnInfo(name = "created_at")
    val date: Instant = Instant.now(),
    @ColumnInfo(name = "expiration_date")
    val expirationDate: Instant
)