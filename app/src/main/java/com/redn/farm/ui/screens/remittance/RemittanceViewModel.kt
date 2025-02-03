package com.redn.farm.ui.screens.remittance

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.model.Remittance
import com.redn.farm.data.repository.RemittanceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RemittanceViewModel(
    private val repository: RemittanceRepository
) : ViewModel() {

    val remittances = repository.getAllRemittances()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addRemittance(amount: Double, remarks: String, date: Long) {
        viewModelScope.launch {
            repository.addRemittance(
                Remittance(
                    amount = amount,
                    remarks = remarks,
                    date = date
                )
            )
        }
    }

    fun deleteRemittance(remittance: Remittance) {
        viewModelScope.launch {
            repository.deleteRemittance(remittance)
        }
    }

    fun updateRemittance(remittance: Remittance) {
        viewModelScope.launch {
            repository.updateRemittance(remittance)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RemittanceViewModel::class.java)) {
                val database = FarmDatabase.getDatabase(application)
                return RemittanceViewModel(
                    RemittanceRepository(database.remittanceDao())
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 