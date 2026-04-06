package com.man_behind.checkmate.data.local.db.converter

import androidx.room3.TypeConverter
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.Instant

class LocalDateTimeConverter {

    @TypeConverter
    fun toEpochMillis(date: LocalDateTime?): Long? =
        date?.atZone(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()

    @TypeConverter
    fun fromEpochMillis(epochMillis: Long?): LocalDateTime? =
        epochMillis?.let {
            Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
        }
}