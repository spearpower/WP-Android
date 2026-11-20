package com.workspace.disciplehub.ui.join

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workspace.disciplehub.WpApp
import com.workspace.disciplehub.data.ApiException
import com.workspace.disciplehub.data.Welcome
import com.workspace.disciplehub.ui.learn.TopBar
import com.workspace.disciplehub.ui.theme.DhLime
import com.workspace.disciplehub.ui.theme.DhOnLime
import kotlinx.coroutines.launch

class SetupViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as WpApp).join
    var data by mutableStateOf<Welcome?>(null)
        private set
    var step by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init { load() }

    fun load() {
        viewModelScope.launch {
            try {
                data = repo.welcome().also { step = it.step }
            } catch (e: ApiException) {
                error = e.message
            }
        }
    }

    fun send(action: String, vararg fields: Pair<String, String>, onDone: () -> Unit) {
        busy = true
        error = null
        viewModelScope.launch {
            try {
                step = repo.step(action, *fields)
                if (step.isEmpty()) onDone()
            } catch (e: ApiException) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }
}

/** First sign-in of a self sign-up: own password, privacy policy, then place and center (as welcome.php on the web). */
@Composable
fun SetupScreen(onDone: () -> Unit, onLogout: () -> Unit, vm: SetupViewModel = viewModel()) {
    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Welcome to DH Workspace", onLogout)
        val d = vm.data
        if (d == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (vm.error != null) Text(vm.error ?: "", modifier = Modifier.padding(24.dp)) else CircularProgressIndicator()
            }
            return@Column
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val steps = listOf("password", "privacy", "setup")
            Text("Step ${(steps.indexOf(vm.step) + 1).coerceAtLeast(1)} of 3", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            vm.error?.let { Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) { Text(it, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp)) } }
            when (vm.step) {
                "password" -> PasswordStep(vm, onDone)
                "privacy" -> PrivacyStep(vm, d.privacy?.text.orEmpty(), onDone)
                "setup" -> PlaceStep(vm, d, onDone)
                else -> androidx.compose.runtime.LaunchedEffect(Unit) { onDone() }
            }
        }
    }
}

@Composable
private fun NextButton(text: String, enabled: Boolean, busy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled && !busy,
        colors = ButtonDefaults.buttonColors(containerColor = DhLime, contentColor = DhOnLime),
        shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().height(52.dp),
    ) { if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = DhOnLime, strokeWidth = 2.5.dp) else Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
private fun PasswordStep(vm: SetupViewModel, onDone: () -> Unit) {
    var p1 by rememberSaveable { mutableStateOf("") }
    var p2 by rememberSaveable { mutableStateOf("") }
    Text("Choose your own password", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("At least 8 characters, with letters and numbers. It replaces the one we sent you.", style = MaterialTheme.typography.bodyMedium)
    OutlinedTextField(p1, { p1 = it }, label = { Text("New password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(p2, { p2 = it }, label = { Text("Type it again") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    NextButton("Save my password", p1.isNotEmpty() && p2.isNotEmpty(), vm.busy) { vm.send("password", "password" to p1, "confirm" to p2, onDone = onDone) }
}

@Composable
private fun PrivacyStep(vm: SetupViewModel, text: String, onDone: () -> Unit) {
    var ok by rememberSaveable { mutableStateOf(false) }
    Text("Our privacy policy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.verticalScroll(rememberScrollState()).padding(14.dp))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(ok, { ok = it })
        Text("I have read and accept the privacy policy.")
    }
    NextButton("Continue", ok, vm.busy) { vm.send("privacy", "accept" to "1", onDone = onDone) }
}

@Composable
private fun PlaceStep(vm: SetupViewModel, d: Welcome, onDone: () -> Unit) {
    var country by rememberSaveable { mutableStateOf("Uganda") }
    var other by rememberSaveable { mutableStateOf("") }
    var unitId by rememberSaveable { mutableStateOf(0) }
    var unitName by rememberSaveable { mutableStateOf("") }
    var region by rememberSaveable { mutableStateOf("") }
    var centre by rememberSaveable { mutableStateOf(-1) }
    val listed = d.places.containsKey(country)
    val realCountry = if (country == "Other") other.trim() else country
    val centres = d.centres.filter { country == "Other" || it.country.equals(country, true) }
    Text("Where do you live?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    PickerField("Country", country, (d.places.keys + "Other").map { Triple(it, it, "") }, { country = it; unitId = 0; unitName = ""; centre = -1 })
    if (country == "Other") OutlinedTextField(other, { other = it }, label = { Text("Your country") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    if (listed) {
        PickerField(if (country == "Kenya") "County" else "District", unitName, d.places[country].orEmpty().map { Triple(it.id, it.name, "") }, { id ->
            unitId = id; unitName = d.places[country].orEmpty().firstOrNull { it.id == id }?.name.orEmpty()
        })
    } else {
        OutlinedTextField(region, { region = it }, label = { Text("Region or city") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
    Text("Which center would you like to join?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    val centreLabel = when (centre) {
        -1 -> ""
        0 -> "My center is not listed"
        else -> d.centres.firstOrNull { it.id == centre }?.name.orEmpty()
    }
    PickerField("Center", centreLabel, listOf(Triple(0, "My center is not listed", "A leader will help you")) + centres.map { Triple(it.id, it.name, listOf(it.district, it.country).filter(String::isNotBlank).joinToString(", ")) }, { centre = it })
    Text("A leader of that center will welcome you. Until then you can already learn.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    NextButton("Finish", realCountry.isNotBlank() && centre >= 0 && (!listed || unitId > 0), vm.busy) {
        vm.send("setup", "country" to realCountry, "unit_id" to unitId.toString(), "region" to region, "group_id" to centre.toString(), onDone = onDone)
    }
}
