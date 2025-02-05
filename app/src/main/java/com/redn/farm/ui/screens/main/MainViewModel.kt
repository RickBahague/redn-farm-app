package com.redn.farm.ui.screens.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.redn.farm.data.local.session.SessionManager

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application)

    fun logout() {
        sessionManager.endSession()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
                MainViewModel(application)
            }
        }
    }
} 