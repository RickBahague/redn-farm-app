package com.redn.farm.ui.screens.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.dao.UserDao
import com.redn.farm.data.local.security.PasswordManager
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.security.Rbac
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginState {
    object Initial : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userDao: UserDao,
    @ApplicationContext appContext: Context
) : ViewModel() {
    private val sessionManager = SessionManager(appContext)
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState: StateFlow<LoginState> = _loginState

    init {
        checkSession()
    }

    private fun checkSession() {
        if (!sessionManager.isLoggedIn()) {
            _loginState.value = LoginState.Initial
        } else {
            _loginState.value = LoginState.Success
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading
                Log.d("LoginViewModel", "Starting login process for username: $username")

                if (username.isBlank() || password.isBlank()) {
                    Log.d("LoginViewModel", "Username or password is blank")
                    _loginState.value = LoginState.Error("Username and password are required")
                    return@launch
                }

                val user = userDao.getUserByUsername(username)
                Log.d("LoginViewModel", "User found: ${user != null}")

                if (user == null) {
                    Log.d("LoginViewModel", "User not found in database")
                    _loginState.value = LoginState.Error("Invalid username or password")
                    return@launch
                }

                if (!user.is_active) {
                    Log.d("LoginViewModel", "User account is deactivated")
                    _loginState.value = LoginState.Error("Account is deactivated")
                    return@launch
                }

                Log.d("LoginViewModel", "Verifying password...")
                val isPasswordValid = PasswordManager.verifyPassword(password, user.password_hash)
                Log.d("LoginViewModel", "Password verification result: $isPasswordValid")

                if (isPasswordValid) {
                    Log.d("LoginViewModel", "Login successful")
                    sessionManager.createSession(username, Rbac.normalizeRoleForStorage(user.role))
                    _loginState.value = LoginState.Success
                } else {
                    Log.d("LoginViewModel", "Password verification failed")
                    _loginState.value = LoginState.Error("Invalid username or password")
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Login failed with exception", e)
                _loginState.value = LoginState.Error("Login failed: ${e.message}")
            }
        }
    }

    fun logout() {
        sessionManager.endSession()
        _loginState.value = LoginState.Initial
        Log.d("LoginViewModel", "User logged out")
    }
}
