package com.ivy.creditcards.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.creditcards.model.CreditCardUi
import com.ivy.data.model.AccountId
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.IvyIcon
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

object CardStackDefaults {
    /** How much of each card behind the front one stays visible. */
    val PeekHeight: Dp = 72.dp

    /** Air between a card and the "View details" pill on either side of it. */
    val Gap: Dp = 12.dp

    /** Cards behind the front one are inset by this much per level, so they read as tucked in. */
    val StackInset: Dp = 6.dp

    /** Dim applied to the cards behind the front one. */
    const val BehindScrimAlpha = 0.25f
}

private val PillBorder: Dp = 1.dp
private val PillHorizontalPadding: Dp = 14.dp
private val PillVerticalPadding: Dp = 6.dp
private val PillChevronSize: Dp = 16.dp
private val PillChevronSpacing: Dp = 4.dp
private const val ChevronRotation = -90f

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
 * actions), followed by the other cards: stacked with only their top band visible when
 * collapsed, or as a full list of revealable cards when [expanded]. A small "View details"
 * pill sits midway between cards, with [CardStackDefaults.Gap] on both sides. Tapping
 * another card makes it active.
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
        }
    }
}

/**
 * The inactive cards tucked behind each other, CRED style: every card is drawn at full size,
 * each one pushed down by [CardStackDefaults.PeekHeight] and drawn over the previous, so the
 * cards behind show only their top band and continue behind the next (no cut corners), and
 * the front card is complete. Cards behind are slightly narrower and dimmed. Tap selects.
 */
@Composable
private fun StackedPeek(
    cards: ImmutableList<CreditCardUi>,
    onSelect: (AccountId) -> Unit,
) {
    val shape = RoundedCornerShape(CreditCardFaceDefaults.CornerRadius)
    Box(modifier = Modifier.fillMaxWidth()) {
        cards.forEachIndexed { index, card ->
            val depth = cards.lastIndex - index
            key(card.id) {
                CreditCardFace(
                    card = card,
                    modifier = Modifier
                        .padding(
                            top = CardStackDefaults.PeekHeight * index,
                            start = CardStackDefaults.StackInset * depth,
                            end = CardStackDefaults.StackInset * depth,
                        )
                        .clip(shape)
                        .drawWithContent {
                            drawContent()
                            if (depth > 0) drawRect(Color.Black.copy(alpha = CardStackDefaults.BehindScrimAlpha))
                        }
                        .clickable { onSelect(card.id) },
                )
            }
        }
    }
}

/**
 * A quiet caption-sized chip ("View details ›") centred between cards. A plain row rather
 * than a Material chip or button: those enforce a 48 dp touch height that would break the
 * equal spacing.
 */
@Composable
private fun ViewDetailsPill(
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = CardStackDefaults.Gap),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(UI.shapes.rFull)
                .border(width = PillBorder, color = UI.colors.medium, shape = UI.shapes.rFull)
                .clickable(onClick = onClick)
                .padding(horizontal = PillHorizontalPadding, vertical = PillVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.view_details),
                style = UI.typo.c.style(color = UI.colors.pureInverse),
            )
            Spacer(Modifier.width(PillChevronSpacing))
            IvyIcon(
                modifier = Modifier
                    .size(PillChevronSize)
                    .rotate(ChevronRotation),
                icon = R.drawable.ic_expand_more,
                tint = UI.colors.pureInverse,
                contentDescription = "view details",
            )
        }
    }
}
