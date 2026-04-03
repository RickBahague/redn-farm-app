package com.redn.farm.ui.screens.acquire

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.model.Product
import com.redn.farm.data.model.AcquisitionLocation
import com.redn.farm.data.repository.AcquisitionDraftPricingPreview
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.data.repository.AcquisitionRepository
import com.redn.farm.data.repository.AcquisitionSaveOutcome
import com.redn.farm.data.repository.ProductRepository
import com.redn.farm.security.Rbac
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AcquireProduceViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    private val acquisitionRepository: AcquisitionRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val sessionManager = SessionManager(appContext)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedLocation = MutableStateFlow<AcquisitionLocation?>(null)
    val selectedLocation = _selectedLocation.asStateFlow()

    private val _selectedDateRange = MutableStateFlow<Pair<LocalDateTime?, LocalDateTime?>>(null to null)
    val selectedDateRange = _selectedDateRange.asStateFlow()
    
    private val _acquisitions = MutableStateFlow<List<Acquisition>>(emptyList())
    val acquisitions = combine(
        acquisitionRepository.getAllAcquisitions(),
        _searchQuery,
        _selectedLocation,
        _selectedDateRange
    ) { acquisitions, query, location, dateRange ->
        acquisitions.filter { acquisition ->
            val matchesSearch = acquisition.product_name.contains(query, ignoreCase = true) ||
                acquisition.acquisition_id.toString().contains(query)

            val matchesLocation = location == null || acquisition.location == location

            val acquisitionDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(acquisition.date_acquired),
                ZoneId.systemDefault()
            )
            val matchesDateRange = isWithinDateRange(acquisitionDateTime, dateRange)

            matchesSearch && matchesLocation && matchesDateRange
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            acquisitionRepository.getAllAcquisitions().collect {
                _acquisitions.value = it
            }
        }
        viewModelScope.launch {
            productRepository.getAllProducts().collect {
                _products.value = it
            }
        }
    }

    fun addAcquisition(acquisition: Acquisition) {
        viewModelScope.launch {
            if (!Rbac.canWriteAcquisitions(sessionManager.getRole())) {
                _userMessage.emit("You don't have permission to add acquisitions.")
                return@launch
            }
            when (val o = acquisitionRepository.insertWithPricing(acquisition)) {
                AcquisitionSaveOutcome.Success -> {}
                is AcquisitionSaveOutcome.ValidationError -> _userMessage.emit(o.message)
                is AcquisitionSaveOutcome.SavedWithoutActivePreset -> _userMessage.emit(o.message)
            }
        }
    }

    fun updateAcquisition(acquisition: Acquisition) {
        viewModelScope.launch {
            if (!Rbac.canWriteAcquisitions(sessionManager.getRole())) {
                _userMessage.emit("You don't have permission to update acquisitions.")
                return@launch
            }
            when (val o = acquisitionRepository.updateWithPricing(acquisition)) {
                AcquisitionSaveOutcome.Success -> {}
                is AcquisitionSaveOutcome.ValidationError -> _userMessage.emit(o.message)
                is AcquisitionSaveOutcome.SavedWithoutActivePreset -> _userMessage.emit(o.message)
            }
        }
    }

    suspend fun previewDraftPricing(acquisition: Acquisition): AcquisitionDraftPricingPreview =
        acquisitionRepository.previewDraftPricing(acquisition)

    fun deleteAcquisition(acquisition: Acquisition) {
        viewModelScope.launch {
            if (!Rbac.canWriteAcquisitions(sessionManager.getRole())) {
                _userMessage.emit("You don't have permission to delete acquisitions.")
                return@launch
            }
            acquisitionRepository.deleteAcquisition(acquisition)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateLocationFilter(location: AcquisitionLocation?) {
        _selectedLocation.value = location
    }

    fun updateDateRange(dateRange: Pair<LocalDateTime?, LocalDateTime?>) {
        _selectedDateRange.value = dateRange
    }

    private fun isWithinDateRange(
        date: LocalDateTime,
        range: Pair<LocalDateTime?, LocalDateTime?>
    ): Boolean {
        val (start, end) = range
        return when {
            start == null && end == null -> true
            start == null -> date <= end!!
            end == null -> date >= start
            else -> date in start..end
        }
    }
} 