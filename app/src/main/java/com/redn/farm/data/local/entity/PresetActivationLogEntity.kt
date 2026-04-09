package com.redn.farm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Append-only log of preset activation events (MGT-US-05, MGT-US-06).
 * Rows are never updated or deleted.
 */
@Entity(tableName = "preset_activation_log")
data class PresetActivationLogEntity(
    @PrimaryKey(autoGenerate = true)
    val log_id: Int = 0,
    val activated_at: Long,
    val activated_by: String,
    val preset_id_activated: String,
    val preset_id_deactivated: String? = null   // null on very first activation
)
