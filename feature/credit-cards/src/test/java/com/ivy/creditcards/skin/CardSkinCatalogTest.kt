package com.ivy.creditcards.skin

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.ivy.data.model.CardNetwork
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test

private const val DarkTextLuminance = 0.5f

class CardSkinCatalogTest {
    private val fallback = Color(0xFF123456)

    private fun resolve(
        issuer: String? = null,
        name: String = "",
        network: CardNetwork = CardNetwork.UNKNOWN,
        tier: String? = null,
    ) = CardSkinCatalog.resolve(issuer = issuer, cardName = name, network = network, tier = tier, fallback = fallback)

    @Test
    fun `issuer keys match whole tokens of the issuer or the card name`() {
        resolve(issuer = "HDFC Bank").wordmark shouldBe "HDFC BANK"
        resolve(name = "My hdfc card").wordmark shouldBe "HDFC BANK"
        resolve(issuer = "Standard Chartered Bank").wordmark shouldBe "STANDARD CHARTERED"
        resolve(issuer = "American Express").wordmark shouldBe "AMERICAN EXPRESS"
        resolve(issuer = "SBI Card").wordmark shouldBe "SBI CARD"
        resolve(issuer = "State Bank of India").wordmark shouldBe "SBI CARD"
        resolve(issuer = "OneCard").wordmark shouldBe "OneCard"
    }

    @Test
    fun `token boundaries prevent false matches`() {
        resolve(issuer = "Mauritius Commercial Bank").wordmark.shouldBeNull()
        resolve(issuer = "Citizens Bank").wordmark.shouldBeNull()
        resolve(issuer = "Yesterday Bank").wordmark.shouldBeNull()
        resolve(issuer = "AU Small Finance Bank").wordmark shouldBe "AU BANK"
        resolve(issuer = "Citibank").wordmark shouldBe "citi"
    }

    @Test
    fun `premium tiers turn the face graphite and keep the issuer accent`() {
        val regular = resolve(issuer = "HDFC Bank")
        val infinia = resolve(issuer = "HDFC Bank", name = "HDFC Infinia")
        val byTier = resolve(issuer = "HDFC Bank", tier = "World Elite")
        infinia.colors shouldNotBe regular.colors
        infinia.colors shouldBe byTier.colors
        infinia.accent shouldBe regular.accent
        infinia.wordmark shouldBe "HDFC BANK"
        infinia.textColor shouldBe Color.White
    }

    @Test
    fun `unknown issuer falls back to the network`() {
        resolve(issuer = "Some Bank", network = CardNetwork.VISA).colors.size shouldBe 2
        resolve(issuer = "Some Bank", network = CardNetwork.MASTERCARD).accent shouldNotBe null
        resolve(issuer = "Some Bank", network = CardNetwork.AMEX).wordmark shouldBe "AMERICAN EXPRESS"
        resolve(issuer = null, network = CardNetwork.RUPAY).colors.size shouldBe 2
    }

    @Test
    fun `unknown issuer and network is the solid account colour`() {
        val skin = resolve(issuer = "Some Bank", network = CardNetwork.UNKNOWN)
        skin shouldBe CardSkinCatalog.solid(fallback)
        skin.colors.size shouldBe 1
        skin.colors[0] shouldBe fallback
        skin.accent.shouldBeNull()
        skin.wordmark.shouldBeNull()
    }

    @Test
    fun `solid picks a readable text colour`() {
        (CardSkinCatalog.solid(Color.White).textColor.luminance() < DarkTextLuminance) shouldBe true
        (CardSkinCatalog.solid(Color.Black).textColor.luminance() > DarkTextLuminance) shouldBe true
    }
}
