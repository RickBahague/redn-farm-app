package com.redn.farm.ui.screens.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.local.security.PasswordManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Initial : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val database = FarmDatabase.getDatabase(application)
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState: StateFlow<LoginState> = _loginState

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

                val user = database.userDao().getUserByUsername(username)
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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
                LoginViewModel(application)
            }
        }
    }
} 