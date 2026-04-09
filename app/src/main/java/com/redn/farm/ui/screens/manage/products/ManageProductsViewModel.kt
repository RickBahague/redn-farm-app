package com.redn.farm.ui.screens.manage.products

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.data.model.Product
import com.redn.farm.data.model.ProductPrice
import com.redn.farm.data.model.ProductFilters
import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.repository.AcquisitionRepository
import com.redn.farm.data.repository.ProductRepository
import com.redn.farm.security.Rbac
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ManageProductsViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    private val repository: ProductRepository,
    private val acquisitionRepository: AcquisitionRepository,
) : ViewModel() {

    private val sessionManager = SessionManager(appContext)
    private val _canMutateProducts = MutableStateFlow(
        Rbac.canMutateProducts(sessionManager.getRole())
    )
    val canMutateProducts: StateFlow<Boolean> = _canMutateProducts.asStateFlow()

    val canViewPresetDetail: StateFlow<Boolean> = MutableStateFlow(
        Rbac.canManageSettingsAndPricing(sessionManager.getRole())
    ).asStateFlow()

    val products = repository.getAllProducts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val productPrices = repository.getAllProductPrices()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Latest acquisition per product that supplies the active SRP (INV-US-06) — **PRD-US-01** catalog line. */
    val activeAcquisitionByProductId: StateFlow<Map<String, Acquisition>> =
        acquisitionRepository.observeAllActiveSrps()
            .map { list -> list.associateBy { it.product_id } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyMap()
            )

    fun observePriceHistory(productId: String): Flow<List<ProductPrice>> =
        if (productId == "new") flowOf(emptyList()) else repository.getPriceHistory(productId)

    fun observeAcquisitionHistory(productId: String): Flow<List<Acquisition>> =
        if (productId == "new") flowOf(emptyList()) else acquisitionRepository.getAcquisitionsForProduct(productId)

    suspend fun updateProduct(product: Product) {
        if (!Rbac.canMutateProducts(sessionManager.getRole())) return
        repository.updateProduct(product)
    }

    suspend fun updateProductPrice(productPrice: ProductPrice) {
        if (!Rbac.canMutateProducts(sessionManager.getRole())) return
        repository.updateProductPrice(productPrice)
    }

    suspend fun deleteProduct(productId: String) {
        if (!Rbac.canMutateProducts(sessionManager.getRole())) return
        repository.deleteProduct(productId)
    }

    suspend fun insertProduct(product: Product) {
        if (!Rbac.canMutateProducts(sessionManager.getRole())) return
        repository.insertProduct(product)
    }

    suspend fun insertProductPrice(productPrice: ProductPrice) {
        if (!Rbac.canMutateProducts(sessionManager.getRole())) return
        repository.insertProductPrice(productPrice)
    }

    fun getFilteredProductsFlow(filters: ProductFilters): Flow<List<Product>> =
        repository.getFilteredProducts(filters)
}
