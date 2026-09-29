package com.ivy.data.security

import com.ivy.data.security.fake.FakeCardSecretsCipher
import com.ivy.data.security.fake.FakeCardSecretsStore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.Test
import java.util.UUID

class FakeCardSecretsCipherTest {
    private val cipher = FakeCardSecretsCipher()

    @Test
    fun `round trip`() {
        val encrypted = cipher.encrypt("4111111111111111")

        encrypted shouldNotContain "4111111111111111"
        cipher.decrypt(encrypted) shouldBe "4111111111111111"
    }

    @Test
    fun `decrypt of garbage throws`() {
        shouldThrow<IllegalArgumentException> { cipher.decrypt("not a cipher text") }
    }

    @Test
    fun `simulated failure throws`() {
        val encrypted = cipher.encrypt("123")
        cipher.failDecrypt = true

        shouldThrow<IllegalStateException> { cipher.decrypt(encrypted) }
    }

    @Test
    fun `fake store keeps secrets per card`() {
        val store = FakeCardSecretsStore()
        val cardId = UUID.randomUUID()

        store.read(cardId) shouldBe null
        store.write(cardId, StoredCardSecrets(panCipher = "p", cvvCipher = null))
        store.read(cardId) shouldBe StoredCardSecrets(panCipher = "p", cvvCipher = null)

        store.remove(cardId)
        store.read(cardId) shouldBe null

        store.write(cardId, StoredCardSecrets(panCipher = "p", cvvCipher = "c"))
        store.clear()
        store.read(cardId) shouldBe null
    }
}
