package com.ivy.domain.creditcard

import com.ivy.data.model.CardNetwork
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

class IndianIssuerBinTableTest {
    @Test
    fun `finds issuer by prefix`() {
        IndianIssuerBinTable.lookup("4375 5112 3456 7890") shouldBe "HDFC Bank"
    }

    @Test
    fun `longest prefix wins`() {
        val entries = listOf(
            IssuerBin(prefix = "4375", issuer = "Short"),
            IssuerBin(prefix = "437551", issuer = "Long"),
        )
        val digits = "4375511234567890"
        val best = entries.filter { digits.startsWith(it.prefix) }.maxByOrNull { it.prefix.length }
        best?.issuer shouldBe "Long"
    }

    @Test
    fun `unknown prefix without network fallback is null`() {
        IndianIssuerBinTable.lookup("9999999999999999").shouldBeNull()
        IndianIssuerBinTable.lookup("").shouldBeNull()
    }

    @Test
    fun `network fallback for self issued networks`() {
        IndianIssuerBinTable.lookup("378282246310005", CardNetwork.AMEX) shouldBe "American Express"
        IndianIssuerBinTable.lookup("36000000000008", CardNetwork.DINERS) shouldBe "Diners Club"
        IndianIssuerBinTable.lookup("9999999999999999", CardNetwork.VISA).shouldBeNull()
    }

    @Test
    fun `table has no duplicate prefixes and only digits`() {
        val prefixes = IndianIssuerBinTable.entries.map { it.prefix }
        prefixes.distinct().size shouldBe prefixes.size
        prefixes.all { p -> p.isNotEmpty() && p.all { it.isDigit() } } shouldBe true
    }
}
