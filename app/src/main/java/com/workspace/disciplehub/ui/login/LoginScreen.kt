package com.workspace.disciplehub.ui.login

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workspace.disciplehub.WpApp
import com.workspace.disciplehub.data.ApiException
import com.workspace.disciplehub.data.Me
import com.workspace.disciplehub.ui.common.openUrl
import com.workspace.disciplehub.ui.theme.DhForest
import com.workspace.disciplehub.ui.theme.DhLime
import com.workspace.disciplehub.ui.theme.DhOnLime
import kotlinx.coroutines.launch

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val wp = app as WpApp
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    val siteBase: String get() = wp.api.siteBase

    fun login(username: String, password: String, onDone: (Me) -> Unit) {
        if (busy) return
        if (username.isBlank() || password.isEmpty()) {
            error = "Enter your username and password."
            return
        }
        busy = true
        error = null
        viewModelScope.launch {
            try {
                onDone(wp.auth.login(username, password))
            } catch (e: ApiException) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }
}

@Composable
fun LoginScreen(notice: String?, onSignedIn: (Me) -> Unit, vm: LoginViewModel = viewModel()) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val submit = {
        focus.clearFocus()
        vm.login(username, password, onSignedIn)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(DhForest)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Brand
            Surface(shape = RoundedCornerShape(18.dp), color = DhLime, modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("DH", color = DhOnLime, fontWeight = FontWeight.Black, fontSize = 30.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("DH Workspace", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Learn, grow and serve together",
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Sign in", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    (vm.error ?: notice)?.let {
                        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth().padding(12.dp).semantics { contentDescription = "Error: $it" },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username, email or phone") },
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                        singleLine = true,
                        enabled = !vm.busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password",
                                )
                            }
                        },
                        singleLine = true,
                        enabled = !vm.busy,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { submit() },
                        enabled = !vm.busy,
                        colors = ButtonDefaults.buttonColors(containerColor = DhLime, contentColor = DhOnLime),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        if (vm.busy) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = DhOnLime, strokeWidth = 2.5.dp)
                        } else {
                            Text("Sign in", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    TextButton(onClick = { openUrl(context, vm.siteBase + "reset.php") }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Forgot your password?")
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("New to Disciple Hub?", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f))
                TextButton(onClick = { openUrl(context, vm.siteBase + "join.php") }) {
                    Text("Join", color = DhLime, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
