package com.ivy.domain.creditcard

import com.ivy.data.model.CardNetwork
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

class BinDatasetTest {
    private val csv = """
        # Source: test
        bin,brand,type,category,issuer
        461786,VISA,CREDIT,Platinum,HDFC Bank
        367000,DINERS CLUB,CREDIT,,Yes Bank
        437551,VISA,CREDIT,Classic,ICICI Bank

        12345,VISA,CREDIT,,Too short
        abcdef,VISA,CREDIT,,Not digits
        524111
        # Local hints
        652850,RUPAY,CREDIT,,Slice
    """.trimIndent()

    private val records = parseBinCsv(csv.lineSequence())

    @Test
    fun `parses rows and skips comments, header, blanks and malformed bins`() {
        records.keys shouldBe setOf("461786", "367000", "437551", "524111", "652850")
        records.getValue("461786") shouldBe BinRecord(
            bin = "461786",
            brand = "VISA",
            cardType = "CREDIT",
            tier = "Platinum",
            issuer = "HDFC Bank",
        )
    }

    @Test
    fun `missing columns and empty fields are null`() {
        records.getValue("524111") shouldBe BinRecord("524111", null, null, null, null)
        records.getValue("367000").tier.shouldBeNull()
        records.getValue("367000").issuer shouldBe "Yes Bank"
    }

    @Test
    fun `generic categories are not a tier`() {
        records.getValue("437551").tier.shouldBeNull()
    }

    @Test
    fun `lookup uses the first six digits and tolerates spaces`() {
        records.lookupCard("4617 8612 3456 7890")?.issuer shouldBe "HDFC Bank"
        records.lookupCard("6528 50")?.issuer shouldBe "Slice"
        records.lookupCard("46178").shouldBeNull()
        records.lookupCard("9999999999999999").shouldBeNull()
        records.lookupCard("").shouldBeNull()
    }

    @Test
    fun `self issued networks default their issuer`() {
        defaultIssuerFor(CardNetwork.AMEX) shouldBe "American Express"
        defaultIssuerFor(CardNetwork.DINERS) shouldBe "Diners Club"
        defaultIssuerFor(CardNetwork.VISA).shouldBeNull()
        defaultIssuerFor(CardNetwork.UNKNOWN).shouldBeNull()
    }
}
