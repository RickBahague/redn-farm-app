package com.redn.farm.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.dao.UserDao
import com.redn.farm.data.local.security.PasswordManager
import com.redn.farm.data.local.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordFieldErrors(
    val current: String? = null,
    val newPassword: String? = null,
    val confirm: String? = null,
    val banner: String? = null
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    application: Application,
    private val userDao: UserDao
) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    private val _fieldErrors = MutableStateFlow(ChangePasswordFieldErrors())
    val fieldErrors: StateFlow<ChangePasswordFieldErrors> = _fieldErrors.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    fun clearTransient() {
        _fieldErrors.value = ChangePasswordFieldErrors()
        _done.value = false
    }

    fun submit(currentPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            _fieldErrors.value = ChangePasswordFieldErrors()
            _done.value = false

            if (newPassword != confirmPassword) {
                _fieldErrors.value = ChangePasswordFieldErrors(confirm = "Does not match new password")
                return@launch
            }
            if (newPassword.length < 4) {
                _fieldErrors.value = ChangePasswordFieldErrors(newPassword = "Must be at least 4 characters")
                return@launch
            }

            val username = sessionManager.getUsername() ?: run {
                _fieldErrors.value = ChangePasswordFieldErrors(banner = "Not signed in")
                return@launch
            }
            val user = userDao.getUserByUsername(username) ?: run {
                _fieldErrors.value = ChangePasswordFieldErrors(banner = "Account not found")
                return@launch
            }

            _isLoading.value = true
            if (!PasswordManager.verifyPassword(currentPassword, user.password_hash)) {
                _isLoading.value = false
                _fieldErrors.value = ChangePasswordFieldErrors(current = "Incorrect password")
                return@launch
            }

            val newHash = PasswordManager.hashPassword(newPassword)
            userDao.updateUser(
                user.copy(
                    password_hash = newHash,
                    date_updated = System.currentTimeMillis()
                )
            )
            _isLoading.value = false
            _done.value = true
        }
    }
}
