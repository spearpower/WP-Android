package com.workspace.disciplehub.data

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Sign in, stay signed in, sign out. */
class AuthRepository(private val api: ApiClient, private val tokens: TokenStore, private val device: DeviceInfo) {

    suspend fun isSignedIn(): Boolean = tokens.tokens() != null

    suspend fun login(username: String, password: String): Me {
        val reply = api.post(
            "auth.php",
            buildJsonObject {
                put("action", "login")
                put("username", username.trim())
                put("password", password)
                put("device_id", device.id())
                put("device_name", device.name)
                put("app_version", device.appVersion)
                put("os_version", device.osVersion)
            },
            LoginReply.serializer(),
            auth = false,
        )
        tokens.save(Tokens(reply.accessToken, reply.refreshToken, reply.expiresIn))
        tokens.saveMe(reply.me)
        return reply.me
    }

    /** The person from the server, or the last one saved when the phone is offline. */
    suspend fun me(): Me {
        return try {
            api.get("me.php", Me.serializer()).also { tokens.saveMe(it) }
        } catch (e: ApiException) {
            if (e.code == "network") tokens.lastMe() ?: throw e else throw e
        }
    }

    suspend fun cachedMe(): Me? = tokens.lastMe()

    suspend fun logout() {
        val t = tokens.tokens()
        if (t != null) {
            runCatching {
                api.postRaw("auth.php", buildJsonObject {
                    put("action", "logout")
                    put("refresh_token", t.refreshToken)
                }, auth = false)
            }
        }
        tokens.clear()
    }

    suspend fun ping(): Ping = api.get("ping.php", Ping.serializer(), auth = false)
}
