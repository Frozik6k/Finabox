package ru.frozik6k.finabox.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "things")
data class ThingDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val box: String,
    @ColumnInfo(name = "created_at")
    val date: Instant = Instant.now(),
    @ColumnInfo(name = "expiration_date")
    val expirationDate: Instant
)