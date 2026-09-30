package com.ivy.creditcards.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.creditcards.model.CreditCardUi
import com.ivy.data.model.AccountId
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.IvyOutlinedButton
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

object CardStackDefaults {
    val PeekHeight: Dp = 72.dp
    val Gap: Dp = 12.dp
    const val InactiveAlpha = 0.92f
}

data class CardStackCallbacks(
    val onSelectCard: (AccountId) -> Unit,
    val onToggleExpanded: () -> Unit,
    val onExpand: () -> Unit,
    val onPayNow: (CreditCardUi) -> Unit,
    val onViewDetails: (AccountId) -> Unit,
    val onReveal: (AccountId) -> Unit,
    val onCloseReveal: (AccountId) -> Unit,
)

/**
 * The active card on top (tap toggles, swipe up expands, swipe left reveals the quick
 * actions) with its "View details" pill, followed by the other cards: stacked with only
 * their top band visible when collapsed, or as a full list of revealable cards when
 * [expanded]. Tapping another card makes it active.
 */
@Composable
fun CardStack(
    cards: ImmutableList<CreditCardUi>,
    activeCardId: AccountId?,
    expanded: Boolean,
    revealedCardId: AccountId?,
    callbacks: CardStackCallbacks,
    quickActions: QuickActionCallbacks,
    modifier: Modifier = Modifier,
) {
    val active = cards.firstOrNull { it.id == activeCardId } ?: cards.firstOrNull() ?: return
    val others = cards.filter { it.id != active.id }.toImmutableList()
    Column(modifier = modifier.animateContentSize()) {
        key(active.id) {
            RevealableCard(
                card = active,
                revealed = revealedCardId == active.id,
                callbacks = RevealCallbacks(
                    onClick = callbacks.onToggleExpanded,
                    onReveal = { callbacks.onReveal(active.id) },
                    onCloseReveal = { callbacks.onCloseReveal(active.id) },
                    onPayNow = { callbacks.onPayNow(active) },
                    onSwipeUp = callbacks.onExpand,
                ),
                showPayNow = true,
                swipeUpEnabled = !expanded,
            ) {
                QuickActionGrid(cardId = active.id, callbacks = quickActions)
            }
        }
        ViewDetailsPill(onClick = { callbacks.onViewDetails(active.id) })
        if (others.isEmpty()) return@Column
        Spacer(Modifier.height(CardStackDefaults.Gap))
        if (expanded) {
            ExpandedList(
                cards = others,
                revealedCardId = revealedCardId,
                callbacks = callbacks,
                quickActions = quickActions,
            )
        } else {
            StackedPeek(cards = others, onSelect = callbacks.onSelectCard)
        }
    }
}

@Composable
private fun ExpandedList(
    cards: ImmutableList<CreditCardUi>,
    revealedCardId: AccountId?,
    callbacks: CardStackCallbacks,
    quickActions: QuickActionCallbacks,
) {
    Column {
        cards.forEach { card ->
            key(card.id) {
                RevealableCard(
                    card = card,
                    revealed = revealedCardId == card.id,
                    callbacks = RevealCallbacks(
                        onClick = { callbacks.onSelectCard(card.id) },
                        onReveal = { callbacks.onReveal(card.id) },
                        onCloseReveal = { callbacks.onCloseReveal(card.id) },
                        onPayNow = { callbacks.onPayNow(card) },
                        onSwipeUp = {},
                    ),
                ) {
                    QuickActionGrid(cardId = card.id, callbacks = quickActions)
                }
            }
            ViewDetailsPill(onClick = { callbacks.onViewDetails(card.id) })
            Spacer(Modifier.height(CardStackDefaults.Gap))
        }
    }
}

/**
 * Every inactive card is cropped to its top band (issuer, due amount, status); the last
 * one gets two bands so the stack reads as cards tucked behind each other. Bands are
 * tap-only: a 72 dp crop cannot host the quick-action grid.
 */
@Composable
private fun StackedPeek(
    cards: ImmutableList<CreditCardUi>,
    onSelect: (AccountId) -> Unit,
) {
    Column {
        cards.forEachIndexed { index, card ->
            val bands = if (index == cards.lastIndex) 2 else 1
            PeekBand(
                card = card,
                height = CardStackDefaults.PeekHeight * bands,
                onClick = { onSelect(card.id) },
            )
        }
    }
}

@Composable
private fun PeekBand(
    card: CreditCardUi,
    height: Dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
            .alpha(CardStackDefaults.InactiveAlpha)
            .clickable(onClick = onClick),
    ) {
        // Unbounded height so the face keeps its real size and only the top shows
        CreditCardFace(
            card = card,
            modifier = Modifier.wrapContentHeight(align = Alignment.Top, unbounded = true),
        )
    }
}

@Composable
private fun ViewDetailsPill(
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        IvyOutlinedButton(
            text = stringResource(R.string.view_details),
            iconStart = null,
            solidBackground = true,
            onClick = onClick,
        )
    }
}
