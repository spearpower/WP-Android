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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
