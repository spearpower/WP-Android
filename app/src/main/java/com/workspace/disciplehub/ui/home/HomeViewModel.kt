package com.workspace.disciplehub.ui.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.workspace.disciplehub.WpApp
import com.workspace.disciplehub.data.ApiException
import com.workspace.disciplehub.data.Home
import kotlinx.coroutines.launch

/** Home dashboard: shows the saved dashboard at once, then the fresh one from the server. */
class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as WpApp).home

    var home by mutableStateOf<Home?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            home = repo.cached()
            refresh()
        }
    }

    fun refresh() {
        if (loading) return
        loading = true
        viewModelScope.launch {
            try {
                home = repo.load()
                error = null
            } catch (e: ApiException) {
                error = if (home == null) e.message else null // with a saved dashboard, the offline banner says enough
            } finally {
                loading = false
            }
        }
    }
}
