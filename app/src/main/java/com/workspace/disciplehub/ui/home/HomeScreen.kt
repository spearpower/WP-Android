package com.workspace.disciplehub.ui.home

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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.workspace.disciplehub.data.Me
import com.workspace.disciplehub.ui.theme.DhForest
import com.workspace.disciplehub.ui.theme.DhGreen
import com.workspace.disciplehub.ui.theme.DhLime
import kotlinx.coroutines.delay
import java.util.Calendar

/** A dashboard card: which feature it opens, its colour. Phase 1 fills them with live numbers. */
private data class HomeCard(val feature: String, val title: String, val line: String, val icon: ImageVector, val tint: Color)

private val LEARNING = listOf(
    HomeCard("self_study", "Continue learning", "Pick up your course where you left it", Icons.Outlined.School, Color(0xFF22745D)),
    HomeCard("live_sessions", "Live sessions", "Join today's sessions", Icons.Outlined.LiveTv, Color(0xFF1E88E5)),
    HomeCard("library", "Library", "Books to read and download", Icons.Outlined.LocalLibrary, Color(0xFF6B46C1)),
    HomeCard("fun_quiz", "Fun Quiz", "Test what you learned", Icons.Outlined.Quiz, Color(0xFFD69E2E)),
    HomeCard("bible", "My Bible", "Read and look up verses", Icons.AutoMirrored.Outlined.MenuBook, Color(0xFF8D6E63)),
    HomeCard("reading_plans", "Reading plans", "Keep your daily streak", Icons.Outlined.TaskAlt, Color(0xFF00897B)),
    HomeCard("rhema_ai", "Rhema AI", "Ask about the Bible and faith", Icons.Outlined.AutoAwesome, Color(0xFF5E35B1)),
    HomeCard("forum", "Forum", "Discuss with other learners", Icons.Outlined.Forum, Color(0xFF3949AB)),
    HomeCard("prayer_wall", "Prayer wall", "Share and pray for requests", Icons.Outlined.VolunteerActivism, Color(0xFFC2412D)),
    HomeCard("rewards", "Rewards", "Your points and badges", Icons.Outlined.EmojiEvents, Color(0xFFB7791F)),
)

private val LEADING = listOf(
    HomeCard("learners_progress", "Learners' progress", "Who is moving, who has stalled", Icons.Outlined.Insights, Color(0xFF22745D)),
    HomeCard("members", "My group", "Members of your center", Icons.Outlined.Groups, Color(0xFF1E88E5)),
    HomeCard("live_sessions_host", "Sessions I host", "Schedule and attendance", Icons.Outlined.LiveTv, Color(0xFF6B46C1)),
)

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 4..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(me: Me, offline: Boolean, onRefresh: () -> Unit, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refreshing by remember { mutableStateOf(false) }
    val learning = LEARNING.filter { me.can(it.feature) }
    val leading = LEADING.filter { me.can(it.feature) }
    val open: (HomeCard) -> Unit = { card ->
        if (card.feature in listOf("self_study", "live_sessions", "library")) onOpen(card.feature)
        else Toast.makeText(context, "${card.title} comes in the next update.", Toast.LENGTH_SHORT).show()
    }

    if (refreshing) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            onRefresh()
            delay(900)
            refreshing = false
        }
    }

    PullToRefreshBox(isRefreshing = refreshing, onRefresh = { refreshing = true }, modifier = modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item { Hero(me) }
            if (offline) item { Banner(Icons.Outlined.CloudOff, "You're offline. Showing what was saved on this phone.") }
            if (me.setupStep.isNotEmpty()) item { Banner(Icons.Outlined.TaskAlt, "Finish setting up your account on the DH Workspace website to unlock everything.") }
            if (learning.isNotEmpty()) {
                item { SectionTitle("Learning") }
                items(learning.chunked(2)) { row -> CardRow(row, open) }
            }
            if (leading.isNotEmpty()) {
                item { SectionTitle("Leading") }
                items(leading.chunked(2)) { row -> CardRow(row, open) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Hero(me: Me) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(DhForest, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(greeting() + ",", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.titleMedium)
            Text(
                me.firstName.ifBlank { me.name },
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(me.roleLabel.ifBlank { me.role }, DhLime, Color(0xFF1B1F0A))
                me.center?.let { Chip(it.name, DhGreen, Color.White) }
            }
        }
    }
}

@Composable
private fun Chip(text: String, bg: Color, fg: Color) {
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
    }
}

@Composable
private fun Banner(icon: ImageVector, text: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
    )
}

@Composable
private fun CardRow(row: List<HomeCard>, open: (HomeCard) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        row.forEach { card -> FeatureCard(card, Modifier.weight(1f)) { open(card) } }
        if (row.size == 1) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun FeatureCard(card: HomeCard, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(132.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(40.dp).background(card.tint.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(card.icon, contentDescription = null, tint = card.tint)
            }
            Column {
                Text(card.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(card.line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
