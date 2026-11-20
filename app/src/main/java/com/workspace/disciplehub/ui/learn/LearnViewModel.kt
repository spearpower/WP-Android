package com.workspace.disciplehub.ui.learn

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.workspace.disciplehub.WpApp
import com.workspace.disciplehub.data.ApiException
import com.workspace.disciplehub.data.Attempt
import com.workspace.disciplehub.data.Catalogue
import com.workspace.disciplehub.data.Course
import com.workspace.disciplehub.data.LessonProgress
import com.workspace.disciplehub.data.ProgressResult
import com.workspace.disciplehub.data.StudyLang
import com.workspace.disciplehub.data.SubmitResult
import kotlinx.coroutines.launch

/** Where the learner is inside the Learn tab (the tab keeps its own back stack). */
sealed interface LearnPage {
    data object Catalogue : LearnPage
    data class CoursePage(val courseId: Int) : LearnPage
    data class LessonPage(val courseId: Int, val lessonId: Int) : LearnPage
    data class QuizPage(val courseId: Int) : LearnPage
}

class LearnViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as WpApp).study

    val stack = mutableStateListOf<LearnPage>(LearnPage.Catalogue)
    val page: LearnPage get() = stack.last()

    var languages by mutableStateOf<List<StudyLang>>(emptyList())
        private set
    var lang by mutableStateOf("en")
        private set
    var catalogue by mutableStateOf<Catalogue?>(null)
        private set
    var course by mutableStateOf<Course?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    // Assessment
    var attempt by mutableStateOf<Attempt?>(null)
        private set
    var result by mutableStateOf<SubmitResult?>(null)
        private set
    var quizBusy by mutableStateOf(false)
        private set
    var quizError by mutableStateOf<String?>(null)
        private set

    private var started = false
    private var pending: Triple<Int, Int?, String?>? = null // course, lesson, lang asked for before the start finished

    fun start(preferredLang: String) {
        if (started) return
        started = true
        viewModelScope.launch {
            try {
                val boot = repo.bootstrap()
                languages = boot.languages.filter { it.courses > 0 }.ifEmpty { boot.languages }
                val want = pending?.third ?: boot.lastLang ?: preferredLang
                lang = languages.firstOrNull { it.code == want }?.code ?: languages.firstOrNull()?.code ?: "en"
            } catch (e: ApiException) {
                error = e.message
            }
            loadCatalogue()
            pending?.let { (c, l, _) -> open(c, l) }
            pending = null
        }
    }

    /** From Home's "Continue": straight to a course (and lesson) in its language. */
    fun openFromHome(courseId: Int, lessonId: Int?, courseLang: String?) {
        if (!started || languages.isEmpty()) {
            pending = Triple(courseId, lessonId, courseLang)
            return
        }
        if (courseLang != null && courseLang != lang && languages.any { it.code == courseLang }) {
            lang = courseLang
            loadCatalogue()
        }
        open(courseId, lessonId)
    }

    private fun open(courseId: Int, lessonId: Int?) {
        stack.removeRange(1, stack.size)
        stack.add(LearnPage.CoursePage(courseId))
        loadCourse(courseId) {
            if (lessonId != null && it.lessons.any { l -> l.id == lessonId && l.media != null }) stack.add(LearnPage.LessonPage(courseId, lessonId))
        }
    }

    fun chooseLang(code: String) {
        if (code == lang) return
        lang = code
        loadCatalogue()
    }

    fun loadCatalogue() {
        viewModelScope.launch {
            repo.cachedCatalogue(lang)?.let { catalogue = it }
            loading = true
            try {
                catalogue = repo.catalogue(lang)
                error = null
            } catch (e: ApiException) {
                if (catalogue == null || catalogue?.lang != lang) error = e.message
            } finally {
                loading = false
            }
        }
    }

    fun openCourse(id: Int) {
        stack.add(LearnPage.CoursePage(id))
        loadCourse(id)
    }

    private fun loadCourse(id: Int, then: (Course) -> Unit = {}) {
        viewModelScope.launch {
            course = repo.cachedCourse(id, lang)?.course?.takeIf { it.id == id }
            loading = true
            try {
                course = repo.course(id, lang).course
                error = null
            } catch (e: ApiException) {
                if (course?.id != id) error = e.message
            } finally {
                loading = false
            }
            course?.takeIf { it.id == id }?.let(then)
        }
    }

    fun refreshCourse() {
        (page as? LearnPage.CoursePage)?.let { loadCourse(it.courseId) }
    }

    fun openLesson(courseId: Int, lessonId: Int) {
        stack.add(LearnPage.LessonPage(courseId, lessonId))
    }

    /** The player moves on to the next lesson without growing the back stack. */
    fun replaceLesson(courseId: Int, lessonId: Int) {
        stack[stack.lastIndex] = LearnPage.LessonPage(courseId, lessonId)
    }

    /** The server's answer to a progress report: keep the course page in step. */
    fun progressSaved(r: ProgressResult) {
        val c = course ?: return
        course = c.copy(sections = c.sections.map { s ->
            s.copy(lessons = s.lessons.map { l ->
                if (l.id != r.lessonId) l else l.copy(progress = LessonProgress(completed = r.completed || l.progress?.completed == true, percent = r.percent, position = r.position))
            })
        })
        if (r.newlyCompleted) {
            // The assessment may have unlocked: fetch the course again quietly.
            viewModelScope.launch { runCatching { repo.course(c.id, lang).course }.getOrNull()?.let { course = it } }
        }
    }

    /**
     * Sends watched seconds to the server (in the view-model's scope, so it also finishes when the player closes).
     * [onFail] gets the ranges back so they go with the next report.
     */
    fun sendProgress(
        lessonId: Int,
        position: Double,
        duration: Double,
        ranges: List<Pair<Int, Int>>,
        played: Double,
        onDone: (ProgressResult) -> Unit = {},
        onFail: (List<Pair<Int, Int>>, Double) -> Unit = { _, _ -> },
    ) {
        val code = lang
        viewModelScope.launch {
            try {
                val r = repo.progress(lessonId, code, position, duration, ranges, played)
                progressSaved(r)
                onDone(r)
            } catch (e: ApiException) {
                onFail(ranges, played)
            }
        }
    }

    fun openQuiz(courseId: Int) {
        attempt = null
        result = null
        quizError = null
        stack.add(LearnPage.QuizPage(courseId))
        quizBusy = true
        viewModelScope.launch {
            try {
                attempt = repo.startAttempt(courseId, lang)
            } catch (e: ApiException) {
                quizError = e.message
            } finally {
                quizBusy = false
            }
        }
    }

    fun submitQuiz(answers: Map<Int, Set<Int>>) {
        val a = attempt ?: return
        quizBusy = true
        quizError = null
        viewModelScope.launch {
            try {
                result = repo.submitAttempt(a.attemptId, answers)
                course?.let { c -> runCatching { repo.course(c.id, lang).course }.getOrNull()?.let { course = it } }
            } catch (e: ApiException) {
                quizError = e.message
            } finally {
                quizBusy = false
            }
        }
    }

    /** true when the Learn tab handled Back itself. */
    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        if (page is LearnPage.CoursePage) refreshCourse()
        return true
    }
}
