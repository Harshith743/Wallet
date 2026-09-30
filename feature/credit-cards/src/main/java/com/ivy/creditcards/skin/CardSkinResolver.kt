package com.ivy.creditcards.skin

import androidx.compose.ui.graphics.Color
import com.ivy.data.model.AccountId
import com.ivy.data.model.CardNetwork
import com.ivy.data.model.CardSkinMode
import com.ivy.data.skin.CardSkinImageStore
import javax.inject.Inject

/**
 * Turns a card's stored skin mode into what the face paints. AUTO asks the catalog,
 * COLOR is the plain account colour, IMAGE is the owner's photo when it exists on this
 * device (otherwise the catalog, e.g. after a restore on another phone).
 */
class CardSkinResolver @Inject constructor(
    private val imageStore: CardSkinImageStore,
) {
    @Suppress("LongParameterList")
    fun resolve(
        mode: CardSkinMode,
        cardId: AccountId,
        issuer: String?,
        cardName: String,
        network: CardNetwork,
        tier: String?,
        color: Color,
    ): CardSkinUi {
        val auto = { CardSkinCatalog.resolve(issuer, cardName, network, tier, color) }
        return when (mode) {
            CardSkinMode.COLOR -> CardSkinCatalog.solid(color)
            CardSkinMode.AUTO -> auto()
            CardSkinMode.IMAGE -> imageStore.imagePath(cardId.value)?.let { path ->
                CardSkinUi(
                    colors = auto().colors,
                    textColor = Color.White,
                    accent = null,
                    wordmark = null,
                    imagePath = path,
                )
            } ?: auto()
        }
    }
}
