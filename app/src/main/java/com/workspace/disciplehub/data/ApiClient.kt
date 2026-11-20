package com.workspace.disciplehub.data

import com.workspace.disciplehub.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Talks to DH Workspace's app API (BuildConfig.API_BASE = .../api/v1/). Replies are {"ok", "data"} or {"ok": false,
 * "error": {code, message}}. A 401 "token_expired" renews the sign-in once with the refresh token and repeats the call;
 * when that fails too, [onSignedOut] runs and the app goes back to the sign-in screen.
 */
class ApiClient(private val base: String, private val tokens: TokenStore, private val device: DeviceInfo) {
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", "DHWorkspace-Android/${BuildConfig.VERSION_NAME}").build())
        }
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply { level = okhttp3.logging.HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    /** The web address of the Workspace itself (API_BASE without "api/v1/"), for links like Join and Forgot password. */
    val siteBase: String = base.removeSuffix("/").removeSuffix("/v1").removeSuffix("/api") + "/"

    var onSignedOut: (() -> Unit)? = null

    /** For pictures and files from DH Workspace: adds the sign-in to requests for our own server only. */
    val authedHttp: OkHttpClient by lazy {
        val host = base.toHttpUrlOrNull()?.host
        http.newBuilder().addInterceptor { chain ->
            val req = chain.request()
            val token = tokens.accessNow()
            chain.proceed(if (token != null && req.url.host == host) req.newBuilder().header("Authorization", "Bearer $token").build() else req)
        }.build()
    }

    /**
     * Downloads [path] (relative to API_BASE) into [dest] with the sign-in, renewing it once when it has expired.
     * The file is written to a temporary name first, so a broken download never replaces a good copy.
     */
    suspend fun download(path: String, dest: java.io.File) {
        val first = tokens.tokens() ?: throw ApiException("unauthorized", "Sign in to continue.", 401)
        val status = fetchTo(path, dest, first.accessToken)
        if (status == 401) {
            val fresh = refreshLock.withLock {
                val current = tokens.tokens()
                if (current != null && current.accessToken != first.accessToken) current else renew(current)
            } ?: run {
                signedOut()
                throw ApiException("token_expired", "Your sign-in has expired. Sign in again.", 401)
            }
            val again = fetchTo(path, dest, fresh.accessToken)
            if (again != 200) throw ApiException("server", "The file could not be downloaded (HTTP $again).", again)
        } else if (status != 200) {
            throw ApiException(if (status == 404) "not_found" else "server", if (status == 404) "This file is not available." else "The file could not be downloaded (HTTP $status).", status)
        }
    }

    private suspend fun fetchTo(path: String, dest: java.io.File, access: String): Int = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(base + path).header("Authorization", "Bearer $access").build()
        try {
            http.newCall(request).execute().use { r ->
                if (r.code == 200) {
                    dest.parentFile?.mkdirs()
                    val tmp = java.io.File(dest.path + ".part")
                    r.body.byteStream().use { input -> tmp.outputStream().use { input.copyTo(it) } }
                    if (!tmp.renameTo(dest)) {
                        dest.delete()
                        tmp.renameTo(dest)
                    }
                }
                r.code
            }
        } catch (e: IOException) {
            throw ApiException("network", "No connection to DH Workspace. Check your internet and try again.")
        }
    }
    private val refreshLock = Mutex()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun <T> get(path: String, serializer: KSerializer<T>, auth: Boolean = true): T =
        AppJson.decodeFromJsonElement(serializer, call(path, null, auth))

    suspend fun <T> post(path: String, body: JsonObject, serializer: KSerializer<T>, auth: Boolean = true): T =
        AppJson.decodeFromJsonElement(serializer, call(path, body, auth))

    suspend fun postRaw(path: String, body: JsonObject, auth: Boolean = true): JsonElement = call(path, body, auth)

    private suspend fun call(path: String, body: JsonObject?, auth: Boolean): JsonElement {
        val used = if (auth) tokens.tokens() ?: throw ApiException("unauthorized", "Sign in to continue.", 401) else null
        try {
            return send(path, body, used?.accessToken)
        } catch (e: ApiException) {
            if (!auth || e.http != 401) throw e
        }
        // The access token expired: renew once (other calls waiting here reuse the new one), then repeat.
        val fresh = refreshLock.withLock {
            val current = tokens.tokens()
            if (current != null && current.accessToken != used?.accessToken) current else renew(current)
        } ?: run {
            signedOut()
            throw ApiException("token_expired", "Your sign-in has expired. Sign in again.", 401)
        }
        try {
            return send(path, body, fresh.accessToken)
        } catch (e: ApiException) {
            if (e.http == 401) signedOut()
            throw e
        }
    }

    private suspend fun renew(current: Tokens?): Tokens? {
        current ?: return null
        return try {
            val body = buildJsonObject {
                put("action", "refresh")
                put("refresh_token", current.refreshToken)
                put("device_id", device.id())
            }
            val t = AppJson.decodeFromJsonElement(Tokens.serializer(), send("auth.php", body, null))
            tokens.save(t)
            t
        } catch (e: ApiException) {
            if (e.code == "network") throw e // offline: keep the sign-in, try again later
            null
        }
    }

    private suspend fun signedOut() {
        tokens.clear()
        onSignedOut?.invoke()
    }

    private suspend fun send(path: String, body: JsonObject?, access: String?): JsonElement = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(base + path).apply {
            access?.let { header("Authorization", "Bearer $it") }
            header("Accept", "application/json")
            if (body != null) post(body.toString().toRequestBody(jsonType)) else get()
        }.build()
        val text: String
        val status: Int
        try {
            http.newCall(request).execute().use { r ->
                status = r.code
                text = r.body.string()
            }
        } catch (e: IOException) {
            throw ApiException("network", "No connection to DH Workspace. Check your internet and try again.")
        }
        val root = runCatching { AppJson.parseToJsonElement(text).jsonObject }.getOrNull()
            ?: throw ApiException("server", "DH Workspace sent an answer the app could not read (HTTP $status).", status)
        if (root["ok"]?.jsonPrimitive?.booleanOrNull == true) {
            root["data"] ?: JsonObject(emptyMap())
        } else {
            val err = root["error"]?.jsonObject
            throw ApiException(
                err?.get("code")?.jsonPrimitive?.content ?: "server",
                err?.get("message")?.jsonPrimitive?.content ?: "Something went wrong (HTTP $status).",
                status,
            )
        }
    }
}
