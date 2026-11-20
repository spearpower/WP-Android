package com.workspace.disciplehub.ui.pdf

import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** One open PDF. Android's PdfRenderer allows one page at a time, so pages are drawn one after another. */
class PdfDoc(file: File) : AutoCloseable {
    private val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(fd)
    private val lock = Mutex()
    val pageCount: Int = renderer.pageCount
    val ratios: List<Float> = (0 until pageCount).map { i -> renderer.openPage(i).use { it.width.toFloat() / it.height.coerceAtLeast(1) } }

    suspend fun render(index: Int, widthPx: Int): Bitmap = lock.withLock {
        withContext(Dispatchers.IO) {
            renderer.openPage(index).use { page ->
                val w = widthPx.coerceIn(200, 2000)
                val h = (w / (page.width.toFloat() / page.height)).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(AColor.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bmp
            }
        }
    }

    override fun close() {
        runCatching { renderer.close() }
        runCatching { fd.close() }
    }
}

/**
 * Pages one under another, fit to the width; pinch to zoom, double-tap to zoom in or out.
 * [onPage] reports the page most on screen (1-based).
 */
@Composable
fun PdfPages(file: File, startPage: Int, listState: LazyListState = rememberLazyListState(), onPage: (Int, Int) -> Unit = { _, _ -> }, modifier: Modifier = Modifier) {
    val doc = remember(file) { runCatching { PdfDoc(file) }.getOrNull() }
    DisposableEffect(doc) { onDispose { doc?.close() } }
    if (doc == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("This PDF could not be opened.") }
        return
    }
    LaunchedEffect(doc) { if (startPage > 1) listState.scrollToItem((startPage - 1).coerceIn(0, doc.pageCount - 1)) }
    LaunchedEffect(listState, doc) {
        androidx.compose.runtime.snapshotFlow { listState.firstVisibleItemIndex + if (listState.firstVisibleItemScrollOffset > 300) 1 else 0 }
            .collect { onPage((it + 1).coerceAtMost(doc.pageCount), doc.pageCount) }
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val zoom = rememberTransformableState { z, pan, _ ->
        scale = (scale * z).coerceIn(1f, 4f)
        offset = if (scale == 1f) androidx.compose.ui.geometry.Offset.Zero else offset + pan
    }
    BoxWithConstraints(
        modifier.fillMaxSize().background(Color(0xFF3A3F3E))
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { scale = if (scale > 1f) 1f else 2f; if (scale == 1f) offset = androidx.compose.ui.geometry.Offset.Zero }) }
            .transformable(zoom, lockRotationOnZoomPan = true, enabled = true),
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
        ) {
            items(doc.pageCount) { i ->
                val bmp by produceState<Bitmap?>(null, i, widthPx) { value = doc.render(i, widthPx) }
                Box(Modifier.fillMaxWidth().padding(vertical = 3.dp).aspectRatio(doc.ratios[i]).background(Color.White), contentAlignment = Alignment.Center) {
                    val b = bmp
                    if (b == null) CircularProgressIndicator(Modifier.padding(24.dp))
                    else Image(b.asImageBitmap(), contentDescription = "Page ${i + 1}", modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
