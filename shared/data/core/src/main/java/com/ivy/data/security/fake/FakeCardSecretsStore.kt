package com.ivy.data.security.fake

import com.ivy.data.security.CardSecretsStore
import com.ivy.data.security.StoredCardSecrets
import org.jetbrains.annotations.VisibleForTesting
import java.util.UUID

@VisibleForTesting
class FakeCardSecretsStore : CardSecretsStore {
    private val secrets = mutableMapOf<UUID, StoredCardSecrets>()

    override fun read(cardId: UUID): StoredCardSecrets? = secrets[cardId]

    override fun write(cardId: UUID, secrets: StoredCardSecrets) {
        this.secrets[cardId] = secrets
    }

    override fun remove(cardId: UUID) {
        secrets.remove(cardId)
    }

    override fun clear() {
        secrets.clear()
    }
}
