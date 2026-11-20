package com.workspace.disciplehub.ui.web

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.outlined.CloudOff
import com.workspace.disciplehub.WpApp
import com.workspace.disciplehub.data.ApiException
import com.workspace.disciplehub.ui.common.EmptyState
import com.workspace.disciplehub.ui.learn.TopBar
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * A website module inside the app (Forum, Prayer Wall, Bible, Rhema AI, Fun Quiz, Reading Plans, Rewards) until it is
 * built natively. The app asks for a one-time sign-in link (api/v1/web_code.php), so the page opens signed in.
 * Links to other sites open in the phone's browser.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebScreen(title: String, section: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as WpApp
    var url by remember(section) { mutableStateOf<String?>(null) }
    var error by remember(section) { mutableStateOf<String?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var web by remember { mutableStateOf<WebView?>(null) }
    val siteHost = remember { Uri.parse(app.api.siteBase).host }

    LaunchedEffect(section) {
        try {
            val r = app.api.postRaw("web_code.php", buildJsonObject { put("to", section) })
            url = r.jsonObject["url"]?.jsonPrimitive?.content
        } catch (e: ApiException) {
            error = e.message
        }
    }
    BackHandler { if (web?.canGoBack() == true) web?.goBack() else onBack() }

    Column(Modifier.fillMaxSize()) {
        TopBar(title, onBack)
        if (progress in 1..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        val u = url
        when {
            error != null -> EmptyState(androidx.compose.material.icons.Icons.Outlined.CloudOff, "This page could not open", error ?: "")
            u == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        CookieManager.getInstance().setAcceptCookie(true)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.userAgentString = settings.userAgentString + " DHWorkspaceApp/" + com.workspace.disciplehub.BuildConfig.VERSION_NAME
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) { progress = newProgress }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val target = request.url
                                if (target.host == siteHost && (target.scheme == "http" || target.scheme == "https")) return false
                                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, target)) }
                                return true
                            }

                            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) { progress = 5 }
                        }
                        loadUrl(u)
                        web = this
                    }
                },
                onRelease = { it.destroy() },
            )
        }
    }
}

