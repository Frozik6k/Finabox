package ru.frozik6k.finabox.converter

import androidx.room.TypeConverter
import java.time.Instant

object InstantConverters {
    @TypeConverter
    @JvmStatic
    fun fromEpochMillis(value: Long?): Instant? =
        value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    @JvmStatic
    fun toEpochMillis(value: Instant?): Long? =
        value?.toEpochMilli()
}