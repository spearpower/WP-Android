package com.workspace.disciplehub.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** api/v1/live.php */
@Serializable
data class LiveReply(val sessions: List<LiveSession> = emptyList())

@Serializable
data class LiveSession(
    val id: Int,
    val title: String = "",
    val agenda: String = "",
    val start: String,
    val end: String,
    val live: Boolean = false,
    val past: Boolean = false,
    @SerialName("join_url") val joinUrl: String = "",
    val passcode: String = "",
    @SerialName("my_role") val myRole: String = "",
    @SerialName("can_start") val canStart: Boolean = false,
    val recurrence: String = "",
    val facilitators: List<String> = emptyList(),
)

class LiveRepository(private val api: ApiClient, private val tokens: TokenStore) {
    suspend fun cached(): LiveReply? = tokens.cacheGet("live")?.let { runCatching { AppJson.decodeFromString(LiveReply.serializer(), it) }.getOrNull() }

    suspend fun load(): LiveReply = api.get("live.php", LiveReply.serializer()).also {
        tokens.cachePut("live", AppJson.encodeToString(LiveReply.serializer(), it))
    }
}
