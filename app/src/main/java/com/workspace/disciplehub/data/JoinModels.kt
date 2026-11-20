package com.workspace.disciplehub.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class JoinResult(val username: String = "", val sent: Boolean = false, @SerialName("sent_to") val sentTo: String = "", val channel: String = "EMAIL")

@Serializable
data class Welcome(
    val step: String = "",
    val privacy: Privacy? = null,
    val places: Map<String, List<Place>> = emptyMap(),
    val centres: List<Centre> = emptyList(),
)

@Serializable
data class Privacy(val version: Int = 1, val text: String = "")

@Serializable
data class Place(val id: Int, val name: String = "")

@Serializable
data class Centre(val id: Int, val name: String = "", val country: String = "", val district: String = "", val category: String = "")

@Serializable
data class StepReply(val step: String = "")

class JoinRepository(private val api: ApiClient) {
    suspend fun submit(first: String, last: String, email: String, phone: String, cc: String, channel: String, usernameKind: String, own: String, started: Long): JoinResult =
        api.post(
            "join.php",
            buildJsonObject {
                put("action", "submit"); put("first_name", first); put("last_name", last); put("email", email); put("phone", phone); put("cc", cc)
                put("channel", channel); put("username_kind", usernameKind); put("own", own); put("website", ""); put("started", started)
            },
            JoinResult.serializer(), auth = false,
        )

    suspend fun welcome(): Welcome = api.get("welcome.php", Welcome.serializer())

    suspend fun step(action: String, vararg fields: Pair<String, String>): String = api.post(
        "welcome.php", buildJsonObject { put("action", action); fields.forEach { (k, v) -> put(k, v) } }, StepReply.serializer(),
    ).step
}
