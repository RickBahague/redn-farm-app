package com.redn.farm.ui.screens.pricing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.entity.PricingPresetEntity
import com.redn.farm.data.repository.PricingPresetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PresetHistoryViewModel @Inject constructor(
    private val repository: PricingPresetRepository
) : ViewModel() {

    val presets: StateFlow<List<PricingPresetEntity>> = repository.getAllPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteInactivePreset(
        presetId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching { repository.deleteInactivePreset(presetId) }
                .onSuccess { onSuccess() }
                .onFailure { e -> onError(e.message ?: "Could not delete preset.") }
        }
    }
}
