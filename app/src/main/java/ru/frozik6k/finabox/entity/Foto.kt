package ru.frozik6k.finabox.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Foto(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val path: String,
)