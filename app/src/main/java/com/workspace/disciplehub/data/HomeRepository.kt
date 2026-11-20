package com.workspace.disciplehub.data

/** The Home dashboard: the server's reply, saved so the next start shows it at once (and offline). */
class HomeRepository(private val api: ApiClient, private val tokens: TokenStore) {
    suspend fun cached(): Home? = tokens.cacheGet(KEY)?.let { runCatching { AppJson.decodeFromString(Home.serializer(), it) }.getOrNull() }

    suspend fun load(): Home {
        val home = api.get("home.php", Home.serializer())
        tokens.cachePut(KEY, AppJson.encodeToString(Home.serializer(), home))
        return home
    }

    private companion object {
        const val KEY = "home"
    }
}
