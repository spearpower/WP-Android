package com.workspace.disciplehub.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.workspace.disciplehub.BuildConfig
import com.workspace.disciplehub.data.ApiClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.coroutines.resume
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Push notifications (Firebase Cloud Messaging). Firebase is started from BuildConfig (local.properties fcm.*), so no
 * google-services.json is needed in the project; without those values push stays off and the app works as before.
 * Channels let each person switch a kind of notice off in the phone's own settings.
 */
object Push {
    val CHANNELS = listOf(
        Triple("live", "Live sessions", "Reminders before your sessions start"),
        Triple("learning", "Learning", "New lessons, courses, quizzes and reading"),
        Triple("community", "Community", "Prayer wall, messages, center news"),
        Triple("rewards", "Rewards", "Points, badges and awards"),
        Triple("general", "General", "Other notices from DH Workspace"),
    )

    val configured: Boolean
        get() = BuildConfig.FCM_APP_ID.isNotBlank() && BuildConfig.FCM_API_KEY.isNotBlank() && BuildConfig.FCM_PROJECT_ID.isNotBlank()

    var started = false
        private set

    /** What happened at the last registration, shown on Me (e.g. "On" or the error from Google). */
    var status by androidx.compose.runtime.mutableStateOf("Not set up")
        private set

    fun start(context: Context) {
        createChannels(context)
        if (!configured || started) return
        runCatching {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(
                    context,
                    FirebaseOptions.Builder()
                        .setApplicationId(BuildConfig.FCM_APP_ID)
                        .setApiKey(BuildConfig.FCM_API_KEY)
                        .setProjectId(BuildConfig.FCM_PROJECT_ID)
                        .setGcmSenderId(BuildConfig.FCM_SENDER_ID)
                        .build(),
                )
            }
            started = true
            status = "Starting"
        }.onFailure { status = "Firebase could not start: ${it.message}"; android.util.Log.w("DHPush", "init failed", it) }
    }

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        CHANNELS.forEach { (id, name, desc) ->
            nm.createNotificationChannel(NotificationChannel(id, name, if (id == "live") NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT).apply { description = desc })
        }
    }

    /** Tells the server this phone's push token (after sign-in, and whenever Firebase gives a new one). */
    suspend fun register(api: ApiClient, token: String? = null) {
        if (!started) return
        val t = token ?: currentToken() ?: return
        runCatching {
            api.postRaw("devices.php", buildJsonObject {
                put("action", "register")
                put("push_token", t)
            })
        }.onSuccess { status = "On" }.onFailure { status = "Registered with Google, but the server did not save it: ${it.message}" }
    }

    private suspend fun currentToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cont.resume(task.result)
            } else {
                val e = task.exception
                val reason = generateSequence<Throwable>(e) { it.cause }.mapNotNull { it.message }.distinct().joinToString(" / ")
                status = "Google refused: $reason"
                android.util.Log.w("DHPush", "token failed: $reason", e)
                cont.resume(null)
            }
        }
    }
}
