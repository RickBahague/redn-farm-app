package com.redn.farm.ui.screens.order

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.model.Product
import com.redn.farm.data.pricing.OrderPricingResolver
import com.redn.farm.data.repository.AcquisitionRepository
import com.redn.farm.data.repository.PricingPresetRepository
import com.redn.farm.data.repository.ProductRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ProductActiveSrpRow(
    val product: Product,
    val acquisition: Acquisition?,
    val summaryFromPerKg: Double?
)

class ActiveSrpsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = FarmDatabase.getDatabase(application)
    private val productRepository = ProductRepository(database.productDao(), database.productPriceDao())
    private val pricingPresetRepository = PricingPresetRepository(
        database.pricingPresetDao(),
        database.presetActivationLogDao()
    )
    private val acquisitionRepository = AcquisitionRepository(
        database.acquisitionDao(),
        pricingPresetRepository,
        database.productDao()
    )

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
                ProductActiveSrpRow(
                    product = p,
                    acquisition = acq,
                    summaryFromPerKg = OrderPricingResolver.minPerKgSrpAcrossChannels(acq)
                )
            }
            .filter { it.acquisition != null && it.summaryFromPerKg != null }
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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
                ActiveSrpsViewModel(app)
            }
        }
    }
}
