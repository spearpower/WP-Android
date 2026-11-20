package com.workspace.disciplehub.ui.me

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.workspace.disciplehub.BuildConfig
import com.workspace.disciplehub.data.Me

@Composable
fun MeScreen(me: Me, onLogout: () -> Unit, modifier: Modifier = Modifier) {
    var confirm by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as com.workspace.disciplehub.WpApp
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AsyncImage(
            model = me.photoUrl,
            contentDescription = "Profile photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(96.dp).clip(CircleShape),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(me.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(me.roleLabel.ifBlank { me.role }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 6.dp)) {
                InfoRow(Icons.Outlined.Person, "Username", me.username)
                HorizontalDivider()
                InfoRow(Icons.Outlined.Badge, "Role", me.roleLabel.ifBlank { me.role })
                me.center?.let {
                    HorizontalDivider()
                    InfoRow(Icons.Outlined.LocationOn, "Center", listOf(it.name, it.country).filter(String::isNotBlank).joinToString(", "))
                }
                HorizontalDivider()
                InfoRow(Icons.Outlined.Notifications, "Push notifications", com.workspace.disciplehub.push.Push.status)
                HorizontalDivider()
                InfoRow(Icons.Outlined.Info, "App version", BuildConfig.VERSION_NAME + if (BuildConfig.FLAVOR == "dev") " · " + BuildConfig.API_BASE else "")
            }
        }
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 6.dp)) {
                LinkRow(Icons.Outlined.Notifications, "Notification settings") {
                    val i = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    runCatching { context.startActivity(i) }
                }
                HorizontalDivider()
                LinkRow(Icons.Outlined.PrivacyTip, "Privacy policy") { com.workspace.disciplehub.ui.common.openUrl(context, app.api.siteBase + "privacy.php") }
                HorizontalDivider()
                LinkRow(Icons.Outlined.DeleteForever, "Delete my account") { deleting = true }
            }
        }
        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = { confirm = true }, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Sign out")
        }
    }
    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("Delete your account?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("We will ask the DH Workspace administrators to delete your account and personal data (learning and reading progress, profile). They confirm it is you first, within 30 days. You will be signed out now.")
                    androidx.compose.material3.OutlinedTextField(reason, { reason = it }, label = { Text("Reason (optional)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    deleting = false
                    app.scope.launch {
                        val ok = runCatching {
                            app.api.postRaw("account.php", kotlinx.serialization.json.buildJsonObject {
                                put("action", kotlinx.serialization.json.JsonPrimitive("delete_request")); put("reason", kotlinx.serialization.json.JsonPrimitive(reason))
                            })
                        }.isSuccess
                        android.widget.Toast.makeText(context, if (ok) "Your request was sent. You are signed out." else "The request could not be sent. Try again, or use the website page.", android.widget.Toast.LENGTH_LONG).show()
                        if (ok) onLogout()
                    }
                }) { Text("Ask to delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancel") } },
        )
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Sign out?") },
            text = { Text("You will need your username and password to sign in again on this phone.") },
            confirmButton = { TextButton(onClick = { confirm = false; onLogout() }) { Text("Sign out") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LinkRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
