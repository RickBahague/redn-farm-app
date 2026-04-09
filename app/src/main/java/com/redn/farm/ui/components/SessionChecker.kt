package com.redn.farm.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.redn.farm.navigation.Screen
import com.redn.farm.ui.screens.login.LoginState
import com.redn.farm.ui.screens.login.LoginViewModel

@Composable
fun SessionChecker(
    navController: NavHostController,
    loginViewModel: LoginViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
    content: @Composable () -> Unit
) {
    val loginState by loginViewModel.loginState.collectAsState()

    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginState.Initial -> {
                if (navController.currentDestination?.route != Screen.Login.route) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
            else -> {} // Do nothing for other states
        }
    }

    content()
} 