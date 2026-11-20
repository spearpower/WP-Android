package com.workspace.disciplehub.ui.lead

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.workspace.disciplehub.WpApp
import com.workspace.disciplehub.data.ApiException
import com.workspace.disciplehub.data.CheckinQr
import com.workspace.disciplehub.data.Gathering
import com.workspace.disciplehub.data.LearnerRow
import com.workspace.disciplehub.data.MembersReply
import com.workspace.disciplehub.data.Roster
import com.workspace.disciplehub.ui.common.EmptyState
import com.workspace.disciplehub.ui.common.openUrl
import com.workspace.disciplehub.ui.learn.TopBar
import com.workspace.disciplehub.ui.theme.DhGreen
import com.workspace.disciplehub.ui.theme.DhLime
import com.workspace.disciplehub.ui.theme.DhOnLime
import kotlinx.coroutines.launch

class LeadViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as WpApp).lead
    var learners by mutableStateOf<List<LearnerRow>?>(null); private set
    var members by mutableStateOf<MembersReply?>(null); private set
    var gatherings by mutableStateOf<List<Gathering>?>(null); private set
    var roster by mutableStateOf<Roster?>(null); private set
    var qr by mutableStateOf<CheckinQr?>(null); private set
    var error by mutableStateOf<String?>(null); private set
    var openGathering by mutableStateOf<Gathering?>(null); private set

    private fun run(block: suspend () -> Unit) = viewModelScope.launch {
        try { error = null; block() } catch (e: ApiException) { error = e.message }
    }

    fun loadLearners() = run { learners = repo.learners() }
    fun loadMembers() = run { members = repo.members() }
    fun loadGatherings() = run { gatherings = repo.gatherings() }
    fun open(g: Gathering?) {
        openGathering = g; roster = null; qr = null
        if (g != null) run { roster = repo.roster(g.activityId) }
    }
    fun mark(memberId: Int, present: Boolean) {
        val g = openGathering ?: return
        roster = roster?.let { r -> r.copy(people = r.people.map { if (it.memberId == memberId) it.copy(present = present) else it }) }
        run { val n = repo.mark(g.activityId, memberId, present); roster = roster?.copy(present = n) }
    }
    fun showQr(renew: Boolean = false) {
        val g = openGathering ?: return
        run { qr = repo.qr(g.activityId, renew) }
    }
    fun hideQr() { qr = null }
}

private fun dial(context: Context, number: String) = openUrl(context, "tel:" + number.filter { it.isDigit() || it == '+' })
private fun whatsapp(context: Context, number: String) = openUrl(context, "https://wa.me/" + number.filter { it.isDigit() })

@Composable
private fun SearchBox(q: String, onQ: (String) -> Unit, hint: String) {
    OutlinedTextField(q, onQ, singleLine = true, placeholder = { Text(hint) }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
        shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
}

// ---- Learners' progress -----------------------------------------------------------------------------------------
@Composable
fun LearnersScreen(onBack: () -> Unit, vm: LeadViewModel = viewModel()) {
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { vm.loadLearners() }
    var q by remember { mutableStateOf("") }
    var openId by remember { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize()) {
        TopBar("Learners' progress", onBack)
        val list = vm.learners
        if (list == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { if (vm.error != null) Text(vm.error ?: "", Modifier.padding(24.dp)) else CircularProgressIndicator() }
            return@Column
        }
        SearchBox(q, { q = it }, "Search a learner or center")
        val shown = list.filter { q.isBlank() || it.name.contains(q, true) || it.centre.contains(q, true) }
        if (shown.isEmpty()) EmptyState(Icons.Outlined.Insights, "No learners", "Nobody in your centers has started a course yet.")
        LazyColumn(Modifier.fillMaxSize()) {
            items(shown, key = { it.userId }) { l ->
                val best = l.courses.maxOfOrNull { it.percent } ?: 0
                Column(Modifier.fillMaxWidth().clickable { openId = if (openId == l.userId) null else l.userId }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(l.name, fontWeight = FontWeight.Bold)
                            Text(l.centre + (if (l.lastSeen.isNotBlank()) " · last active ${l.lastSeen.take(10)}" else ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${l.courses.size} course${if (l.courses.size == 1) "" else "s"}", style = MaterialTheme.typography.labelMedium)
                    }
                    LinearProgressIndicator(progress = { best / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(5.dp), color = DhGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant, drawStopIndicator = {})
                    if (openId == l.userId) {
                        l.courses.forEach { c ->
                            Text("• ${c.title}: ${c.lessonsDone}/${c.lessonsTotal} lessons" + (if (c.passed) " · passed" else c.best?.let { " · best ${it.toInt()}%" } ?: "") + if (c.certificate) " · certificate" else "",
                                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

// ---- My group ---------------------------------------------------------------------------------------------------
@Composable
fun MembersScreen(onBack: () -> Unit, vm: LeadViewModel = viewModel()) {
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { vm.loadMembers() }
    val context = LocalContext.current
    var q by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        TopBar("My group" + (vm.members?.let { " (${it.total})" } ?: ""), onBack)
        val list = vm.members?.members
        if (list == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { if (vm.error != null) Text(vm.error ?: "", Modifier.padding(24.dp)) else CircularProgressIndicator() }
            return@Column
        }
        SearchBox(q, { q = it }, "Search a name, center or phone")
        val shown = list.filter { q.isBlank() || it.name.contains(q, true) || it.centre.contains(q, true) || it.phone.contains(q) }
        if (shown.isEmpty()) EmptyState(Icons.Outlined.Groups, "No members", "No active members match.")
        LazyColumn(Modifier.fillMaxSize()) {
            items(shown, key = { it.memberId }) { m ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AsyncImage(m.photoUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                    Column(Modifier.weight(1f)) {
                        Text(m.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOf(m.centre, m.stage).filter(String::isNotBlank).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    if (m.phone.isNotBlank()) IconButton(onClick = { dial(context, m.phone) }) { Icon(Icons.Outlined.Call, "Call ${m.name}", tint = DhGreen) }
                    val wa = m.whatsapp.ifBlank { m.phone }
                    if (wa.isNotBlank()) IconButton(onClick = { whatsapp(context, wa) }) { Icon(Icons.Outlined.Chat, "WhatsApp ${m.name}", tint = DhGreen) }
                }
            }
        }
    }
}

// ---- Attendance -------------------------------------------------------------------------------------------------
@Composable
fun AttendanceScreen(onBack: () -> Unit, vm: LeadViewModel = viewModel()) {
    val g = vm.openGathering
    BackHandler { if (vm.qr != null) vm.hideQr() else if (g != null) vm.open(null) else onBack() }
    LaunchedEffect(Unit) { vm.loadGatherings() }
    Column(Modifier.fillMaxSize()) {
        TopBar(g?.title ?: "Attendance") { if (vm.qr != null) vm.hideQr() else if (g != null) vm.open(null) else onBack() }
        val qr = vm.qr
        when {
            qr != null -> QrView(qr, onRenew = { vm.showQr(true) })
            g != null -> RosterView(vm)
            vm.gatherings == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { if (vm.error != null) Text(vm.error ?: "", Modifier.padding(24.dp)) else CircularProgressIndicator() }
            vm.gatherings!!.isEmpty() -> EmptyState(Icons.Outlined.EventAvailable, "No gatherings", "Add the gathering as an activity on the website first (from 14 days ago to 14 days ahead show here).")
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(vm.gatherings!!, key = { it.activityId }) { a ->
                    Column(Modifier.fillMaxWidth().clickable { vm.open(a) }.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(a.title, fontWeight = FontWeight.Bold)
                        Text("${a.date} · ${a.centre}" + (if (a.type.isNotBlank()) " · ${a.type}" else ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${a.present} present", style = MaterialTheme.typography.labelMedium, color = DhGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun RosterView(vm: LeadViewModel) {
    val r = vm.roster
    if (r == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { if (vm.error != null) Text(vm.error ?: "", Modifier.padding(24.dp)) else CircularProgressIndicator() }
        return
    }
    var q by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${r.present} of ${r.people.size} present", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(onClick = { vm.showQr() }, colors = ButtonDefaults.buttonColors(containerColor = DhLime, contentColor = DhOnLime)) {
                Icon(Icons.Outlined.QrCode2, null); Spacer(Modifier.size(6.dp)); Text("Show QR")
            }
        }
        SearchBox(q, { q = it }, "Search a name")
        LazyColumn(Modifier.fillMaxSize()) {
            items(r.people.filter { q.isBlank() || it.name.contains(q, true) }, key = { it.memberId }) { p ->
                Row(Modifier.fillMaxWidth().clickable { vm.mark(p.memberId, !p.present) }.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = p.present, onCheckedChange = { vm.mark(p.memberId, it) })
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontWeight = FontWeight.SemiBold)
                        Text(p.centre + if (p.method == "QR") " · checked in with QR" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun QrView(qr: CheckinQr, onRenew: () -> Unit) {
    val bitmap = remember(qr.qr) {
        runCatching { Base64.decode(qr.qr.substringAfter("base64,"), Base64.DEFAULT).let { BitmapFactory.decodeByteArray(it, 0, it.size) } }.getOrNull()
    }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
        Text("Scan to check in", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Members open DH Workspace → Scan to check in, or scan with the phone camera.", style = MaterialTheme.typography.bodyMedium)
        Surface(shape = RoundedCornerShape(20.dp), color = androidx.compose.ui.graphics.Color.White) {
            bitmap?.let { Image(it.asImageBitmap(), "Check-in QR code", modifier = Modifier.size(280.dp).padding(12.dp)) }
        }
        Text("Valid until ${qr.validUntil.take(16)}", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onRenew) { Text("Make a new code") }
    }
}

// ---- QR check-in for everyone -------------------------------------------------------------------------------------
/** Opens Google's code scanner (no camera permission needed) and checks the person in. */
fun scanAndCheckIn(context: Context, app: WpApp) {
    val scanner = GmsBarcodeScanning.getClient(context, GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAutoZoom().build())
    scanner.startScan()
        .addOnSuccessListener { code ->
            val text = code.rawValue.orEmpty()
            app.scope.launch {
                val msg = try { app.lead.checkin(text).let { r -> (if (r.title.isNotBlank()) r.title + ": " else "") + r.message } } catch (e: ApiException) { e.message }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
        .addOnFailureListener { Toast.makeText(context, "The scanner could not start: ${it.message}", Toast.LENGTH_LONG).show() }
}
