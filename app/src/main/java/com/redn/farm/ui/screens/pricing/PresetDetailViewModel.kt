package com.redn.farm.ui.screens.pricing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.entity.PricingPresetEntity
import com.redn.farm.data.repository.PricingPresetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PresetDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PricingPresetRepository
) : ViewModel() {

    private val presetId: String = checkNotNull(savedStateHandle["presetId"])

    private val _preset = MutableStateFlow<PricingPresetEntity?>(null)
    val preset: StateFlow<PricingPresetEntity?> = _preset.asStateFlow()

    init {
        viewModelScope.launch {
            _preset.value = repository.getPresetById(presetId)
        }
    }
}
