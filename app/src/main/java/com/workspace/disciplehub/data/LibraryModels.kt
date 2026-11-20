package com.workspace.disciplehub.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

/** api/v1/library.php */
@Serializable
data class LibraryHome(val themes: List<Shelf> = emptyList(), val books: List<Book> = emptyList())

@Serializable
data class Shelf(val id: Int, val name: String = "", val description: String = "", val color: String = "", val books: Int = 0)

@Serializable
data class Book(
    val id: Int,
    val theme: Int = 0,
    val title: String = "",
    val author: String = "",
    val year: Int? = null,
    val language: String = "",
    val description: String = "",
    val pages: Int = 0,
    val size: Long = 0,
    val cover: String = "",
    val download: Boolean = false,
    val added: String = "",
    val progress: BookProgress? = null,
)

@Serializable
data class BookProgress(val page: Int = 1, val percent: Int = 0, val finished: Boolean = false, val last: String = "")

@Serializable
data class BookReply(val book: Book, val bookmarks: List<Bookmark> = emptyList())

@Serializable
data class Bookmark(val id: Int, val page: Int, val note: String = "")

@Serializable
data class BookmarksReply(val bookmarks: List<Bookmark> = emptyList())

@Serializable
data class ReadResult(val page: Int = 1, val percent: Int = 0, val finished: Boolean = false, @SerialName("just_finished") val justFinished: Boolean = false)

class LibraryRepository(private val api: ApiClient, private val tokens: TokenStore, private val filesDir: File) {
    suspend fun cached(): LibraryHome? = tokens.cacheGet("library")?.let { runCatching { AppJson.decodeFromString(LibraryHome.serializer(), it) }.getOrNull() }

    suspend fun home(): LibraryHome = api.get("library.php?action=home", LibraryHome.serializer()).also {
        tokens.cachePut("library", AppJson.encodeToString(LibraryHome.serializer(), it))
    }

    suspend fun book(id: Int): BookReply = api.get("library.php?action=book&id=$id", BookReply.serializer())

    suspend fun progress(id: Int, page: Int, pages: Int, seen: Collection<Int>): ReadResult = api.post(
        "library.php",
        buildJsonObject {
            put("action", "progress"); put("id", id); put("page", page); put("pages", pages)
            put("seen", JsonArray(seen.take(400).map { JsonPrimitive(it) }))
        },
        ReadResult.serializer(),
    )

    suspend fun addBookmark(id: Int, page: Int, note: String): List<Bookmark> = api.post(
        "library.php", buildJsonObject { put("action", "bookmark_add"); put("id", id); put("page", page); put("note", note) }, BookmarksReply.serializer(),
    ).bookmarks

    suspend fun deleteBookmark(id: Int, bookmark: Int): List<Bookmark> = api.post(
        "library.php", buildJsonObject { put("action", "bookmark_delete"); put("id", id); put("bookmark", bookmark) }, BookmarksReply.serializer(),
    ).bookmarks

    /** The book's PDF on this phone (downloaded once, then read offline). [size] tells when the book was replaced. */
    suspend fun pdf(book: Book): File {
        val file = File(filesDir, "books/${book.id}-${book.size}.pdf")
        if (file.exists() && file.length() > 0) return file
        withContext(Dispatchers.IO) { File(filesDir, "books").listFiles { f -> f.name.startsWith("${book.id}-") }?.forEach { it.delete() } }
        api.download("files.php?kind=book&b=${book.id}", file)
        return file
    }

    /** A certificate PDF (made fresh each time, kept in the cache). */
    suspend fun certificate(code: String, cacheDir: File): File {
        val file = File(cacheDir, "certificates/${code.replace(Regex("[^A-Za-z0-9-]"), "")}.pdf")
        api.download("files.php?kind=certificate&code=$code", file)
        return file
    }

    suspend fun clearDownloads() = withContext(Dispatchers.IO) { File(filesDir, "books").deleteRecursively() }
}
