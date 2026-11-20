package com.workspace.disciplehub.ui.join

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workspace.disciplehub.WpApp
import com.workspace.disciplehub.data.ApiException
import com.workspace.disciplehub.data.JoinResult
import com.workspace.disciplehub.ui.learn.TopBar
import com.workspace.disciplehub.ui.theme.DhLime
import com.workspace.disciplehub.ui.theme.DhOnLime
import kotlinx.coroutines.launch

class SignUpViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as WpApp).join
    val opened = System.currentTimeMillis()
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var done by mutableStateOf<JoinResult?>(null)
        private set

    fun submit(first: String, last: String, email: String, phone: String, cc: String, channel: String, kind: String, own: String) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try {
                done = repo.submit(first.trim(), last.trim(), email.trim(), phone.trim(), cc, channel, kind, own.trim(), opened)
            } catch (e: ApiException) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }
}

/** Join Disciple Hub: the same sign-up as join.php on the website; the password comes by email or SMS. */
@Composable
fun SignUpScreen(onBack: () -> Unit, onSignIn: (String) -> Unit, vm: SignUpViewModel = viewModel()) {
    BackHandler(onBack = onBack)
    var first by rememberSaveable { mutableStateOf("") }
    var last by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var cc by rememberSaveable { mutableStateOf("256") }
    var channel by rememberSaveable { mutableStateOf("EMAIL") }
    var kind by rememberSaveable { mutableStateOf("email") }
    var own by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Join Disciple Hub", onBack)
        val done = vm.done
        if (done != null) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
                Icon(Icons.Outlined.MarkEmailRead, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(64.dp))
                Text("Your account is ready!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    if (done.sent) "We sent your password by ${if (done.channel == "PHONE") "text message" else "email"} to ${done.sentTo}. Sign in with it below."
                    else "The password could not be sent. Use \"Forgot your password?\" on the sign-in screen, or ask a center leader for help.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text("Your username: ${done.username}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
                }
                Button(onClick = { onSignIn(done.username) }, colors = ButtonDefaults.buttonColors(containerColor = DhLime, contentColor = DhOnLime)) { Text("Go to sign in", fontWeight = FontWeight.Bold) }
            }
            return@Column
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Create your DH Workspace account to learn, join live sessions and read the Library.", style = MaterialTheme.typography.bodyMedium)
            vm.error?.let { Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) { Text(it, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp)) } }
            OutlinedTextField(first, { first = it }, label = { Text("First name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(last, { last = it }, label = { Text("Last name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(email, { email = it }, label = { Text("Email (optional if you use your phone)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PickerField("Code", "+$cc", listOf(Triple("256", "+256 Uganda", ""), Triple("254", "+254 Kenya", "")), { cc = it }, Modifier.width(120.dp))
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone (optional)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.weight(1f))
            }
            Text("Send my password by", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(channel == "EMAIL", { channel = "EMAIL" }, label = { Text("Email") })
                FilterChip(channel == "PHONE", { channel = "PHONE" }, label = { Text("Text message") })
            }
            Text("My username will be", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(kind == "email", { kind = "email" }, label = { Text("My email") })
                FilterChip(kind == "phone", { kind = "phone" }, label = { Text("My phone") })
                FilterChip(kind == "own", { kind = "own" }, label = { Text("My own") })
            }
            if (kind == "own") OutlinedTextField(own, { own = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { vm.submit(first, last, email, phone, cc, channel, kind, own) },
                enabled = !vm.busy,
                colors = ButtonDefaults.buttonColors(containerColor = DhLime, contentColor = DhOnLime),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (vm.busy) CircularProgressIndicator(Modifier.size(22.dp), color = DhOnLime, strokeWidth = 2.5.dp) else Text("Create my account", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("I already have an account") }
        }
    }
}
