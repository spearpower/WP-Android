package com.workspace.disciplehub.data

import android.content.Context
import android.os.Build
import com.workspace.disciplehub.BuildConfig
import java.util.UUID

/** What the server is told about this phone (api/v1 app_devices): a random id made once, name and versions. */
class DeviceInfo(context: Context, private val tokens: TokenStore) {
    val name: String = listOf(Build.MANUFACTURER.replaceFirstChar { it.uppercase() }, Build.MODEL)
        .distinct().joinToString(" ").take(120)
    val appVersion: String = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    val osVersion: String = "Android ${Build.VERSION.RELEASE}"

    suspend fun id(): String = tokens.deviceId { UUID.randomUUID().toString().replace("-", "") }
}
