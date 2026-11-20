package com.workspace.disciplehub.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Self Study (api/v1/study.php). Lists and course pages are saved so they open at once and offline. */
class StudyRepository(private val api: ApiClient, private val tokens: TokenStore) {

    suspend fun bootstrap(): StudyBootstrap = cachedCall("study_boot", StudyBootstrap.serializer()) {
        api.get("study.php?action=bootstrap", StudyBootstrap.serializer())
    }

    suspend fun catalogue(lang: String): Catalogue = cachedCall("study_cat_$lang", Catalogue.serializer()) {
        api.get("study.php?action=catalogue&lang=$lang", Catalogue.serializer())
    }

    suspend fun cachedCatalogue(lang: String): Catalogue? = cached("study_cat_$lang", Catalogue.serializer())

    suspend fun course(id: Int, lang: String): CourseReply = cachedCall("study_course_${id}_$lang", CourseReply.serializer()) {
        api.get("study.php?action=course&course_id=$id&lang=$lang", CourseReply.serializer())
    }

    suspend fun cachedCourse(id: Int, lang: String): CourseReply? = cached("study_course_${id}_$lang", CourseReply.serializer())

    /** Seconds really played since the last report: [ranges] = [[from, to], ...]. */
    suspend fun progress(lessonId: Int, lang: String, position: Double, duration: Double, ranges: List<Pair<Int, Int>>, played: Double): ProgressResult =
        api.post(
            "study.php",
            buildJsonObject {
                put("action", "progress")
                put("lesson_id", lessonId)
                put("lang", lang)
                put("position", position)
                put("duration", duration)
                put("played", played)
                put("ranges", JsonArray(ranges.map { JsonArray(listOf(JsonPrimitive(it.first), JsonPrimitive(it.second))) }))
            },
            ProgressResult.serializer(),
        )

    suspend fun startAttempt(courseId: Int, lang: String): Attempt = api.post(
        "study.php",
        buildJsonObject {
            put("action", "start_attempt")
            put("course_id", courseId)
            put("lang", lang)
        },
        Attempt.serializer(),
    )

    suspend fun submitAttempt(attemptId: Int, answers: Map<Int, Set<Int>>): SubmitResult = api.post(
        "study.php",
        buildJsonObject {
            put("action", "submit_attempt")
            put("attempt_id", attemptId)
            put("answers", buildJsonObject {
                answers.forEach { (q, opts) -> put(q.toString(), JsonArray(opts.map { JsonPrimitive(it) })) }
            })
        },
        SubmitResult.serializer(),
    )

    private suspend fun <T> cached(key: String, serializer: kotlinx.serialization.KSerializer<T>): T? =
        tokens.cacheGet(key)?.let { runCatching { AppJson.decodeFromString(serializer, it) }.getOrNull() }

    /** Asks the server; when offline, answers with the saved copy if there is one. */
    private suspend fun <T> cachedCall(key: String, serializer: kotlinx.serialization.KSerializer<T>, call: suspend () -> T): T {
        return try {
            call().also { tokens.cachePut(key, AppJson.encodeToString(serializer, it)) }
        } catch (e: ApiException) {
            if (e.code == "network") cached(key, serializer) ?: throw e else throw e
        }
    }
}
