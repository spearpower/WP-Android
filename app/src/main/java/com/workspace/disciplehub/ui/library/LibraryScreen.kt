package com.workspace.disciplehub.ui.library

import android.app.Application
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.workspace.disciplehub.WpApp
import com.workspace.disciplehub.data.ApiException
import com.workspace.disciplehub.data.Book
import com.workspace.disciplehub.data.Bookmark
import com.workspace.disciplehub.data.LibraryHome
import com.workspace.disciplehub.ui.common.EmptyState
import com.workspace.disciplehub.ui.pdf.PdfPages
import com.workspace.disciplehub.ui.theme.DhForest
import com.workspace.disciplehub.ui.theme.DhGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as WpApp).library
    var home by mutableStateOf<LibraryHome?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var open by mutableStateOf<Book?>(null)
        private set
    var file by mutableStateOf<File?>(null)
        private set
    var fileError by mutableStateOf<String?>(null)
        private set
    var bookmarks by mutableStateOf<List<Bookmark>>(emptyList())
        private set

    // Pages seen since the last report (the server counts new pages for Rewards and finishing).
    private val seen = mutableSetOf<Int>()
    var page = 1
    var pages = 0

    init {
        viewModelScope.launch {
            home = repo.cached()
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                home = repo.home(); error = null
            } catch (e: ApiException) {
                if (home == null) error = e.message
            }
        }
    }

    fun openBook(b: Book) {
        open = b; file = null; fileError = null; bookmarks = emptyList(); seen.clear(); page = b.progress?.page ?: 1; pages = b.pages
        viewModelScope.launch {
            try {
                file = repo.pdf(b)
            } catch (e: ApiException) {
                fileError = e.message
            }
        }
        viewModelScope.launch { runCatching { repo.book(b.id) }.getOrNull()?.let { bookmarks = it.bookmarks } }
    }

    fun onPage(p: Int, total: Int) {
        page = p; pages = total; seen += p
    }

    fun sync(onFinished: (String) -> Unit = {}) {
        val b = open ?: return
        if (seen.isEmpty() || pages <= 0) return
        val list = seen.toList()
        seen.clear()
        val p = page
        val n = pages
        viewModelScope.launch {
            try {
                val r = repo.progress(b.id, p, n, list)
                if (r.justFinished) onFinished(b.title)
            } catch (e: ApiException) {
                seen += list
            }
        }
    }

    fun close() {
        sync()
        open = null
        file = null
        refresh()
    }

    fun addBookmark(note: String) {
        val b = open ?: return
        val p = page
        viewModelScope.launch { runCatching { repo.addBookmark(b.id, p, note) }.getOrNull()?.let { bookmarks = it } }
    }

    fun deleteBookmark(id: Int) {
        val b = open ?: return
        viewModelScope.launch { runCatching { repo.deleteBookmark(b.id, id) }.getOrNull()?.let { bookmarks = it } }
    }
}

@Composable
fun LibraryScreen(modifier: Modifier = Modifier, vm: LibraryViewModel = viewModel()) {
    val book = vm.open
    if (book != null) {
        Reader(vm, book, modifier)
    } else {
        Shelves(vm, modifier)
    }
}

@Composable
private fun Shelves(vm: LibraryViewModel, modifier: Modifier) {
    val home = vm.home
    var query by remember { mutableStateOf("") }
    val books = home?.books.orEmpty().filter { query.isBlank() || it.title.contains(query, true) || it.author.contains(query, true) }
    val reading = books.filter { it.progress != null && !it.progress.finished }.sortedByDescending { it.progress?.last }
    LazyColumn(modifier.fillMaxSize()) {
        item {
            Column(
                Modifier.fillMaxWidth().background(DhForest, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Library", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text("Search books or authors", color = Color.White.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.White) },
                    shape = RoundedCornerShape(50),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.6f), unfocusedBorderColor = Color.White.copy(alpha = 0.3f), cursorColor = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        vm.error?.let { item { EmptyState(Icons.Outlined.LocalLibrary, "The Library could not load", it, Modifier.height(320.dp)) } }
        if (reading.isNotEmpty()) {
            item { ShelfTitle("Continue reading") }
            item { BookRow(reading, vm::openBook) }
        }
        home?.themes?.forEach { shelf ->
            val onShelf = books.filter { it.theme == shelf.id }
            if (onShelf.isNotEmpty()) {
                item { ShelfTitle(shelf.name) }
                item { BookRow(onShelf, vm::openBook) }
            }
        }
        val others = books.filter { b -> home?.themes?.none { it.id == b.theme } != false }
        if (others.isNotEmpty()) {
            item { ShelfTitle("More books") }
            item { BookRow(others, vm::openBook) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ShelfTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp))
}

@Composable
private fun BookRow(books: List<Book>, onOpen: (Book) -> Unit) {
    LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(books, key = { it.id }) { b ->
            Card(
                onClick = { onOpen(b) },
                modifier = Modifier.width(128.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Box {
                    if (b.cover.isNotBlank()) {
                        AsyncImage(model = b.cover, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(0.7f).background(MaterialTheme.colorScheme.surfaceVariant))
                    } else {
                        Box(Modifier.fillMaxWidth().aspectRatio(0.7f).background(DhGreen), contentAlignment = Alignment.Center) {
                            Text(b.title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp), maxLines = 5, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Column(Modifier.padding(8.dp)) {
                    Text(b.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (b.author.isNotBlank()) Text(b.author, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    b.progress?.let { p ->
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(progress = { p.percent / 100f }, modifier = Modifier.fillMaxWidth().height(4.dp), color = DhGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant, drawStopIndicator = {})
                    }
                }
            }
        }
    }
}

@Composable
private fun Reader(vm: LibraryViewModel, book: Book, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var page by remember { mutableIntStateOf(vm.page) }
    var total by remember { mutableIntStateOf(book.pages) }
    var showMarks by remember { mutableStateOf(false) }
    val finished: (String) -> Unit = { t -> Toast.makeText(context, "You finished \"$t\". 30 points added!", Toast.LENGTH_LONG).show() }

    BackHandler { vm.close() }
    LaunchedEffect(book.id) {
        while (true) {
            delay(15_000)
            vm.sync(finished)
        }
    }
    DisposableEffect(book.id) { onDispose { vm.sync() } }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(DhForest).statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.close() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text(book.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (total > 0) "Page $page of $total" else "Opening…", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = { showMarks = true }) { Icon(Icons.Outlined.Bookmark, contentDescription = "Bookmarks", tint = Color.White) }
            IconButton(onClick = { vm.addBookmark(""); Toast.makeText(context, "Page $page bookmarked", Toast.LENGTH_SHORT).show() }) {
                Icon(Icons.Outlined.BookmarkAdd, contentDescription = "Bookmark this page", tint = Color.White)
            }
            val f = vm.file
            if (book.download && f != null) {
                IconButton(onClick = {
                    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", f)
                    val send = Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(Intent.createChooser(send, "Save or share the book"))
                }) { Icon(Icons.Outlined.Share, contentDescription = "Save or share", tint = Color.White) }
            }
        }
        val f = vm.file
        when {
            f != null -> PdfPages(f, vm.page, listState, onPage = { p, n -> page = p; total = n; vm.onPage(p, n) })
            vm.fileError != null -> EmptyState(Icons.Outlined.LocalLibrary, "The book could not open", vm.fileError ?: "")
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator()
                    Text("Downloading the book (${book.size / 1024 / 1024 + 1} MB). It stays on this phone for reading offline.",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 32.dp))
                }
            }
        }
    }
    if (showMarks) {
        AlertDialog(
            onDismissRequest = { showMarks = false },
            title = { Text("Bookmarks") },
            text = {
                if (vm.bookmarks.isEmpty()) Text("No bookmarks yet. Tap the bookmark button to mark a page.")
                else Column {
                    vm.bookmarks.forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showMarks = false; scope.launch { listState.scrollToItem(m.page - 1) } }, modifier = Modifier.weight(1f)) {
                                Text("Page ${m.page}" + if (m.note.isNotBlank()) " · ${m.note}" else "", modifier = Modifier.fillMaxWidth())
                            }
                            IconButton(onClick = { vm.deleteBookmark(m.id) }) { Icon(Icons.Outlined.Delete, contentDescription = "Remove", modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMarks = false }) { Text("Close") } },
        )
    }
}

