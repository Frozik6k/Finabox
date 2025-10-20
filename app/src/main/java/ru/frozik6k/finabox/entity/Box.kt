package ru.frozik6k.finabox.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Box(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val fotos: List<Foto>
)
