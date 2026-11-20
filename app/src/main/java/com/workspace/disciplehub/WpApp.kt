package com.workspace.disciplehub

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.workspace.disciplehub.data.ApiClient
import com.workspace.disciplehub.data.AuthRepository
import com.workspace.disciplehub.data.DeviceInfo
import com.workspace.disciplehub.data.HomeRepository
import com.workspace.disciplehub.data.JoinRepository
import com.workspace.disciplehub.data.LeadRepository
import com.workspace.disciplehub.data.LibraryRepository
import com.workspace.disciplehub.data.LiveRepository
import com.workspace.disciplehub.data.StudyRepository
import com.workspace.disciplehub.data.TokenStore

/** The app's single objects (no DI framework): the token store, the API client and the sign-in repository. */
class WpApp : Application(), SingletonImageLoader.Factory {
    lateinit var tokens: TokenStore
        private set
    lateinit var api: ApiClient
        private set
    lateinit var auth: AuthRepository
        private set
    lateinit var home: HomeRepository
        private set
    lateinit var study: StudyRepository
        private set
    lateinit var live: LiveRepository
        private set
    lateinit var library: LibraryRepository
        private set
    lateinit var join: JoinRepository
        private set
    lateinit var lead: LeadRepository
        private set

    /** Pictures (book covers, profile photos) load with the sign-in when they come from DH Workspace. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context).components { add(OkHttpNetworkFetcherFactory(callFactory = { api.authedHttp })) }.build()

    /** Work that outlives a screen (push registration). */
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)

    /** A screen a notification asked to open ("live", "learn", "library", "home"); MainScreen takes it. */
    val pendingRoute = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    override fun onCreate() {
        super.onCreate()
        com.workspace.disciplehub.push.Push.start(this)
        tokens = TokenStore(this)
        val device = DeviceInfo(this, tokens)
        api = ApiClient(BuildConfig.API_BASE, tokens, device)
        auth = AuthRepository(api, tokens, device)
        home = HomeRepository(api, tokens)
        study = StudyRepository(api, tokens)
        live = LiveRepository(api, tokens)
        library = LibraryRepository(api, tokens, filesDir)
        join = JoinRepository(api)
        lead = LeadRepository(api)
    }
}
