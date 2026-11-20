package com.workspace.disciplehub.ui.live

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workspace.disciplehub.WpApp
import com.workspace.disciplehub.data.ApiException
import com.workspace.disciplehub.data.LiveSession
import com.workspace.disciplehub.ui.common.EmptyState
import com.workspace.disciplehub.ui.common.minutesUntil
import com.workspace.disciplehub.ui.common.openUrl
import com.workspace.disciplehub.ui.common.sessionWhen
import com.workspace.disciplehub.ui.theme.DhForest
import com.workspace.disciplehub.ui.theme.DhGreen
import com.workspace.disciplehub.ui.theme.DhLime
import com.workspace.disciplehub.ui.theme.DhOnLime
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class LiveViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as WpApp).live
    var sessions by androidx.compose.runtime.mutableStateOf<List<LiveSession>?>(null)
        private set
    var loading by androidx.compose.runtime.mutableStateOf(false)
        private set
    var error by androidx.compose.runtime.mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            sessions = repo.cached()?.sessions
            refresh()
        }
    }

    fun refresh() {
        if (loading) return
        loading = true
        viewModelScope.launch {
            try {
                sessions = repo.load().sessions
                error = null
            } catch (e: ApiException) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }
}

/** On phones Gerald wants only Today, Tomorrow and This week (2026-10-xx rule for the mobile web, kept in the app). */
private enum class LiveFilter(val label: String) { Today("Today"), Tomorrow("Tomorrow"), Week("This week") }

private fun localDate(iso: String): LocalDate? = runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate() }.getOrNull()

private fun matches(s: LiveSession, f: LiveFilter): Boolean {
    val today = LocalDate.now(ZoneId.systemDefault())
    val day = localDate(s.start) ?: return false
    return when (f) {
        LiveFilter.Today -> day == today || s.live
        LiveFilter.Tomorrow -> day == today.plusDays(1)
        LiveFilter.Week -> !day.isBefore(today) && !day.isAfter(today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(modifier: Modifier = Modifier, vm: LiveViewModel = viewModel()) {
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf(LiveFilter.Week) }
    val all = vm.sessions.orEmpty().filter { !it.past }
    val shown = all.filter { matches(it, filter) }

    PullToRefreshBox(isRefreshing = vm.loading && vm.sessions != null, onRefresh = vm::refresh, modifier = modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(
                    Modifier.fillMaxWidth().background(DhForest, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                        .statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Live Sessions", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiveFilter.entries.forEach { f ->
                            val count = all.count { matches(it, f) }
                            FilterChip(
                                selected = filter == f,
                                onClick = { filter = f },
                                label = { Text(if (count > 0) "${f.label} ($count)" else f.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White.copy(alpha = 0.1f), labelColor = Color.White,
                                    selectedContainerColor = DhLime, selectedLabelColor = DhOnLime,
                                ),
                                border = null,
                            )
                        }
                    }
                }
            }
            vm.error?.takeIf { vm.sessions == null }?.let { item { EmptyState(Icons.Outlined.EventBusy, "Sessions could not load", it, Modifier.height(320.dp)) } }
            if (vm.sessions != null && shown.isEmpty()) {
                item {
                    EmptyState(Icons.Outlined.EventBusy, "No sessions ${filter.label.lowercase()}",
                        if (all.isNotEmpty()) "Your next one: ${all.first().title}, ${sessionWhen(all.first().start)}." else "You have no live sessions coming up.",
                        Modifier.height(320.dp))
                }
            }
            items(shown, key = { "${it.id}_${it.start}" }) { s -> SessionCard(s, onJoin = { openUrl(context, s.joinUrl) }, onCopy = { copy(context, s.passcode) }) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun copy(context: Context, text: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Passcode", text))
    Toast.makeText(context, "Passcode copied", Toast.LENGTH_SHORT).show()
}

@Composable
private fun SessionCard(s: LiveSession, onJoin: () -> Unit, onCopy: () -> Unit) {
    val soon = (minutesUntil(s.start) ?: Long.MAX_VALUE) <= 15
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(42.dp).background(Color(0xFF1E88E5).copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Videocam, contentDescription = null, tint = Color(0xFF1E88E5))
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (s.live) Badge("LIVE", Color(0xFFC2412D), Color.White)
                        if (s.myRole == "FACILITATOR") Badge("Facilitator", DhGreen, Color.White)
                    }
                    Text(s.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(sessionWhen(s.start), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (s.recurrence.isNotBlank()) Text(s.recurrence, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (s.facilitators.isNotEmpty()) Text("With " + s.facilitators.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (s.agenda.isNotBlank()) Text(s.agenda, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (s.joinUrl.isNotBlank()) {
                    if (s.live || soon) {
                        Button(onClick = onJoin, colors = ButtonDefaults.buttonColors(containerColor = DhGreen)) { Text("Join now") }
                    } else {
                        OutlinedButton(onClick = onJoin) { Text("Join") }
                    }
                }
                Spacer(Modifier.weight(1f))
                if (s.passcode.isNotBlank()) {
                    Text("Passcode ${s.passcode}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = onCopy) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy passcode", modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, bg: Color, fg: Color) {
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(text, color = fg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}
