package com.redn.farm.ui.screens.manage.employees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.data.model.Employee
import com.redn.farm.data.repository.EmployeePaymentRepository
import com.redn.farm.data.repository.EmployeeRepository
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.Context
import javax.inject.Inject

@HiltViewModel
class ManageEmployeesViewModel @Inject constructor(
    private val repository: EmployeeRepository,
    private val paymentRepository: EmployeePaymentRepository,
    @ApplicationContext appContext: Context
) : ViewModel() {

    private val sessionManager = SessionManager(appContext)

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _saveSucceeded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saveSucceeded: SharedFlow<Unit> = _saveSucceeded.asSharedFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val employees = searchQuery
        .combine(repository.getAllEmployees()) { query, employees ->
            if (query.isEmpty()) {
                employees
            } else {
                employees.filter {
                    it.fullName.contains(query, ignoreCase = true) ||
                        it.contact.contains(query, ignoreCase = true) ||
                        it.formattedId.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addEmployee(firstname: String, lastname: String, contact: String) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                if (!Rbac.canWriteEmployees(sessionManager.getRole())) {
                    _userMessage.emit("You don't have permission to add employees.")
                    return@launch
                }
                repository.addEmployee(
                    Employee(
                        firstname = firstname,
                        lastname = lastname,
                        contact = contact
                    )
                )
                _saveSucceeded.emit(Unit)
                delay(200)
                _userMessage.emit("Employee saved")
            } catch (_: Exception) {
                _userMessage.emit("Save failed — try again")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateEmployee(employee: Employee) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                if (!Rbac.canWriteEmployees(sessionManager.getRole())) {
                    _userMessage.emit("You don't have permission to update employees.")
                    return@launch
                }
                repository.updateEmployee(
                    employee.copy(
                        date_updated = System.currentTimeMillis()
                    )
                )
                _saveSucceeded.emit(Unit)
                delay(200)
                _userMessage.emit("Employee saved")
            } catch (_: Exception) {
                _userMessage.emit("Save failed — try again")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteEmployee(employee: Employee) {
        viewModelScope.launch {
            if (!Rbac.canWriteEmployees(sessionManager.getRole())) {
                _userMessage.emit("You don't have permission to delete employees.")
                return@launch
            }
            if (paymentRepository.countPaymentsForEmployee(employee.employee_id) > 0) {
                _userMessage.emit("Cannot delete — employee has payment history")
                return@launch
            }
            try {
                repository.deleteEmployee(employee)
            } catch (_: Exception) {
                _userMessage.emit("Save failed — try again")
            }
        }
    }
}
