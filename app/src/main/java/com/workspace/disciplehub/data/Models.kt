package com.workspace.disciplehub.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val AppJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}

/** The signed-in person (api/v1/me_data.php). */
@Serializable
data class Me(
    @SerialName("user_id") val userId: Int,
    val username: String = "",
    val name: String = "",
    @SerialName("first_name") val firstName: String = "",
    val role: String = "",
    @SerialName("role_label") val roleLabel: String = "",
    val center: Center? = null,
    @SerialName("photo_url") val photoUrl: String = "",
    val language: String = "en",
    @SerialName("setup_step") val setupStep: String = "",
    val features: Map<String, Boolean> = emptyMap(),
) {
    fun can(feature: String): Boolean = features[feature] == true
}

@Serializable
data class Center(val id: Int, val name: String = "", val country: String = "")

@Serializable
data class Tokens(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int = 3600,
)

@Serializable
data class LoginReply(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int = 3600,
    val me: Me,
)

@Serializable
data class Ping(
    @SerialName("api_version") val apiVersion: String = "",
    @SerialName("min_app_version") val minAppVersion: Int = 1,
    val maintenance: Boolean = false,
    @SerialName("join_url") val joinUrl: String = "",
    @SerialName("reset_url") val resetUrl: String = "",
)

/** A failed call: [code] from the server (invalid_credentials, token_expired, ...) or "network" / "server". */
class ApiException(val code: String, override val message: String, val http: Int = 0) : Exception(message)
