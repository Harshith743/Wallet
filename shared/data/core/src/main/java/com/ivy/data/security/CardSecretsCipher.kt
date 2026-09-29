package com.ivy.data.security

/**
 * Symmetric encryption for card secrets (PAN, CVV).
 *
 * Implementations must produce a self-contained cipher text (IV included) so that
 * [decrypt] needs nothing but the key. The production implementation keeps the key
 * in the Android Keystore; tests use a fake.
 */
interface CardSecretsCipher {
    fun encrypt(plain: String): String

    /**
     * @throws Exception when the cipher text is malformed, tampered with, or was
     * encrypted with a key that no longer exists.
     */
    fun decrypt(cipherText: String): String
}
