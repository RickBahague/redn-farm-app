package com.redn.farm.data.local.converters

import androidx.room.TypeConverter
import com.redn.farm.data.model.AcquisitionLocation
import com.redn.farm.data.model.CustomerType
import com.redn.farm.data.model.FarmOperationType

class EnumConverters {
    @TypeConverter
    fun toAcquisitionLocation(value: String?): AcquisitionLocation? {
        return value?.let { AcquisitionLocation.valueOf(it) }
    }

    @TypeConverter
    fun fromAcquisitionLocation(location: AcquisitionLocation?): String? {
        return location?.name
    }

    @TypeConverter
    fun toCustomerType(value: String?): CustomerType? {
        return value?.let { CustomerType.valueOf(it) }
    }

    @TypeConverter
    fun fromCustomerType(type: CustomerType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toFarmOperationType(value: String?): FarmOperationType? {
        return value?.let { FarmOperationType.valueOf(it) }
    }

    @TypeConverter
    fun fromFarmOperationType(type: FarmOperationType?): String? {
        return type?.name
    }
} 