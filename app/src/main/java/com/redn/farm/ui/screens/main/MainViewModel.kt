package com.redn.farm.ui.screens.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.dao.UserDao
import com.redn.farm.data.local.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val userDao: UserDao
) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    init {
        viewModelScope.launch {
            _isAdmin.value = resolveAdmin()
        }
    }

    private suspend fun resolveAdmin(): Boolean {
        if (sessionManager.isAdmin()) return true
        val username = sessionManager.getUsername() ?: return false
        val user = userDao.getUserByUsername(username) ?: return false
        return user.role.equals("ADMIN", ignoreCase = true)
    }

    fun logout() {
        sessionManager.endSession()
    }
}
