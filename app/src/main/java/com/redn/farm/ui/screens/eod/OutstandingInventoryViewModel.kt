package com.redn.farm.ui.screens.eod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.repository.DayCloseRepository
import com.redn.farm.data.util.DateWindowHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OutstandingInventoryUiState {
    object Loading : OutstandingInventoryUiState()
    data class Ready(val lines: List<DayCloseRepository.OutstandingProductLine>) : OutstandingInventoryUiState()
    data class Error(val message: String) : OutstandingInventoryUiState()
}

@HiltViewModel
class OutstandingInventoryViewModel @Inject constructor(
    private val repo: DayCloseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OutstandingInventoryUiState>(OutstandingInventoryUiState.Loading)
    val uiState: StateFlow<OutstandingInventoryUiState> = _uiState.asStateFlow()

    fun load(businessDateMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            _uiState.value = OutstandingInventoryUiState.Loading
            try {
                val lines = repo.buildOutstandingInventory(businessDateMillis)
                _uiState.value = OutstandingInventoryUiState.Ready(lines)
            } catch (e: Exception) {
                _uiState.value = OutstandingInventoryUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
