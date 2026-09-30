package com.ivy.creditcards.skin

import androidx.compose.ui.graphics.Color
import com.ivy.data.model.AccountId
import com.ivy.data.model.CardNetwork
import com.ivy.data.model.CardSkinMode
import com.ivy.data.skin.fake.FakeCardSkinImageStore
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.util.UUID

class CardSkinResolverTest {
    private val store = FakeCardSkinImageStore()
    private val resolver = CardSkinResolver(store)
    private val cardId = AccountId(UUID.randomUUID())
    private val color = Color(0xFF223344)

    private fun resolve(mode: CardSkinMode) = resolver.resolve(
        mode = mode,
        cardId = cardId,
        issuer = "HDFC Bank",
        cardName = "Pixel",
        network = CardNetwork.VISA,
        tier = null,
        color = color,
    )

    @Test
    fun `auto uses the catalog and colour the plain swatch`() {
        resolve(CardSkinMode.AUTO).wordmark shouldBe "HDFC BANK"
        resolve(CardSkinMode.COLOR) shouldBe CardSkinCatalog.solid(color)
    }

    @Test
    fun `image uses the stored photo with white text, or the catalog when the file is missing`() {
        resolve(CardSkinMode.IMAGE).imagePath.shouldBeNull()
        resolve(CardSkinMode.IMAGE).wordmark shouldBe "HDFC BANK"

        store.committed[cardId.value] = "/photos/card.jpg"

        val skin = resolve(CardSkinMode.IMAGE)
        skin.imagePath shouldBe "/photos/card.jpg"
        skin.textColor shouldBe Color.White
        skin.wordmark.shouldBeNull()
    }
}
