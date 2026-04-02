package com.redn.farm.ui.screens.manage.employees.payment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.data.model.EmployeePayment
import com.redn.farm.data.repository.EmployeePaymentRepository
import com.redn.farm.security.Rbac
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EmployeePaymentViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application)
    private val repository = EmployeePaymentRepository(
        FarmDatabase.getDatabase(application).employeePaymentDao()
    )

    val payments = repository.getAllPayments()

    fun addPayment(payment: EmployeePayment) {
        viewModelScope.launch {
            if (!Rbac.canWriteEmployees(sessionManager.getRole())) return@launch
            repository.addPayment(payment)
        }
    }

    fun updatePayment(payment: EmployeePayment) {
        viewModelScope.launch {
            if (!Rbac.canWriteEmployees(sessionManager.getRole())) return@launch
            repository.updatePayment(payment)
        }
    }

    fun deletePayment(payment: EmployeePayment) {
        viewModelScope.launch {
            if (!Rbac.canWriteEmployees(sessionManager.getRole())) return@launch
            repository.deletePayment(payment)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as Application)
                EmployeePaymentViewModel(application)
            }
        }
    }
} 