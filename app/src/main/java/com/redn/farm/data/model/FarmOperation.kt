package com.redn.farm.data.model

import java.time.LocalDateTime

data class FarmOperation(
    val operation_id: Int = 0,
    val operation_type: FarmOperationType,
    val operation_date: LocalDateTime = LocalDateTime.now(),
    val details: String,
    val area: String = "",
    val weather_condition: String = "",
    val personnel: String = "",
    val product_id: String? = null,
    val product_name: String = "",
    val date_created: LocalDateTime = LocalDateTime.now(),
    val date_updated: LocalDateTime = LocalDateTime.now()
)

enum class FarmOperationType {
    SOWING,
    HARVESTING,
    PESTICIDE_APPLICATION,
    FERTILIZER_APPLICATION,
    IRRIGATION,
    WEEDING,
    PRUNING,
    OTHER;

    override fun toString(): String {
        return when (this) {
            SOWING -> "Sowing"
            HARVESTING -> "Harvesting"
            PESTICIDE_APPLICATION -> "Pesticide Application"
            FERTILIZER_APPLICATION -> "Fertilizer Application"
            IRRIGATION -> "Irrigation"
            WEEDING -> "Weeding"
            PRUNING -> "Pruning"
            OTHER -> "Other"
        }
    }
} 