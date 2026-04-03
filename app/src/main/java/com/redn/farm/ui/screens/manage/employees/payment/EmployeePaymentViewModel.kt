package com.redn.farm.ui.screens.manage.employees.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.data.model.EmployeePayment
import com.redn.farm.data.repository.EmployeePaymentRepository
import com.redn.farm.security.Rbac
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import android.content.Context
import javax.inject.Inject

@HiltViewModel
class EmployeePaymentViewModel @Inject constructor(
    private val repository: EmployeePaymentRepository,
    @ApplicationContext appContext: Context
) : ViewModel() {

    private val sessionManager = SessionManager(appContext)

    val payments: Flow<List<EmployeePayment>> = repository.getAllPayments()

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
}
