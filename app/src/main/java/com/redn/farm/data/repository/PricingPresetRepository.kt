package com.redn.farm.data.repository

import com.redn.farm.data.local.dao.PresetActivationLogDao
import com.redn.farm.data.local.dao.PricingPresetDao
import com.redn.farm.data.local.entity.PresetActivationLogEntity
import com.redn.farm.data.local.entity.PricingPresetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PricingPresetRepository @Inject constructor(
    private val pricingPresetDao: PricingPresetDao,
    private val presetActivationLogDao: PresetActivationLogDao
) {

    fun getAllPresets(): Flow<List<PricingPresetEntity>> = pricingPresetDao.getAllPresets()

    fun getActivePreset(): Flow<PricingPresetEntity?> = pricingPresetDao.getActivePreset()

    suspend fun getPresetById(presetId: String): PricingPresetEntity? =
        pricingPresetDao.getById(presetId)

    suspend fun getActivePresetOnce(): PricingPresetEntity? =
        pricingPresetDao.getActivePresetOnce()

    /**
     * Inserts a new row; caller must set [PricingPresetEntity.is_active] = false and
     * [PricingPresetEntity.saved_by] to the logged-in username (**AUTH-US-04** AC6).
     */
    suspend fun savePreset(preset: PricingPresetEntity) {
        require(!preset.is_active) { "New presets must be saved inactive" }
        pricingPresetDao.insert(preset)
    }

    suspend fun activatePreset(presetId: String, activatedBy: String) {
        val previous = pricingPresetDao.getActivePresetOnce()?.preset_id
        val now = System.currentTimeMillis()
        pricingPresetDao.activatePreset(
            presetId = presetId,
            activatedAt = now,
            activatedBy = activatedBy,
            previousActiveId = previous,
            logDao = presetActivationLogDao
        )
    }

    fun getActivationLog(): Flow<List<PresetActivationLogEntity>> =
        presetActivationLogDao.getAllLogs()

    /**
     * Removes a preset row. The active preset cannot be deleted; activate another first.
     */
    suspend fun deleteInactivePreset(presetId: String) {
        val preset = pricingPresetDao.getById(presetId)
            ?: throw IllegalArgumentException("Preset not found or was already removed.")
        check(!preset.is_active) {
            "Cannot delete the active pricing preset. Activate another preset first."
        }
        pricingPresetDao.deleteById(presetId)
    }

    /** Admin reset: clears **preset_activation_log** then **pricing_presets** (EXP-US-02). */
    suspend fun truncatePresetsAndActivationLog() {
        pricingPresetDao.truncatePresetsAndActivationLog(presetActivationLogDao)
    }
}
