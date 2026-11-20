package com.workspace.disciplehub.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.store by preferencesDataStore(name = "wp_session")

/**
 * Keeps the sign-in tokens, the last known "me" and this phone's device id. The tokens are encrypted with an AES key
 * that lives in the Android Keystore (it never leaves the phone's secure hardware); the app's backups are off.
 */
class TokenStore(private val context: Context) {
    private val kTokens = stringPreferencesKey("tokens")
    private val kMe = stringPreferencesKey("me")
    private val kDevice = stringPreferencesKey("device_id")

    @Volatile private var cached: Tokens? = null
    @Volatile private var loaded = false

    suspend fun tokens(): Tokens? {
        if (loaded) return cached
        val sealed = context.store.data.first()[kTokens]
        cached = sealed?.let { runCatching { AppJson.decodeFromString(Tokens.serializer(), decrypt(it)) }.getOrNull() }
        loaded = true
        return cached
    }

    suspend fun save(tokens: Tokens) {
        context.store.edit { it[kTokens] = encrypt(AppJson.encodeToString(Tokens.serializer(), tokens)) }
        cached = tokens
        loaded = true
    }

    /** The access token already in memory (for image requests, which cannot wait); null before the first load. */
    fun accessNow(): String? = cached?.accessToken

    suspend fun lastMe(): Me? = context.store.data.first()[kMe]?.let { runCatching { AppJson.decodeFromString(Me.serializer(), it) }.getOrNull() }

    suspend fun saveMe(me: Me) {
        context.store.edit { it[kMe] = AppJson.encodeToString(Me.serializer(), me) }
    }

    /** Saved replies (JSON) so screens show at once and work offline. Cleared on sign-out. */
    suspend fun cacheGet(key: String): String? = context.store.data.first()[stringPreferencesKey("cache_$key")]

    suspend fun cachePut(key: String, json: String) {
        context.store.edit { it[stringPreferencesKey("cache_$key")] = json }
    }

    /** Signs this phone out: everything goes except the device id. */
    suspend fun clear() {
        context.store.edit { prefs ->
            val device = prefs[kDevice]
            prefs.clear()
            device?.let { prefs[kDevice] = it }
        }
        cached = null
        loaded = true
    }

    suspend fun deviceId(create: () -> String): String {
        context.store.data.first()[kDevice]?.let { return it }
        val id = create()
        context.store.edit { it[kDevice] = id }
        return id
    }

    // ---- Keystore AES-GCM -----------------------------------------------------------------------------------
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val out = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun decrypt(sealed: String): String {
        val bytes = Base64.decode(sealed, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, 12))
        return String(cipher.doFinal(bytes, 12, bytes.size - 12), Charsets.UTF_8)
    }

    private companion object {
        const val ALIAS = "wp_session_key"
        const val TRANSFORM = "AES/GCM/NoPadding"
    }
}
