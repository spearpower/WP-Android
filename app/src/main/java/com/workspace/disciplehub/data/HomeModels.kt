package com.workspace.disciplehub.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** api/v1/home.php: each part is null when the role does not include it. */
@Serializable
data class Home(
    val learning: Learning? = null,
    val sessions: List<SessionItem>? = null,
    val points: Points? = null,
)

@Serializable
data class Learning(
    @SerialName("courses_started") val coursesStarted: Int = 0,
    @SerialName("courses_finished") val coursesFinished: Int = 0,
    @SerialName("lessons_completed") val lessonsCompleted: Int = 0,
    val percent: Int = 0,
    val hours: Double = 0.0,
    val certificates: Int = 0,
    @SerialName("week_minutes") val weekMinutes: Int = 0,
    @SerialName("week_active_days") val weekActiveDays: Int = 0,
    @SerialName("continue") val continueLesson: ContinueLesson? = null,
    @SerialName("first_course") val firstCourse: FirstCourse? = null,
)

@Serializable
data class ContinueLesson(
    @SerialName("course_id") val courseId: Int,
    @SerialName("lesson_id") val lessonId: Int,
    val lang: String = "en",
    @SerialName("course_title") val courseTitle: String = "",
    @SerialName("lesson_title") val lessonTitle: String = "",
    @SerialName("course_percent") val coursePercent: Int = 0,
    @SerialName("course_done") val courseDone: Boolean = false,
)

@Serializable
data class FirstCourse(@SerialName("course_id") val courseId: Int, val title: String = "")

@Serializable
data class SessionItem(
    val id: Int,
    val title: String = "",
    val start: String,
    val end: String,
    val live: Boolean = false,
    @SerialName("can_start") val canStart: Boolean = false,
    @SerialName("join_url") val joinUrl: String = "",
)

@Serializable
data class Points(
    val month: Int = 0,
    val year: Int = 0,
    @SerialName("all_time") val allTime: Int = 0,
    @SerialName("month_rank") val monthRank: Int? = null,
    @SerialName("month_label") val monthLabel: String = "",
)
