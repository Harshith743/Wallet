package com.ivy.ui.money

import io.kotest.matchers.shouldBe
import org.junit.Test
import java.util.Locale

class CurrencySymbolTest {
    @Test
    fun `uses the looked up symbol`() {
        currencySymbol("INR", Locale.US, lookup = { _, _ -> "₹" }) shouldBe "₹"
    }

    @Test
    fun `falls back to the code when the symbol is blank or equals the code`() {
        currencySymbol("BGN", Locale.US, lookup = { _, _ -> "" }) shouldBe "BGN"
        currencySymbol("BGN", Locale.US, lookup = { _, _ -> "bgn" }) shouldBe "BGN"
        currencySymbol("BGN", Locale.US, lookup = { _, _ -> null }) shouldBe "BGN"
    }
}
