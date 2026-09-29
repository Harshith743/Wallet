package com.ivy.data.security.fake

import com.ivy.data.security.CardSecretsCipher
import org.jetbrains.annotations.VisibleForTesting
import java.util.Base64

/**
 * Reversible, obviously-not-secure stand-in for tests: "fake:" + Base64(plain).
 * [decrypt] rejects anything without the prefix so mismatches are caught.
 */
@VisibleForTesting
class FakeCardSecretsCipher : CardSecretsCipher {
    var failDecrypt: Boolean = false

    override fun encrypt(plain: String): String =
        PREFIX + Base64.getEncoder().encodeToString(plain.toByteArray(Charsets.UTF_8))

    override fun decrypt(cipherText: String): String {
        check(!failDecrypt) { "Simulated decrypt failure" }
        require(cipherText.startsWith(PREFIX)) { "Not a fake cipher text" }
        return String(Base64.getDecoder().decode(cipherText.removePrefix(PREFIX)), Charsets.UTF_8)
    }

    companion object {
        private const val PREFIX = "fake:"
    }
}
