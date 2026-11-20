package com.workspace.disciplehub.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// api/v1/study.php (Self Study): the same data as the web's learner page.

@Serializable
data class StudyBootstrap(
    val languages: List<StudyLang> = emptyList(),
    @SerialName("last_lang") val lastLang: String? = null,
    @SerialName("complete_percent") val completePercent: Int = 90,
)

@Serializable
data class StudyLang(val code: String, val name: String = "", val native: String = "", val rtl: Boolean = false, val color: String = "", val courses: Int = 0)

@Serializable
data class Catalogue(val lang: String = "en", val categories: List<Category> = emptyList())

@Serializable
data class Category(val id: Int, val title: String = "", val courses: List<CourseCard> = emptyList())

@Serializable
data class CourseCard(
    val id: Int,
    val title: String = "",
    val description: String = "",
    val status: String = "PUBLISHED",
    val lessons: Int = 0,
    val duration: Int = 0,
    val thumbnail: String = "",
    val progress: CourseProgress? = null,
)

@Serializable
data class CourseProgress(val completed: Int = 0, val started: Int = 0)

@Serializable
data class CourseReply(val lang: String = "en", val course: Course)

@Serializable
data class Course(
    val id: Int,
    val title: String = "",
    val description: String = "",
    val status: String = "PUBLISHED",
    val sections: List<Section> = emptyList(),
    val assessment: AssessmentSummary? = null,
) {
    val lessons: List<Lesson> get() = sections.flatMap { it.lessons }
}

@Serializable
data class Section(val id: Int, val title: String = "", val description: String = "", val lessons: List<Lesson> = emptyList())

@Serializable
data class Lesson(
    val id: Int,
    val title: String = "",
    val description: String = "",
    val media: Media? = null,
    val progress: LessonProgress? = null,
)

@Serializable
data class Media(
    @SerialName("media_id") val mediaId: Int = 0,
    val type: String = "",
    val duration: Int = 0,
    @SerialName("youtube_id") val youtubeId: String = "",
    val thumbnail: String = "",
)

@Serializable
data class LessonProgress(val completed: Boolean = false, val percent: Double = 0.0, val position: Int = 0)

@Serializable
data class AssessmentSummary(
    val id: Int,
    val title: String = "",
    val instructions: String = "",
    val questions: Int = 0,
    @SerialName("pass_percent") val passPercent: Int = 70,
    @SerialName("lessons_total") val lessonsTotal: Int = 0,
    @SerialName("lessons_done") val lessonsDone: Int = 0,
    val unlocked: Boolean = false,
    val passed: Boolean = false,
    @SerialName("best_percent") val bestPercent: Double? = null,
    val certificate: String? = null,
    @SerialName("certificate_url") val certificateUrl: String? = null,
)

@Serializable
data class ProgressResult(
    @SerialName("lesson_id") val lessonId: Int,
    val percent: Double = 0.0,
    val position: Int = 0,
    val completed: Boolean = false,
    @SerialName("newly_completed") val newlyCompleted: Boolean = false,
)

@Serializable
data class Attempt(
    @SerialName("attempt_id") val attemptId: Int,
    val assessment: AssessmentSummary,
    val questions: List<Question> = emptyList(),
    @SerialName("true_false") val trueFalse: List<String> = listOf("True", "False"),
)

@Serializable
data class Question(
    val id: Int,
    val type: String = "SINGLE",
    val points: Int = 1,
    val prompt: String = "",
    val options: List<QuestionOption> = emptyList(),
    val explanation: String = "",
    val chosen: List<Int> = emptyList(),
    @SerialName("is_correct") val isCorrect: Boolean? = null,
)

@Serializable
data class QuestionOption(val id: Int, val text: String = "", val correct: Boolean? = null)

@Serializable
data class SubmitResult(
    @SerialName("attempt_id") val attemptId: Int,
    val score: Int = 0,
    val max: Int = 0,
    val percent: Double = 0.0,
    @SerialName("pass_percent") val passPercent: Int = 70,
    val passed: Boolean = false,
    val certificate: String? = null,
    @SerialName("certificate_url") val certificateUrl: String? = null,
    val review: List<Question> = emptyList(),
)
