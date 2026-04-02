package com.redn.farm.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.dao.UserDao
import com.redn.farm.data.local.entity.UserEntity
import com.redn.farm.data.local.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    application: Application,
    private val userDao: UserDao
) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    private val _profileUser = MutableStateFlow<UserEntity?>(null)
    val profileUser: StateFlow<UserEntity?> = _profileUser.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val username = sessionManager.getUsername() ?: run {
                _profileUser.value = null
                _isAdmin.value = false
                return@launch
            }
            val user = userDao.getUserByUsername(username)
            _profileUser.value = user
            _isAdmin.value =
                sessionManager.isAdmin() || user?.role.equals("ADMIN", ignoreCase = true)
        }
    }
}
