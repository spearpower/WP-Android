package com.workspace.disciplehub

import android.app.Application
import com.workspace.disciplehub.data.ApiClient
import com.workspace.disciplehub.data.AuthRepository
import com.workspace.disciplehub.data.DeviceInfo
import com.workspace.disciplehub.data.HomeRepository
import com.workspace.disciplehub.data.TokenStore

/** The app's single objects (no DI framework): the token store, the API client and the sign-in repository. */
class WpApp : Application() {
    lateinit var tokens: TokenStore
        private set
    lateinit var api: ApiClient
        private set
    lateinit var auth: AuthRepository
        private set
    lateinit var home: HomeRepository
        private set

    override fun onCreate() {
        super.onCreate()
        tokens = TokenStore(this)
        val device = DeviceInfo(this, tokens)
        api = ApiClient(BuildConfig.API_BASE, tokens, device)
        auth = AuthRepository(api, tokens, device)
        home = HomeRepository(api, tokens)
    }
}
