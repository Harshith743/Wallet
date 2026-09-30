package com.ivy.creditcards.skin

import androidx.compose.ui.graphics.Color
import com.ivy.data.model.CardNetwork
import com.ivy.data.model.CardSkinMode
import javax.inject.Inject

/**
 * Turns a card's stored skin mode into what the face paints. AUTO asks the catalog,
 * COLOR is the plain account colour. IMAGE falls back to the catalog until a photo is
 * supported.
 */
class CardSkinResolver @Inject constructor() {
    @Suppress("LongParameterList")
    fun resolve(
        mode: CardSkinMode,
        issuer: String?,
        cardName: String,
        network: CardNetwork,
        tier: String?,
        color: Color,
    ): CardSkinUi = when (mode) {
        CardSkinMode.COLOR -> CardSkinCatalog.solid(color)
        CardSkinMode.AUTO, CardSkinMode.IMAGE -> CardSkinCatalog.resolve(
            issuer = issuer,
            cardName = cardName,
            network = network,
            tier = tier,
            fallback = color,
        )
    }
}
