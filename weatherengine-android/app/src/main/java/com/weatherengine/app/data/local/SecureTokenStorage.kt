package com.weatherengine.app.data.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SecureTokenStorage {
    fun getToken(): String?
    fun saveToken(token: String)
    fun clearToken()
}

/**
 * Hardware/Keystore-backed secure token storage using AES-256 GCM.
 * Tokens are never persisted in plaintext.
 * Any Keystore corruption or decryption failure safely clears the token
 * to force re-authentication without crashing the app.
 */
class KeystoreTokenStorage(
    context: Context,
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
) : SecureTokenStorage {

    companion object {
        private const val PREFS_NAME = "weatherengine_secure_prefs"
        private const val KEY_ALIAS = "weatherengine_jwt_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val PREF_KEY_TOKEN_CIPHERTEXT = "encrypted_token_b64"
        private const val PREF_KEY_TOKEN_IV = "encrypted_token_iv_b64"
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    override fun getToken(): String? {
        val cipherB64 = prefs.getString(PREF_KEY_TOKEN_CIPHERTEXT, null) ?: return null
        val ivB64 = prefs.getString(PREF_KEY_TOKEN_IV, null) ?: return null

        return try {
            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            val iv = Base64.decode(ivB64, Base64.DEFAULT)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val ciphertext = Base64.decode(cipherB64, Base64.DEFAULT)
            val plaintextBytes = cipher.doFinal(ciphertext)
            String(plaintextBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            clearToken()
            null
        }
    }

    override fun saveToken(token: String) {
        try {
            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(token.toByteArray(Charsets.UTF_8))

            prefs.edit()
                .putString(PREF_KEY_TOKEN_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .putString(PREF_KEY_TOKEN_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
        } catch (_: Exception) {
            clearToken()
        }
    }

    override fun clearToken() {
        prefs.edit()
            .remove(PREF_KEY_TOKEN_CIPHERTEXT)
            .remove(PREF_KEY_TOKEN_IV)
            .apply()
    }
}
