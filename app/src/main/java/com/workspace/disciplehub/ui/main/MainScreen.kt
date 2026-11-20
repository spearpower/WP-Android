package com.workspace.disciplehub.ui.main

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import com.workspace.disciplehub.data.Me
import com.workspace.disciplehub.ui.common.EmptyState
import com.workspace.disciplehub.ui.home.HomeScreen
import com.workspace.disciplehub.ui.learn.LearnScreen
import com.workspace.disciplehub.ui.learn.LearnTarget
import com.workspace.disciplehub.ui.live.LiveScreen
import com.workspace.disciplehub.ui.library.LibraryScreen
import com.workspace.disciplehub.ui.me.MeScreen

enum class Tab(val label: String, val icon: ImageVector, val iconOn: ImageVector, val feature: String?) {
    Home("Home", Icons.Outlined.Home, Icons.Filled.Home, null),
    Learn("Learn", Icons.Outlined.School, Icons.Filled.School, "self_study"),
    Live("Live", Icons.Outlined.LiveTv, Icons.Filled.LiveTv, "live_sessions"),
    Library("Library", Icons.Outlined.LocalLibrary, Icons.Filled.LocalLibrary, "library"),
    Me("Me", Icons.Outlined.Person, Icons.Filled.Person, null),
}

@Composable
fun MainScreen(me: Me, offline: Boolean, onRefresh: () -> Unit, onLogout: () -> Unit) {
    val tabs = Tab.entries.filter { it.feature == null || me.can(it.feature) }
    var current by rememberSaveable { mutableStateOf(Tab.Home) }
    if (current !in tabs) current = Tab.Home
    var learnTarget by remember { mutableStateOf<LearnTarget?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as com.workspace.disciplehub.WpApp

    // A tapped notification: open its tab.
    val route by app.pendingRoute.collectAsState()
    androidx.compose.runtime.LaunchedEffect(route) {
        val r = route ?: return@LaunchedEffect
        val tab = when (r) { "live" -> Tab.Live; "learn" -> Tab.Learn; "library" -> Tab.Library; "me" -> Tab.Me; else -> Tab.Home }
        if (tab in tabs) current = tab
        app.pendingRoute.value = null
    }

    // Push: ask once for permission (Android 13+), then tell the server this phone's token.
    val askPermission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {
        app.scope.launch { com.workspace.disciplehub.push.Push.register(app.api) }
    }
    androidx.compose.runtime.LaunchedEffect(me.userId) {
        if (!com.workspace.disciplehub.push.Push.started) return@LaunchedEffect
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            askPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            com.workspace.disciplehub.push.Push.register(app.api)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // each screen handles the status bar itself
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab,
                        onClick = { current = tab },
                        icon = { Icon(if (current == tab) tab.iconOn else tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.tertiaryContainer),
                    )
                }
            }
        },
    ) { padding ->
        val m = Modifier.padding(padding)
        when (current) {
            Tab.Home -> HomeScreen(
                me, offline, onRefresh,
                onOpen = { feature -> Tab.entries.firstOrNull { it.feature == feature && it in tabs }?.let { current = it } },
                onContinue = { courseId, lessonId, lang ->
                    if (Tab.Learn in tabs) {
                        learnTarget = LearnTarget(courseId, lessonId, lang)
                        current = Tab.Learn
                    }
                },
                modifier = m,
            )
            Tab.Learn -> LearnScreen(me, learnTarget, onTargetUsed = { learnTarget = null }, modifier = m)
            Tab.Live -> LiveScreen(modifier = m)
            Tab.Library -> LibraryScreen(modifier = m)
            Tab.Me -> MeScreen(me, onLogout, modifier = m)
        }
    }
}
