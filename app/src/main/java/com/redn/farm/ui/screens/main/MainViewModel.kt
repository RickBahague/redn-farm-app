package com.redn.farm.ui.screens.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.dao.UserDao
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.security.Rbac
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

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    init {
        viewModelScope.launch {
            resolveRole()
            _isAdmin.value = _userRole.value?.equals("ADMIN", ignoreCase = true) == true
        }
    }

    private suspend fun resolveRole() {
        val username = sessionManager.getUsername() ?: return
        val user = userDao.getUserByUsername(username) ?: return
        _userRole.value = Rbac.normalizeRole(user.role)
    }
}
