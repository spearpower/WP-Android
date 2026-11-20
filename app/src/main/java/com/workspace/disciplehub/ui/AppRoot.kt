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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.workspace.disciplehub.ui.join.SetupScreen
import com.workspace.disciplehub.ui.join.SignUpScreen
import com.workspace.disciplehub.ui.login.LoginScreen
import com.workspace.disciplehub.ui.main.MainScreen
import com.workspace.disciplehub.ui.theme.DhForest
import com.workspace.disciplehub.ui.theme.DhLime

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var joining by rememberSaveable { mutableStateOf(false) }
    var prefill by rememberSaveable { mutableStateOf("") }
    when (val s = state) {
        AuthState.Loading -> Box(Modifier.fillMaxSize().background(DhForest), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DhLime)
        }
        is AuthState.SignedOut -> if (joining) {
            SignUpScreen(onBack = { joining = false }, onSignIn = { prefill = it; joining = false })
        } else {
            LoginScreen(notice = s.notice, onSignedIn = vm::signedIn, onJoin = { joining = true }, prefill = prefill)
        }
        // New self sign-ups finish their setup first (own password, privacy policy, center).
        is AuthState.SignedIn -> if (s.me.setupStep.isNotEmpty() && !s.offline) {
            SetupScreen(onDone = vm::refresh, onLogout = vm::logout)
        } else {
            MainScreen(me = s.me, offline = s.offline, onRefresh = vm::refresh, onLogout = vm::logout)
        }
    }
}
