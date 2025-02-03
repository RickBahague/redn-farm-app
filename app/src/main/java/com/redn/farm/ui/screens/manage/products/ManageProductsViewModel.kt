package com.redn.farm.ui.screens.manage.products

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.redn.farm.data.local.DatabaseInitializer
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.model.Product
import com.redn.farm.data.model.ProductPrice
import com.redn.farm.data.repository.ProductRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ManageProductsViewModel(application: Application) : AndroidViewModel(application) {
    private val databaseInitializer = DatabaseInitializer(application)
    private val repository = ProductRepository(
        FarmDatabase.getDatabase(application).productDao(),
        FarmDatabase.getDatabase(application).productPriceDao()
    )

    private val _isReinitializing = MutableStateFlow(false)
    val isReinitializing = _isReinitializing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

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

    fun reinitializeDatabase() {
        viewModelScope.launch {
            try {
                _isReinitializing.value = true
                _error.value = null
                
                // Reinitialize database
                databaseInitializer.reinitializeDatabase()
                
                // Add a small delay to ensure database is ready
                delay(500)
                
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isReinitializing.value = false
            }
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product)
        }
    }

    fun updateProductPrice(productPrice: ProductPrice) {
        viewModelScope.launch {
            repository.updateProductPrice(productPrice)
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as Application)
                ManageProductsViewModel(application)
            }
        }
    }
} 