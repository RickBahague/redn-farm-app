package com.redn.farm.ui.screens.pricing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.entity.PricingPresetEntity
import com.redn.farm.data.repository.PricingPresetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PricingPresetsHomeViewModel @Inject constructor(
    repository: PricingPresetRepository
) : ViewModel() {

    val activePreset: StateFlow<PricingPresetEntity?> = repository.getActivePreset()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
