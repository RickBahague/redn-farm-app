package com.redn.farm.ui.screens.manage.products

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.data.model.Product
import com.redn.farm.data.model.ProductPrice
import com.redn.farm.data.model.ProductFilters
import com.redn.farm.data.repository.ProductRepository
import com.redn.farm.security.Rbac
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ManageProductsViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    private val repository: ProductRepository
) : ViewModel() {

    private val sessionManager = SessionManager(appContext)
    private val _canMutateProducts = MutableStateFlow(
        Rbac.canMutateProducts(sessionManager.getRole())
    )
    val canMutateProducts: StateFlow<Boolean> = _canMutateProducts.asStateFlow()

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
