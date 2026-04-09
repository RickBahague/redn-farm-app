package com.redn.farm.ui.screens.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.model.Product
import com.redn.farm.data.repository.AcquisitionRepository
import com.redn.farm.data.repository.PricingPresetRepository
import com.redn.farm.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProductActiveSrpRow(
    val product: Product,
    val acquisition: Acquisition?,
)

@HiltViewModel
class ActiveSrpsViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val pricingPresetRepository: PricingPresetRepository,
    private val acquisitionRepository: AcquisitionRepository
) : ViewModel() {

    val rows = combine(
        productRepository.getAllProducts(),
        acquisitionRepository.observeAllActiveSrps()
    ) { products, activeList ->
        val byId = activeList.associateBy { it.product_id }
        products
            .filter { it.is_active }
            .sortedBy { it.product_name.lowercase() }
            .map { p ->
                val acq = byId[p.product_id]
                ProductActiveSrpRow(product = p, acquisition = acq)
            }
            .filter { it.acquisition != null }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activePresetName = pricingPresetRepository
        .getActivePreset()
        .map { it?.preset_name }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val activePresetActivatedAt = combine(
        pricingPresetRepository.getActivePreset(),
        pricingPresetRepository.getActivationLog()
    ) { preset, logs ->
        preset?.let { p ->
            logs.firstOrNull { it.preset_id_activated == p.preset_id }?.activated_at
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
}
