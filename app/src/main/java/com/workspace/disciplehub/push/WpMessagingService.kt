package com.workspace.disciplehub.push

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.workspace.disciplehub.MainActivity
import com.workspace.disciplehub.R
import com.workspace.disciplehub.WpApp
import kotlinx.coroutines.launch

/**
 * Notices from DH Workspace (cfg/app_push.php). In the background Android shows them itself and a tap opens the app
 * with the message's data (route); in the foreground this service shows them.
 */
class WpMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val app = application as WpApp
        app.scope.launch { if (app.auth.isSignedIn()) Push.register(app.api, token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val n = message.notification ?: return
        val channel = message.data["channel"]?.takeIf { c -> Push.CHANNELS.any { it.first == c } } ?: "general"
        val open = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        message.data.forEach { (k, v) -> open.putExtra(k, v) }
        val pending = PendingIntent.getActivity(this, (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF22745D.toInt())
            .setContentTitle(n.title)
            .setContentText(n.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        runCatching { NotificationManagerCompat.from(this).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification) }
    }
}
