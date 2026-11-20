package com.workspace.disciplehub.ui.learn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.workspace.disciplehub.data.Question
import com.workspace.disciplehub.data.SubmitResult
import com.workspace.disciplehub.ui.common.EmptyState
import com.workspace.disciplehub.ui.common.openUrl
import com.workspace.disciplehub.ui.theme.DhGreen
import com.workspace.disciplehub.ui.theme.DhLime
import com.workspace.disciplehub.ui.theme.DhOnLime

private val Wrong = Color(0xFFC2412D)

@Composable
fun QuizScreen(vm: LearnViewModel, courseId: Int) {
    val attempt = vm.attempt
    val result = vm.result
    attempt?.let { trueFalseWords = it.trueFalse }
    Column(Modifier.fillMaxSize()) {
        TopBar(attempt?.assessment?.title?.ifBlank { null } ?: "Assessment") { vm.back() }
        when {
            result != null -> ResultView(result, onBack = { vm.back() })
            attempt != null -> Questions(vm, attempt.questions, attempt.assessment.passPercent, attempt.assessment.instructions)
            vm.quizBusy -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> EmptyState(Icons.Outlined.Quiz, "The assessment could not start", vm.quizError ?: "Please try again.")
        }
    }
}

@Composable
private fun Questions(vm: LearnViewModel, questions: List<Question>, passPercent: Int, instructions: String) {
    val answers = remember { mutableStateMapOf<Int, Set<Int>>() }
    var confirm by remember { mutableStateOf(false) }
    val unanswered = questions.count { answers[it.id].isNullOrEmpty() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("${questions.size} questions · pass mark $passPercent%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (instructions.isNotBlank()) Text(instructions, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        itemsIndexed(questions, key = { _, q -> q.id }) { i, q ->
            QuestionCard(i + 1, q, answers[q.id].orEmpty()) { optionId ->
                val now = answers[q.id].orEmpty()
                answers[q.id] = if (q.type == "MULTIPLE") (if (optionId in now) now - optionId else now + optionId) else setOf(optionId)
            }
        }
        item {
            vm.quizError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp)) }
            Button(
                onClick = { if (unanswered > 0) confirm = true else vm.submitQuiz(answers.toMap()) },
                enabled = !vm.quizBusy,
                colors = ButtonDefaults.buttonColors(containerColor = DhLime, contentColor = DhOnLime),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (vm.quizBusy) CircularProgressIndicator(Modifier.size(22.dp), color = DhOnLime, strokeWidth = 2.5.dp)
                else Text("Submit my answers", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Submit now?") },
            text = { Text("$unanswered question(s) have no answer. Unanswered questions score nothing.") },
            confirmButton = { TextButton(onClick = { confirm = false; vm.submitQuiz(answers.toMap()) }) { Text("Submit") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep answering") } },
        )
    }
}

@Composable
private fun QuestionCard(number: Int, q: Question, chosen: Set<Int>, onPick: (Int) -> Unit) {
    val multiple = q.type == "MULTIPLE"
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Question $number" + if (multiple) " · choose all that apply" else "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Text(q.prompt, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            q.options.forEach { o ->
                val on = o.id in chosen
                Row(
                    Modifier.fillMaxWidth()
                        .selectable(selected = on, onClick = { onPick(o.id) }, role = if (multiple) Role.Checkbox else Role.RadioButton)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (multiple) Checkbox(checked = on, onCheckedChange = null) else RadioButton(selected = on, onClick = null)
                    Spacer(Modifier.size(8.dp))
                    Text(optionText(q, o.text), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/** True/false options are named by position in the course language (as on the web). */
private var trueFalseWords: List<String> = listOf("True", "False")

private fun optionText(q: Question, text: String): String {
    if (q.type != "TRUE_FALSE") return text
    val i = q.options.indexOfFirst { it.text == text }
    return trueFalseWords.getOrNull(if (i <= 0) 0 else 1) ?: text
}

@Composable
private fun ResultView(r: SubmitResult, onBack: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Surface(shape = RoundedCornerShape(22.dp), color = if (r.passed) DhGreen else MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(if (r.passed) Icons.Outlined.WorkspacePremium else Icons.Outlined.Quiz, contentDescription = null, modifier = Modifier.size(44.dp),
                        tint = if (r.passed) Color.White else MaterialTheme.colorScheme.onErrorContainer)
                    Text("${r.percent.toInt()}%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black,
                        color = if (r.passed) Color.White else MaterialTheme.colorScheme.onErrorContainer)
                    Text(
                        if (r.passed) "Well done! You passed and earned a certificate." else "Not yet. You need ${r.passPercent}% to pass. Watch the lessons again and try once more.",
                        color = if (r.passed) Color.White else MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text("${r.score} of ${r.max} points", color = if (r.passed) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                r.certificateUrl?.let { url ->
                    Button(onClick = { openUrl(context, url) }, colors = ButtonDefaults.buttonColors(containerColor = DhLime, contentColor = DhOnLime)) { Text("View certificate") }
                }
                OutlinedButton(onClick = onBack) { Text("Back to the course") }
            }
        }
        item { Text("Your answers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        itemsIndexed(r.review, key = { _, q -> q.id }) { i, q -> ReviewCard(i + 1, q) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ReviewCard(number: Int, q: Question) {
    val ok = q.isCorrect == true
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (ok) DhGreen else Wrong),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if (ok) Icons.Filled.CheckCircle else Icons.Filled.Cancel, contentDescription = if (ok) "Correct" else "Not correct", tint = if (ok) DhGreen else Wrong, modifier = Modifier.size(18.dp))
                Text("Question $number", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(q.prompt, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            q.options.forEach { o ->
                val picked = o.id in q.chosen
                val right = o.correct == true
                val mark = when {
                    right -> "✓ "
                    picked -> "✗ "
                    else -> "   "
                }
                Text(
                    mark + optionText(q, o.text) + if (picked) "  (your answer)" else "",
                    color = when {
                        right -> DhGreen
                        picked -> Wrong
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (right || picked) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.clickable(enabled = false) {},
                )
            }
            if (q.explanation.isNotBlank()) Text(q.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
