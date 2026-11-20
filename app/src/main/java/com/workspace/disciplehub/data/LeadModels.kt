package com.workspace.disciplehub.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** api/v1/lead.php */
@Serializable
data class LearnersReply(val learners: List<LearnerRow> = emptyList())

@Serializable
data class LearnerRow(
    @SerialName("user_id") val userId: Int,
    val name: String = "",
    val centre: String = "",
    @SerialName("last_seen") val lastSeen: String = "",
    val courses: List<LearnerCourse> = emptyList(),
)

@Serializable
data class LearnerCourse(
    @SerialName("course_id") val courseId: Int,
    val title: String = "",
    val percent: Int = 0,
    @SerialName("lessons_done") val lessonsDone: Int = 0,
    @SerialName("lessons_total") val lessonsTotal: Int = 0,
    val passed: Boolean = false,
    val best: Double? = null,
    val certificate: Boolean = false,
)

@Serializable
data class MembersReply(val members: List<MemberRow> = emptyList(), val total: Int = 0)

@Serializable
data class MemberRow(
    @SerialName("member_id") val memberId: Int,
    val name: String = "",
    val centre: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val email: String = "",
    val stage: String = "",
    @SerialName("photo_url") val photoUrl: String = "",
)

@Serializable
data class GatheringsReply(val gatherings: List<Gathering> = emptyList())

@Serializable
data class Gathering(
    @SerialName("activity_id") val activityId: Int,
    val title: String = "",
    val date: String = "",
    val centre: String = "",
    val type: String = "",
    val location: String = "",
    val present: Int = 0,
)

@Serializable
data class Roster(val title: String = "", val date: String = "", val people: List<RosterPerson> = emptyList(), val present: Int = 0)

@Serializable
data class RosterPerson(
    @SerialName("member_id") val memberId: Int,
    val name: String = "",
    val photo: String = "",
    val centre: String = "",
    val present: Boolean = false,
    val method: String = "",
)

@Serializable
data class CheckinQr(val url: String = "", val qr: String = "", @SerialName("valid_until") val validUntil: String = "", val title: String = "")

@Serializable
data class CheckinResult(val ok: Boolean = false, val message: String = "", val title: String = "")

@Serializable
data class PresentCount(val present: Int = 0)

class LeadRepository(private val api: ApiClient) {
    suspend fun learners(): List<LearnerRow> = api.get("lead.php?action=learners", LearnersReply.serializer()).learners
    suspend fun members(): MembersReply = api.get("lead.php?action=members", MembersReply.serializer())
    suspend fun gatherings(): List<Gathering> = api.get("lead.php?action=gatherings", GatheringsReply.serializer()).gatherings
    suspend fun roster(id: Int): Roster = api.get("lead.php?action=roster&activity_id=$id", Roster.serializer())
    suspend fun mark(id: Int, member: Int, present: Boolean): Int = api.post(
        "lead.php", buildJsonObject { put("action", "mark"); put("activity_id", id); put("member_id", member); put("present", if (present) "1" else "0") }, PresentCount.serializer(),
    ).present
    suspend fun qr(id: Int, renew: Boolean = false): CheckinQr = api.get("lead.php?action=qr&activity_id=$id" + if (renew) "&renew=1" else "", CheckinQr.serializer())
    suspend fun checkin(code: String): CheckinResult = api.post("lead.php", buildJsonObject { put("action", "checkin"); put("code", code) }, CheckinResult.serializer())
}
