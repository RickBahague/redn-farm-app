package com.redn.farm.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.dao.UserDao
import com.redn.farm.data.local.entity.UserEntity
import com.redn.farm.data.local.security.PasswordManager
import com.redn.farm.data.local.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    application: Application,
    private val userDao: UserDao
) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    val users = userDao.getAllUsers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    init {
        viewModelScope.launch {
            _isAuthorized.value = resolveAdmin()
        }
    }

    private suspend fun resolveAdmin(): Boolean {
        if (sessionManager.isAdmin()) return true
        val username = sessionManager.getUsername() ?: return false
        val user = userDao.getUserByUsername(username) ?: return false
        return user.role.equals("ADMIN", ignoreCase = true)
    }

    fun consumeMessage() {
        _message.value = null
    }

    private suspend fun sessionUsername(): String? = sessionManager.getUsername()

    fun createUser(username: String, fullName: String, role: String, password: String) {
        viewModelScope.launch {
            val u = username.trim()
            if (u.isEmpty()) {
                _message.value = "Username is required."
                return@launch
            }
            if (password.length < 4) {
                _message.value = "Password must be at least 4 characters."
                return@launch
            }
            if (userDao.findByUsername(u) != null) {
                _message.value = "Username already taken."
                return@launch
            }
            val normalizedRole = when (role.uppercase()) {
                "ADMIN" -> "ADMIN"
                else -> "USER"
            }
            val hash = PasswordManager.hashPassword(password)
            val now = System.currentTimeMillis()
            userDao.insertUser(
                UserEntity(
                    username = u,
                    password_hash = hash,
                    full_name = fullName.trim().ifEmpty { u },
                    role = normalizedRole,
                    is_active = true,
                    date_created = now,
                    date_updated = now
                )
            )
            _message.value = "User created."
        }
    }

    fun setUserActive(user: UserEntity, active: Boolean) {
        viewModelScope.launch {
            if (!active) {
                val self = sessionUsername()
                if (self != null && user.username.equals(self, ignoreCase = true)) {
                    _message.value = "You cannot deactivate your own account."
                    return@launch
                }
            }
            userDao.updateUser(
                user.copy(
                    is_active = active,
                    date_updated = System.currentTimeMillis()
                )
            )
            _message.value = if (active) "User activated." else "User deactivated."
        }
    }

    fun resetPassword(userId: Int, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            if (newPassword != confirmPassword) {
                _message.value = "Passwords do not match."
                return@launch
            }
            if (newPassword.length < 4) {
                _message.value = "Password must be at least 4 characters."
                return@launch
            }
            val user = userDao.getUserById(userId) ?: return@launch
            val hash = PasswordManager.hashPassword(newPassword)
            userDao.updateUser(
                user.copy(
                    password_hash = hash,
                    date_updated = System.currentTimeMillis()
                )
            )
            _message.value = "Password reset for ${user.username}."
        }
    }
}
