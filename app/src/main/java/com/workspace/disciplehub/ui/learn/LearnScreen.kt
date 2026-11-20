package com.workspace.disciplehub.ui.learn

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.workspace.disciplehub.data.AssessmentSummary
import com.workspace.disciplehub.data.Course
import com.workspace.disciplehub.data.CourseCard
import com.workspace.disciplehub.data.Lesson
import com.workspace.disciplehub.data.Me
import com.workspace.disciplehub.ui.common.EmptyState
import com.workspace.disciplehub.ui.common.openUrl
import com.workspace.disciplehub.ui.theme.DhForest
import com.workspace.disciplehub.ui.theme.DhGreen
import com.workspace.disciplehub.ui.theme.DhLime
import com.workspace.disciplehub.ui.theme.DhOnLime

/** A request from Home: open this course (and lesson) in this language. */
data class LearnTarget(val courseId: Int, val lessonId: Int?, val lang: String?)

@Composable
fun LearnScreen(me: Me, target: LearnTarget?, onTargetUsed: () -> Unit, modifier: Modifier = Modifier, vm: LearnViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.start(me.language) }
    LaunchedEffect(target) {
        target?.let {
            vm.openFromHome(it.courseId, it.lessonId, it.lang)
            onTargetUsed()
        }
    }
    BackHandler(enabled = vm.stack.size > 1) { vm.back() }

    Box(modifier.fillMaxSize()) {
        when (val p = vm.page) {
            LearnPage.Catalogue -> CatalogueView(vm)
            is LearnPage.CoursePage -> CourseView(vm, p.courseId)
            is LearnPage.LessonPage -> LessonScreen(vm, p.courseId, p.lessonId)
            is LearnPage.QuizPage -> QuizScreen(vm, p.courseId)
        }
    }
}

// ---- Courses --------------------------------------------------------------------------------------------------
@Composable
private fun CatalogueView(vm: LearnViewModel) {
    val cat = vm.catalogue
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(
                Modifier.fillMaxWidth().background(DhForest, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Self Study", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Choose your language", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.languages.forEach { l ->
                        FilterChip(
                            selected = l.code == vm.lang,
                            onClick = { vm.chooseLang(l.code) },
                            label = { Text(l.native.ifBlank { l.name }) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                labelColor = Color.White,
                                selectedContainerColor = DhLime,
                                selectedLabelColor = DhOnLime,
                            ),
                            border = null,
                        )
                    }
                }
            }
        }
        vm.error?.takeIf { cat == null }?.let { item { EmptyState(Icons.Outlined.School, "Courses could not load", it, Modifier.height(320.dp)) } }
        if (cat != null && cat.categories.isEmpty()) {
            item { EmptyState(Icons.Outlined.School, "No courses yet", "There are no courses in this language yet. Try another language.", Modifier.height(320.dp)) }
        }
        cat?.categories?.forEach { category ->
            item {
                Text(category.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp))
            }
            items(category.courses, key = { "c${category.id}_${it.id}" }) { c -> CourseCardView(c) { vm.openCourse(c.id) } }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun CourseCardView(c: CourseCard, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = c.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(112.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(c.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${c.lessons} lessons · ${minutes(c.duration)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                c.progress?.let { p ->
                    val pct = if (c.lessons > 0) (p.completed * 100 / c.lessons).coerceAtMost(100) else 0
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.weight(1f).height(6.dp), color = DhGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant, drawStopIndicator = {})
                        Text(if (pct >= 100) "Done" else "$pct%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun minutes(seconds: Int): String = when {
    seconds <= 0 -> "video"
    seconds < 3600 -> "${(seconds + 59) / 60} min"
    else -> "${seconds / 3600} h ${(seconds % 3600) / 60} min"
}

// ---- One course -----------------------------------------------------------------------------------------------
@Composable
private fun CourseView(vm: LearnViewModel, courseId: Int) {
    val course = vm.course?.takeIf { it.id == courseId }
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize()) {
        item { TopBar(course?.title ?: "Course") { vm.back() } }
        if (course == null) {
            item {
                if (vm.error != null) EmptyState(Icons.Outlined.School, "This course could not load", vm.error ?: "", Modifier.height(320.dp))
                else Surface(Modifier.fillMaxWidth().padding(16.dp).height(160.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {}
            }
            return@LazyColumn
        }
        item { CourseHeader(course) }
        course.sections.forEach { section ->
            item {
                Text(section.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp))
            }
            items(section.lessons, key = { "l${it.id}" }) { lesson ->
                LessonRow(lesson, course.lessons.indexOf(lesson) + 1) { vm.openLesson(course.id, lesson.id) }
            }
        }
        course.assessment?.let { a -> item { AssessmentCard(a, onStart = { vm.openQuiz(course.id) }, onCertificate = { a.certificateUrl?.let { openUrl(context, it) } }) } }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(DhForest).statusBarsPadding().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
        Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 12.dp))
    }
}

@Composable
private fun CourseHeader(course: Course) {
    val withVideo = course.lessons.filter { it.media != null }
    val done = withVideo.count { it.progress?.completed == true }
    val pct = if (withVideo.isNotEmpty()) done * 100 / withVideo.size else 0
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(course.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (course.description.isNotBlank()) {
            Text(course.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 6, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.weight(1f).height(8.dp), color = DhGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant, drawStopIndicator = {})
            Text("$done / ${withVideo.size} lessons", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LessonRow(lesson: Lesson, number: Int, onClick: () -> Unit) {
    val playable = lesson.media != null
    val done = lesson.progress?.completed == true
    Row(
        Modifier.fillMaxWidth().clickable(enabled = playable, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(if (done) DhGreen else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            when {
                done -> Icon(Icons.Filled.CheckCircle, contentDescription = "Finished", tint = Color.White)
                playable -> Text("$number", fontWeight = FontWeight.Bold)
                else -> Icon(Icons.Outlined.VideocamOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(lesson.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = if (playable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            val line = when {
                !playable -> "Not yet in this language"
                done -> "Finished"
                (lesson.progress?.percent ?: 0.0) > 0 -> "${lesson.progress!!.percent.toInt()}% watched"
                else -> minutes(lesson.media?.duration ?: 0)
            }
            Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (playable) Icon(Icons.Filled.PlayCircle, contentDescription = "Play", tint = DhGreen)
    }
}

@Composable
private fun AssessmentCard(a: AssessmentSummary, onStart: () -> Unit, onCertificate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(if (a.passed) Icons.Outlined.WorkspacePremium else Icons.Outlined.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(a.title.ifBlank { "Assessment" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            Text(
                "${a.questions} questions · pass mark ${a.passPercent}%" + (a.bestPercent?.let { " · your best ${it.toInt()}%" } ?: ""),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            when {
                a.passed -> {
                    Text("You passed and earned a certificate.", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (a.certificateUrl != null) Button(onClick = onCertificate, colors = ButtonDefaults.buttonColors(containerColor = DhGreen)) { Text("View certificate") }
                        OutlinedButton(onClick = onStart) { Text("Try again") }
                    }
                }
                a.unlocked -> Button(onClick = onStart, colors = ButtonDefaults.buttonColors(containerColor = DhLime, contentColor = DhOnLime)) { Text("Start the assessment", fontWeight = FontWeight.Bold) }
                else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text("Opens when you finish all lessons (${a.lessonsDone} of ${a.lessonsTotal} done).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
    }
}
