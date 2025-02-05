package com.redn.farm.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.redn.farm.navigation.Screen
import com.redn.farm.ui.screens.login.LoginState
import com.redn.farm.ui.screens.login.LoginViewModel

@Composable
fun SessionChecker(
    navController: NavHostController,
    loginViewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
    content: @Composable () -> Unit
) {
    val loginState by loginViewModel.loginState.collectAsState()

    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginState.Initial -> {
                // If not logged in and not on login screen, navigate to login
                if (navController.currentDestination?.route != Screen.Login.route) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {} // Do nothing for other states
        }
    }

    content()
} 