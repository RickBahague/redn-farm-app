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
import kotlinx.coroutines.flow.stateIn

data class ProductActiveSrpRow(
    val product: Product,
    val acquisition: Acquisition?,
    val summaryFromPerKg: Double?
)

class ActiveSrpsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = FarmDatabase.getDatabase(application)
    private val productRepository = ProductRepository(database.productDao(), database.productPriceDao())
    private val acquisitionRepository = AcquisitionRepository(
        database.acquisitionDao(),
        PricingPresetRepository(database.pricingPresetDao(), database.presetActivationLogDao()),
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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
                ActiveSrpsViewModel(app)
            }
        }
    }
}
