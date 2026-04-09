package com.redn.farm.data.local.dao

import androidx.room.*
import com.redn.farm.data.local.entity.PresetActivationLogEntity
import com.redn.farm.data.local.entity.PricingPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PricingPresetDao {

    @Insert
    suspend fun insert(preset: PricingPresetEntity)

    @Query("SELECT * FROM pricing_presets ORDER BY saved_at DESC")
    fun getAllPresets(): Flow<List<PricingPresetEntity>>

    @Query("SELECT * FROM pricing_presets WHERE preset_id = :presetId")
    suspend fun getById(presetId: String): PricingPresetEntity?

    @Query("SELECT * FROM pricing_presets WHERE is_active = 1 LIMIT 1")
    fun getActivePreset(): Flow<PricingPresetEntity?>

    @Query("SELECT * FROM pricing_presets WHERE is_active = 1 LIMIT 1")
    suspend fun getActivePresetOnce(): PricingPresetEntity?

    @Query("UPDATE pricing_presets SET is_active = 0")
    suspend fun deactivateAll()

    @Query("""
        UPDATE pricing_presets
        SET is_active = 1, activated_at = :activatedAt, activated_by = :activatedBy
        WHERE preset_id = :presetId
    """)
    suspend fun activate(presetId: String, activatedAt: Long, activatedBy: String)

    /**
     * Atomically deactivates all presets, activates [preset], and appends a log row.
     * Call this instead of calling [deactivateAll] + [activate] + [PresetActivationLogDao.insert]
     * separately so the operation is a single transaction.
     */
    @Transaction
    suspend fun activatePreset(
        presetId: String,
        activatedAt: Long,
        activatedBy: String,
        previousActiveId: String?,
        logDao: PresetActivationLogDao
    ) {
        deactivateAll()
        activate(presetId, activatedAt, activatedBy)
        logDao.insert(
            PresetActivationLogEntity(
                activated_at = activatedAt,
                activated_by = activatedBy,
                preset_id_activated = presetId,
                preset_id_deactivated = previousActiveId
            )
        )
    }

    @Query("DELETE FROM pricing_presets WHERE preset_id = :presetId")
    suspend fun deleteById(presetId: String)

    @Query("DELETE FROM pricing_presets")
    suspend fun truncate()

    /** EXP-US-02 / Stream C: activation log then presets in one transaction. */
    @Transaction
    suspend fun truncatePresetsAndActivationLog(logDao: PresetActivationLogDao) {
        logDao.truncate()
        truncate()
    }
}
