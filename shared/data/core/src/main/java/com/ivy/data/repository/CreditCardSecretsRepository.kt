package com.ivy.data.repository

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.model.AccountId
import com.ivy.data.model.CardSecrets
import com.ivy.data.model.primitive.CardCvv
import com.ivy.data.model.primitive.CardPan
import com.ivy.data.security.CardSecretsCipher
import com.ivy.data.security.CardSecretsStore
import com.ivy.data.security.StoredCardSecrets
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface CardSecretsError {
    /** No secrets stored for this card (never entered, or restored from a backup). */
    data object Missing : CardSecretsError

    data class DecryptFailed(val reason: String) : CardSecretsError

    data class EncryptFailed(val reason: String) : CardSecretsError
}

/**
 * Encrypted card secrets, decrypted on demand. Never cached in memory.
 */
class CreditCardSecretsRepository @Inject constructor(
    private val store: CardSecretsStore,
    private val cipher: CardSecretsCipher,
    private val dispatchersProvider: DispatchersProvider,
) {
    suspend fun get(cardId: AccountId): Either<CardSecretsError, CardSecrets> =
        withContext(dispatchersProvider.io) {
            either {
                val stored = ensureNotNull(store.read(cardId.value)) { CardSecretsError.Missing }
                val pan = stored.panCipher?.let { decrypt(it).bind() }
                val cvv = stored.cvvCipher?.let { decrypt(it).bind() }
                CardSecrets(
                    pan = pan?.let(CardPan::from)?.getOrNull(),
                    cvv = cvv?.let(CardCvv::from)?.getOrNull(),
                )
            }
        }

    suspend fun save(cardId: AccountId, secrets: CardSecrets): Either<CardSecretsError, Unit> =
        withContext(dispatchersProvider.io) {
            either {
                val panCipher = secrets.pan?.let { encrypt(it.value).bind() }
                val cvvCipher = secrets.cvv?.let { encrypt(it.value).bind() }
                store.write(cardId.value, StoredCardSecrets(panCipher = panCipher, cvvCipher = cvvCipher))
            }
        }

    suspend fun clear(cardId: AccountId): Unit = withContext(dispatchersProvider.io) {
        store.remove(cardId.value)
    }

    suspend fun clearAll(): Unit = withContext(dispatchersProvider.io) {
        store.clear()
    }

    private fun decrypt(cipherText: String): Either<CardSecretsError, String> =
        Either.catch { cipher.decrypt(cipherText) }
            .mapLeft { CardSecretsError.DecryptFailed(it.message.orEmpty()) }

    private fun encrypt(plain: String): Either<CardSecretsError, String> =
        Either.catch { cipher.encrypt(plain) }
            .mapLeft { CardSecretsError.EncryptFailed(it.message.orEmpty()) }
}
