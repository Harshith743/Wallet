package com.ivy.data.security

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted card secrets as stored: already-encrypted strings, or null when absent.
 */
data class StoredCardSecrets(
    val panCipher: String?,
    val cvvCipher: String?,
)

/**
 * Where encrypted card secrets live. Deliberately not the Room database, so that
 * backups (which serialise Room entities) can never contain them.
 */
interface CardSecretsStore {
    fun read(cardId: UUID): StoredCardSecrets?
    fun write(cardId: UUID, secrets: StoredCardSecrets)
    fun remove(cardId: UUID)
    fun clear()
}

@Singleton
class SharedPrefsCardSecretsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : CardSecretsStore {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun read(cardId: UUID): StoredCardSecrets? {
        val pan = prefs.getString(panKey(cardId), null)
        val cvv = prefs.getString(cvvKey(cardId), null)
        if (pan == null && cvv == null) return null
        return StoredCardSecrets(panCipher = pan, cvvCipher = cvv)
    }

    override fun write(cardId: UUID, secrets: StoredCardSecrets) {
        prefs.edit()
            .putOrRemove(panKey(cardId), secrets.panCipher)
            .putOrRemove(cvvKey(cardId), secrets.cvvCipher)
            .apply()
    }

    override fun remove(cardId: UUID) {
        prefs.edit()
            .remove(panKey(cardId))
            .remove(cvvKey(cardId))
            .apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private fun SharedPreferences.Editor.putOrRemove(key: String, value: String?): SharedPreferences.Editor =
        if (value == null) remove(key) else putString(key, value)

    private fun panKey(cardId: UUID): String = "pan_$cardId"

    private fun cvvKey(cardId: UUID): String = "cvv_$cardId"

    companion object {
        private const val PREFS_NAME = "card_secrets"
    }
}
