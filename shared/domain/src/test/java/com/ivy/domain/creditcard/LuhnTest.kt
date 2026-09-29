package com.ivy.domain.creditcard

import io.kotest.matchers.shouldBe
import org.junit.Test

class LuhnTest {
    @Test
    fun `accepts well known valid test numbers`() {
        listOf(
            "4111111111111111", // Visa
            "5555555555554444", // Mastercard
            "2221000000000009", // Mastercard 2-series
            "378282246310005", // Amex
            "30569309025904", // Diners
            "6011111111111117", // Discover
        ).forEach { number ->
            isValidLuhn(number) shouldBe true
        }
    }

    @Test
    fun `rejects a number with one digit changed`() {
        isValidLuhn("4111111111111112") shouldBe false
        isValidLuhn("5555555555554443") shouldBe false
    }

    @Test
    fun `rejects short empty or non digit input`() {
        isValidLuhn("") shouldBe false
        isValidLuhn("41111111111") shouldBe false
        isValidLuhn("4111 1111 1111 1111") shouldBe false
        isValidLuhn("4111a11111111111") shouldBe false
    }

    @Test
    fun `normalize keeps digits only`() {
        normalizeCardNumber("4111 1111-1111 1111") shouldBe "4111111111111111"
        normalizeCardNumber("") shouldBe ""
        normalizeCardNumber("abc") shouldBe ""
    }
}
