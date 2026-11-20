package com.workspace.disciplehub.ui.learn

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.workspace.disciplehub.ui.theme.DhGreen
import com.workspace.disciplehub.ui.theme.DhLime
import com.workspace.disciplehub.ui.theme.DhOnLime
import kotlinx.coroutines.delay

/**
 * Counts only the seconds really played (skipping ahead does not count), like the web player (js/quicklearn.js):
 * small [from, to] ranges are sent every 15 seconds, when playback pauses or ends, and when the lesson closes.
 */
private class WatchTracker {
    var playing = false
    private var last: Float? = null
    var position = 0f
    var duration = 0f
    val pending = mutableListOf<Pair<Int, Int>>()
    var played = 0.0

    fun onSecond(t: Float) {
        val prev = last
        if (playing && prev != null && t > prev && t - prev <= 3f) {
            pending += (kotlin.math.floor(prev).toInt() to kotlin.math.ceil(t).toInt())
            played += (t - prev)
        }
        last = if (playing) t else null
        position = t
    }

    fun take(): Pair<List<Pair<Int, Int>>, Double> {
        val out = merge(pending)
        val p = played
        pending.clear()
        played = 0.0
        return out to p
    }

    fun giveBack(ranges: List<Pair<Int, Int>>, p: Double) {
        pending += ranges
        played += p
    }

    fun hasNews() = pending.isNotEmpty() || played >= 1.0

    private fun merge(list: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
        val sorted = list.filter { it.second > it.first }.sortedBy { it.first }
        val out = mutableListOf<Pair<Int, Int>>()
        for (r in sorted) {
            val lastR = out.lastOrNull()
            if (lastR != null && r.first <= lastR.second + 1) out[out.lastIndex] = lastR.first to maxOf(lastR.second, r.second) else out += r
        }
        return out
    }
}

@Composable
fun LessonScreen(vm: LearnViewModel, courseId: Int, lessonId: Int) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val course = vm.course?.takeIf { it.id == courseId }
    val lessons = course?.lessons.orEmpty()
    val lesson = lessons.firstOrNull { it.id == lessonId }
    val next = lessons.dropWhile { it.id != lessonId }.drop(1).firstOrNull { it.media != null }
    val tracker = remember(lessonId) { WatchTracker() }
    var completed by remember(lessonId) { mutableStateOf(lesson?.progress?.completed == true) }
    var percent by remember(lessonId) { mutableStateOf(lesson?.progress?.percent ?: 0.0) }
    var ended by remember(lessonId) { mutableStateOf(false) }
    // Full screen: the player hands over its view; it is shown in a landscape dialog over everything.
    var fullView by remember { mutableStateOf<View?>(null) }
    var exitFull by remember { mutableStateOf<(() -> Unit)?>(null) }
    val activity = context.findActivity()

    fun sync() {
        if (!tracker.hasNews()) return
        val (ranges, played) = tracker.take()
        vm.sendProgress(
            lessonId, tracker.position.toDouble(), tracker.duration.toDouble(), ranges, played,
            onDone = { r ->
                percent = maxOf(percent, r.percent)
                if (r.newlyCompleted) {
                    completed = true
                    Toast.makeText(context, "Lesson finished", Toast.LENGTH_SHORT).show()
                } else if (r.completed) completed = true
            },
            onFail = { r, p -> tracker.giveBack(r, p) },
        )
    }

    // Report every 15 seconds while the lesson is open, and once more when it closes.
    LaunchedEffect(lessonId) {
        while (true) {
            delay(15_000)
            sync()
        }
    }
    DisposableEffect(lessonId) { onDispose { sync() } }
    DisposableEffect(Unit) { onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED } }

    fullView?.let { view ->
        Dialog(
            onDismissRequest = { exitFull?.invoke() },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            BackHandler { exitFull?.invoke() }
            val window = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            DisposableEffect(window) {
                window?.let {
                    WindowCompat.setDecorFitsSystemWindows(it, false)
                    WindowInsetsControllerCompat(it, it.decorView).apply {
                        hide(WindowInsetsCompat.Type.systemBars())
                        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
                onDispose { }
            }
            AndroidView(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                factory = { (view.parent as? ViewGroup)?.removeView(view); view },
                onRelease = { (it.parent as? ViewGroup)?.removeView(it) },
            )
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(lesson?.title ?: "Lesson") { vm.back() }
        val videoId = lesson?.media?.youtubeId.orEmpty()
        if (videoId.isNotEmpty()) {
            val start = lesson?.progress?.takeIf { !it.completed }?.position?.toFloat() ?: 0f
            key(lessonId) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black),
                    factory = { ctx ->
                        YouTubePlayerView(ctx).apply {
                            enableAutomaticInitialization = false
                            addFullscreenListener(object : FullscreenListener {
                                override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {
                                    fullView = fullscreenView
                                    exitFull = exitFullscreen
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                }

                                override fun onExitFullscreen() {
                                    fullView = null
                                    exitFull = null
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                }
                            })
                            lifecycleOwner.lifecycle.addObserver(this)
                            val options = IFramePlayerOptions.Builder(ctx).controls(1).rel(0).ivLoadPolicy(3).ccLoadPolicy(1).fullscreen(1).build()
                            initialize(object : AbstractYouTubePlayerListener() {
                                override fun onReady(youTubePlayer: YouTubePlayer) {
                                    youTubePlayer.cueVideo(videoId, start)
                                }

                                override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                                    tracker.onSecond(second)
                                }

                                override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                                    if (duration > 0f) tracker.duration = duration
                                }

                                override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                                    tracker.playing = state == PlayerConstants.PlayerState.PLAYING
                                    if (state == PlayerConstants.PlayerState.PAUSED || state == PlayerConstants.PlayerState.ENDED) sync()
                                    if (state == PlayerConstants.PlayerState.ENDED) ended = true
                                }
                            }, true, options)
                        }
                    },
                    onRelease = { view ->
                        lifecycleOwner.lifecycle.removeObserver(view)
                        view.release()
                    },
                )
            }
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(lesson?.title.orEmpty(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (completed) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = DhGreen)
                    Text("Finished", color = DhGreen, fontWeight = FontWeight.Bold)
                } else {
                    LinearProgressIndicator(progress = { (percent / 100.0).toFloat() }, modifier = Modifier.weight(1f).height(6.dp), color = DhGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant, drawStopIndicator = {})
                    Text("${percent.toInt()}% watched", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (!completed) {
                Text("Watch at least 90% of the video to finish the lesson. Skipping ahead does not count.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (lesson?.description?.isNotBlank() == true) {
                Text(lesson.description, style = MaterialTheme.typography.bodyMedium)
            }
            if (next != null) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(if (ended) "Up next" else "Next lesson", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(next.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2)
                        }
                        Button(
                            onClick = { sync(); vm.replaceLesson(courseId, next.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = DhLime, contentColor = DhOnLime),
                        ) {
                            Text("Next")
                            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            } else if (course?.assessment != null) {
                Button(onClick = { sync(); vm.back() }, colors = ButtonDefaults.buttonColors(containerColor = DhGreen)) { Text("Back to the course and assessment") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}


private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
