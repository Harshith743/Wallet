package com.ivy.data.repository

import com.ivy.base.TestDispatchersProvider
import com.ivy.data.model.AccountId
import com.ivy.data.model.CardSecrets
import com.ivy.data.model.primitive.CardCvv
import com.ivy.data.model.primitive.CardPan
import com.ivy.data.security.StoredCardSecrets
import com.ivy.data.security.fake.FakeCardSecretsCipher
import com.ivy.data.security.fake.FakeCardSecretsStore
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

class CreditCardSecretsRepositoryTest {
    private val store = FakeCardSecretsStore()
    private val cipher = FakeCardSecretsCipher()
    private val repository = CreditCardSecretsRepository(
        store = store,
        cipher = cipher,
        dispatchersProvider = TestDispatchersProvider,
    )
    private val cardId = AccountId(UUID.randomUUID())

    @Test
    fun `missing when nothing stored`() = runTest {
        repository.get(cardId).shouldBeLeft() shouldBe CardSecretsError.Missing
    }

    @Test
    fun `save then get round trips and stores only cipher text`() = runTest {
        val secrets = CardSecrets(
            pan = CardPan.unsafe("4111111111111111"),
            cvv = CardCvv.unsafe("123"),
        )

        repository.save(cardId, secrets).shouldBeRight()

        val stored = store.read(cardId.value)
        stored?.panCipher.orEmpty() shouldNotContain "4111111111111111"
        stored?.cvvCipher.orEmpty() shouldNotContain "123"
        repository.get(cardId).shouldBeRight() shouldBe secrets
    }

    @Test
    fun `pan only`() = runTest {
        repository.save(cardId, CardSecrets(pan = CardPan.unsafe("5555555555554444"), cvv = null))

        repository.get(cardId).shouldBeRight() shouldBe CardSecrets(
            pan = CardPan.unsafe("5555555555554444"),
            cvv = null,
        )
    }

    @Test
    fun `decrypt failure is reported`() = runTest {
        store.write(cardId.value, StoredCardSecrets(panCipher = "garbage", cvvCipher = null))

        repository.get(cardId).shouldBeLeft().shouldBeInstanceOf<CardSecretsError.DecryptFailed>()
    }

    @Test
    fun `clear removes only that card, clearAll removes everything`() = runTest {
        val other = AccountId(UUID.randomUUID())
        repository.save(cardId, CardSecrets(pan = CardPan.unsafe("4111111111111111"), cvv = null))
        repository.save(other, CardSecrets(pan = CardPan.unsafe("5555555555554444"), cvv = null))

        repository.clear(cardId)
        repository.get(cardId).shouldBeLeft() shouldBe CardSecretsError.Missing
        repository.get(other).shouldBeRight()

        repository.clearAll()
        repository.get(other).shouldBeLeft() shouldBe CardSecretsError.Missing
    }
}
