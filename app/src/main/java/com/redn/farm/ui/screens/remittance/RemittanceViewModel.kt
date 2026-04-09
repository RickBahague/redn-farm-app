package com.redn.farm.ui.screens.remittance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.data.model.Remittance
import com.redn.farm.data.model.RemittanceEntryType
import com.redn.farm.data.repository.RemittanceRepository
import com.redn.farm.security.Rbac
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemittanceViewModel @Inject constructor(
    private val repository: RemittanceRepository,
    @ApplicationContext appContext: Context
) : ViewModel() {

    private val sessionManager = SessionManager(appContext)

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _saveSucceeded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saveSucceeded: SharedFlow<Unit> = _saveSucceeded.asSharedFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val remittances = repository.getAllRemittances()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun canAddRemittance(): Boolean = Rbac.canWriteRemittances(sessionManager.getRole())

    fun canAddDisbursement(): Boolean = Rbac.canWriteDisbursements(sessionManager.getRole())

    fun canEdit(remittance: Remittance): Boolean {
        val role = sessionManager.getRole()
        return when (RemittanceEntryType.normalize(remittance.entry_type)) {
            RemittanceEntryType.DISBURSEMENT -> Rbac.canWriteDisbursements(role)
            else -> Rbac.canWriteRemittances(role)
        }
    }

    fun canDelete(remittance: Remittance): Boolean = canEdit(remittance)

    fun addRemittance(amount: Double, remarks: String, date: Long, entryType: String) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val normalized = RemittanceEntryType.normalize(entryType)
                when (normalized) {
                    RemittanceEntryType.DISBURSEMENT -> {
                        if (!Rbac.canWriteDisbursements(sessionManager.getRole())) {
                            _userMessage.emit("You don't have permission to add disbursements.")
                            return@launch
                        }
                    }
                    else -> {
                        if (!Rbac.canWriteRemittances(sessionManager.getRole())) {
                            _userMessage.emit("You don't have permission to add remittances.")
                            return@launch
                        }
                    }
                }
                val now = System.currentTimeMillis()
                repository.addRemittance(
                    Remittance(
                        amount = amount,
                        remarks = remarks,
                        date = date,
                        date_updated = now,
                        entry_type = normalized,
                    )
                )
                _saveSucceeded.emit(Unit)
                delay(200)
                _userMessage.emit(
                    if (normalized == RemittanceEntryType.DISBURSEMENT) "Disbursement saved"
                    else "Remittance saved"
                )
            } catch (_: Exception) {
                _userMessage.emit("Save failed — try again")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteRemittance(remittance: Remittance) {
        viewModelScope.launch {
            if (!canDelete(remittance)) {
                _userMessage.emit("You don't have permission to delete this entry.")
                return@launch
            }
            repository.deleteRemittance(remittance)
        }
    }

    fun updateRemittance(remittance: Remittance) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                if (!canEdit(remittance)) {
                    _userMessage.emit("You don't have permission to update this entry.")
                    return@launch
                }
                repository.updateRemittance(
                    remittance.copy(
                        entry_type = RemittanceEntryType.normalize(remittance.entry_type),
                        date_updated = System.currentTimeMillis(),
                    )
                )
                _saveSucceeded.emit(Unit)
                delay(200)
                _userMessage.emit(
                    if (RemittanceEntryType.isDisbursement(remittance.entry_type)) "Disbursement saved"
                    else "Remittance saved"
                )
            } catch (_: Exception) {
                _userMessage.emit("Save failed — try again")
            } finally {
                _isSaving.value = false
            }
        }
    }
}
