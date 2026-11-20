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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workspace.disciplehub.data.Home
import com.workspace.disciplehub.data.Learning
import com.workspace.disciplehub.data.Me
import com.workspace.disciplehub.data.Points
import com.workspace.disciplehub.data.SessionItem
import com.workspace.disciplehub.ui.common.minutesUntil
import com.workspace.disciplehub.ui.common.openUrl
import com.workspace.disciplehub.ui.common.sessionWhen
import com.workspace.disciplehub.ui.theme.DhForest
import com.workspace.disciplehub.ui.theme.DhGreen
import com.workspace.disciplehub.ui.theme.DhLime
import com.workspace.disciplehub.ui.theme.DhOnLime
import java.util.Calendar

/** A shortcut card: which feature it opens, its colour. */
private data class HomeCard(val feature: String, val title: String, val line: String, val icon: ImageVector, val tint: Color)

private val EXPLORE = listOf(
    HomeCard("self_study", "Self Study", "All courses and lessons", Icons.Outlined.School, Color(0xFF22745D)),
    HomeCard("live_sessions", "Live sessions", "Classes on Zoom", Icons.Outlined.LiveTv, Color(0xFF1E88E5)),
    HomeCard("library", "Library", "Books to read and download", Icons.Outlined.LocalLibrary, Color(0xFF6B46C1)),
    HomeCard("fun_quiz", "Fun Quiz", "Test what you learned", Icons.Outlined.Quiz, Color(0xFFD69E2E)),
    HomeCard("bible", "My Bible", "Read and look up verses", Icons.AutoMirrored.Outlined.MenuBook, Color(0xFF8D6E63)),
    HomeCard("reading_plans", "Reading plans", "Keep your daily streak", Icons.Outlined.TaskAlt, Color(0xFF00897B)),
    HomeCard("rhema_ai", "Rhema AI", "Ask about the Bible and faith", Icons.Outlined.AutoAwesome, Color(0xFF5E35B1)),
    HomeCard("forum", "Forum", "Discuss with other learners", Icons.Outlined.Forum, Color(0xFF3949AB)),
    HomeCard("prayer_wall", "Prayer wall", "Share and pray for requests", Icons.Outlined.VolunteerActivism, Color(0xFFC2412D)),
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
fun HomeScreen(
    me: Me,
    offline: Boolean,
    onRefresh: () -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    vm: HomeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val home: Home? = vm.home
    val explore = EXPLORE.filter { me.can(it.feature) }
    val leading = LEADING.filter { me.can(it.feature) }
    val open: (HomeCard) -> Unit = { card ->
        if (card.feature in listOf("self_study", "live_sessions", "library")) onOpen(card.feature)
        else Toast.makeText(context, "${card.title} comes in a coming update.", Toast.LENGTH_SHORT).show()
    }

    PullToRefreshBox(
        isRefreshing = vm.loading && home != null,
        onRefresh = {
            onRefresh()
            vm.refresh()
        },
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            item { Hero(me, home?.points) }
            if (offline) item { Banner(Icons.Outlined.CloudOff, "You're offline. Showing what was saved on this phone.") }
            if (me.setupStep.isNotEmpty()) item { Banner(Icons.Outlined.TaskAlt, "Finish setting up your account on the DH Workspace website to unlock everything.") }
            vm.error?.let { item { Banner(Icons.Outlined.CloudOff, it) } }

            home?.learning?.let { learning ->
                item { ContinueCard(learning) { onOpen("self_study") } }
                item { StatsRow(learning) }
            }
            if (home == null && vm.loading) item { LoadingCard() }

            home?.sessions?.let { sessions ->
                item { SectionTitle("Live sessions", action = if (me.can("live_sessions")) "See all" else null) { onOpen("live_sessions") } }
                if (sessions.isEmpty()) {
                    item { EmptyLine("No live sessions for you in the next two weeks.") }
                } else {
                    items(sessions, key = { it.id.toString() + it.start }) { s -> SessionCard(s) { openUrl(context, s.joinUrl) } }
                }
            }

            if (explore.isNotEmpty()) {
                item { SectionTitle("Explore") }
                items(explore.chunked(2)) { row -> CardRow(row, open) }
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
private fun Hero(me: Me, points: Points?) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(DhForest, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 22.dp),
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
                Chip(me.roleLabel.ifBlank { me.role }, DhLime, DhOnLime)
                me.center?.let { Chip(it.name, DhGreen, Color.White) }
            }
            points?.let { p ->
                Spacer(Modifier.height(8.dp))
                Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = DhLime, modifier = Modifier.size(30.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${p.month} points in ${p.monthLabel.substringBefore(' ')}", color = Color.White, fontWeight = FontWeight.Bold)
                            Text(
                                (p.monthRank?.let { "#$it on this month's board · " } ?: "") + "${p.year} this year",
                                color = Color.White.copy(alpha = 0.75f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueCard(l: Learning, onGo: () -> Unit) {
    val c = l.continueLesson
    val first = l.firstCourse
    Card(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                c != null && !c.courseDone -> {
                    Text("Continue learning", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Text(c.courseTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(c.lessonTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Progress(c.coursePercent)
                    PrimaryButton("Continue", onGo)
                }
                c != null -> {
                    Text("Well done!", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Text("You finished ${c.courseTitle}.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Choose your next course in Self Study.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PrimaryButton("Find a course", onGo)
                }
                else -> {
                    Text("Start learning", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Text(first?.title ?: "Choose your first course", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Watch the lessons in your language, pass the assessment and earn a certificate.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PrimaryButton("Start", onGo)
                }
            }
        }
    }
}

@Composable
private fun Progress(percent: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier.weight(1f).height(8.dp),
            color = DhGreen,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {},
        )
        Text("$percent%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = DhLime, contentColor = DhOnLime),
        shape = RoundedCornerShape(50),
        modifier = Modifier.height(44.dp),
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatsRow(l: Learning) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Stat("${l.percent}%", "Overall", Icons.Outlined.Insights, Modifier.weight(1f))
        Stat(l.lessonsCompleted.toString(), "Lessons", Icons.Outlined.TaskAlt, Modifier.weight(1f))
        Stat(if (l.weekMinutes > 0) "${l.weekMinutes}m" else "0", "This week", Icons.Outlined.School, Modifier.weight(1f))
        Stat(l.certificates.toString(), "Certificates", Icons.Outlined.WorkspacePremium, Modifier.weight(1f))
    }
}

@Composable
private fun Stat(value: String, label: String, icon: ImageVector, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SessionCard(s: SessionItem, onJoin: () -> Unit) {
    val soon = (minutesUntil(s.start) ?: Long.MAX_VALUE) <= 15
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).background(Color(0xFF1E88E5).copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Videocam, contentDescription = null, tint = Color(0xFF1E88E5))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (s.live) {
                        Surface(color = Color(0xFFC2412D), shape = RoundedCornerShape(6.dp)) {
                            Text("LIVE", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text(s.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(sessionWhen(s.start), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (s.joinUrl.isNotBlank()) {
                if (s.live || soon) {
                    Button(onClick = onJoin, colors = ButtonDefaults.buttonColors(containerColor = DhGreen), shape = RoundedCornerShape(50)) { Text("Join") }
                } else {
                    OutlinedButton(onClick = onJoin, shape = RoundedCornerShape(50)) { Text("Join") }
                }
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp).height(150.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {}
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
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
private fun SectionTitle(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        action?.let { androidx.compose.material3.TextButton(onClick = onAction) { Text(it) } }
    }
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
        modifier = modifier.height(124.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(38.dp).background(card.tint.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(card.icon, contentDescription = null, tint = card.tint)
            }
            Column {
                Text(card.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(card.line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

