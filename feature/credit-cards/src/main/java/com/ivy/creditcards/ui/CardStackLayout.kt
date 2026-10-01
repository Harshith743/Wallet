package com.ivy.creditcards.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times

/**
 * Where one card sits in the stack. Every value is a target the UI moves towards:
 * [y] is the card's top, [scale] shrinks it about its top centre (cards tucked behind the
 * front one), [dim] darkens it, [zIndex] orders drawing and touch, and [pillAlpha] is the
 * visibility of the "View details" pill under it.
 */
@Immutable
data class CardSlot(
    val y: Dp,
    val scale: Float,
    val dim: Float,
    val zIndex: Float,
    val pillAlpha: Float,
)

/**
 * The geometry of the card stack, shared by its collapsed and expanded arrangements so a
 * card can travel between any two positions.
 *
 * Collapsed: the active card on top with its pill, then the other cards tucked behind each
 * other, each [CardStackDefaults.PeekHeight] lower than the previous, the last one in front.
 * Expanded: every card full size, one under the other, each followed by its pill.
 */
object CardStackLayout {
    /** The height of a pill row: the pill with [CardStackDefaults.Gap] above and below. */
    val PillRow: Dp = CardStackDefaults.Gap * 2 + CardStackDefaults.PillHeight

    /**
     * One slot per card, in the cards' list order. [activeIndex] outside the list is treated
     * as the first card.
     */
    fun slots(
        count: Int,
        activeIndex: Int,
        expanded: Boolean,
        cardWidth: Dp,
        cardHeight: Dp,
    ): List<CardSlot> {
        val active = if (activeIndex in 0 until count) activeIndex else 0
        val others = count - 1
        return List(count) { index ->
            // Position among the non-active cards, in list order
            val position = if (index < active) index else index - 1
            when {
                index == active -> CardSlot(y = 0.dp, scale = 1f, dim = 0f, zIndex = count.toFloat(), pillAlpha = 1f)
                expanded -> CardSlot(
                    y = (position + 1) * (cardHeight + PillRow),
                    scale = 1f,
                    dim = 0f,
                    zIndex = position.toFloat(),
                    pillAlpha = 1f,
                )

                else -> stackedSlot(
                    position = position,
                    others = others,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                )
            }
        }
    }

    private fun stackedSlot(position: Int, others: Int, cardWidth: Dp, cardHeight: Dp): CardSlot {
        val depth = others - 1 - position
        return CardSlot(
            y = cardHeight + PillRow + position * CardStackDefaults.PeekHeight,
            scale = (cardWidth - CardStackDefaults.StackInset * 2 * depth) / cardWidth,
            dim = if (depth > 0) CardStackDefaults.BehindScrimAlpha else 0f,
            zIndex = position.toFloat(),
            pillAlpha = 0f,
        )
    }

    /** The total height of the arrangement. */
    fun height(count: Int, expanded: Boolean, cardHeight: Dp): Dp {
        val others = count - 1
        return when {
            count <= 0 -> 0.dp
            expanded -> count * (cardHeight + PillRow)
            others == 0 -> cardHeight + PillRow
            else -> cardHeight + PillRow + (others - 1) * CardStackDefaults.PeekHeight + cardHeight
        }
    }
}
