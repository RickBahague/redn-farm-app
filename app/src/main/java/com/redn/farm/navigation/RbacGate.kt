package com.redn.farm.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.security.Rbac

@Composable
fun RequireRole(
    navController: NavHostController,
    allowedNormalizedRoles: Set<String>,
    content: @Composable () -> Unit
) {
    val appContext = LocalContext.current.applicationContext
    val sessionManager = remember(appContext) { SessionManager(appContext) }
    val role = Rbac.normalizeRole(sessionManager.getRole())
    val allowed = role in allowedNormalizedRoles
    LaunchedEffect(role) {
        if (!allowed) {
            navController.popBackStack()
        }
    }
    if (allowed) {
        content()
    }
}
