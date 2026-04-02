package com.redn.farm.ui.screens.remittance

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.data.model.Remittance
import com.redn.farm.data.repository.RemittanceRepository
import com.redn.farm.security.Rbac
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemittanceViewModel(
    private val repository: RemittanceRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    val remittances = repository.getAllRemittances()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addRemittance(amount: Double, remarks: String, date: Long) {
        viewModelScope.launch {
            if (!Rbac.canWriteRemittances(sessionManager.getRole())) {
                _userMessage.emit("You don't have permission to add remittances.")
                return@launch
            }
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
            if (!Rbac.canWriteRemittances(sessionManager.getRole())) {
                _userMessage.emit("You don't have permission to delete remittances.")
                return@launch
            }
            repository.deleteRemittance(remittance)
        }
    }

    fun updateRemittance(remittance: Remittance) {
        viewModelScope.launch {
            if (!Rbac.canWriteRemittances(sessionManager.getRole())) {
                _userMessage.emit("You don't have permission to update remittances.")
                return@launch
            }
            repository.updateRemittance(remittance)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RemittanceViewModel::class.java)) {
                val database = FarmDatabase.getDatabase(application)
                return RemittanceViewModel(
                    RemittanceRepository(database.remittanceDao()),
                    SessionManager(application)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 