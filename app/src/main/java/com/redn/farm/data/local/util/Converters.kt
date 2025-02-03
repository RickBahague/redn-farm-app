package com.redn.farm.data.local.util

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime
import com.redn.farm.data.model.FarmOperationType

class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }

    @TypeConverter
    fun fromFarmOperationType(value: FarmOperationType): String {
        return value.name
    }

    @TypeConverter
    fun toFarmOperationType(value: String): FarmOperationType {
        return FarmOperationType.valueOf(value)
    }
} 