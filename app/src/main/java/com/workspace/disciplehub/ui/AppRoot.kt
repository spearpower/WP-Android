package com.workspace.disciplehub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workspace.disciplehub.ui.login.LoginScreen
import com.workspace.disciplehub.ui.main.MainScreen
import com.workspace.disciplehub.ui.theme.DhForest
import com.workspace.disciplehub.ui.theme.DhLime

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    when (val s = state) {
        AuthState.Loading -> Box(Modifier.fillMaxSize().background(DhForest), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DhLime)
        }
        is AuthState.SignedOut -> LoginScreen(notice = s.notice, onSignedIn = vm::signedIn)
        is AuthState.SignedIn -> MainScreen(me = s.me, offline = s.offline, onRefresh = vm::refresh, onLogout = vm::logout)
    }
}
