package com.redn.farm.ui.screens.pricing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.entity.PricingPresetEntity
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.data.pricing.PresetPreviewCalculator
import com.redn.farm.data.pricing.PricingPresetGson
import com.redn.farm.data.repository.PricingPresetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PresetActivationPreviewViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    private val repository: PricingPresetRepository
) : ViewModel() {

    private val presetId: String = checkNotNull(savedStateHandle["presetId"])
    private val sessionManager = SessionManager(context)

    private val _preset = MutableStateFlow<PricingPresetEntity?>(null)
    val preset: StateFlow<PricingPresetEntity?> = _preset.asStateFlow()

    private val _previewRows = MutableStateFlow<List<PresetPreviewCalculator.ChannelResult>>(emptyList())
    val previewRows: StateFlow<List<PresetPreviewCalculator.ChannelResult>> = _previewRows.asStateFlow()

    private val _activated = MutableStateFlow(false)
    val activated: StateFlow<Boolean> = _activated.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            val p = repository.getPresetById(presetId)
            _preset.value = p
            if (p != null) {
                try {
                    val channels = PricingPresetGson.channelsFromJson(p.channels_json)
                    _previewRows.value = PresetPreviewCalculator.previewSrpsPerKg(
                        bulkCost = 5000.0,
                        bulkQuantityKg = 100.0,
                        spoilageRate = p.spoilage_rate,
                        additionalCostPerKg = p.additional_cost_per_kg,
                        channels = channels
                    )
                } catch (e: Exception) {
                    _error.value = e.message ?: "Preview failed"
                }
            }
        }
    }

    fun confirmActivate() {
        viewModelScope.launch {
            try {
                val by = sessionManager.getUsername() ?: "admin"
                repository.activatePreset(presetId, by)
                _activated.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Activation failed"
            }
        }
    }
}
