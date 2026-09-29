package com.ivy.creditcards.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
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

/**
 * The active card on top with its "View details" pill and [activeExtras] (quick actions),
 * followed by the other cards: stacked with only their top band visible when collapsed,
 * or as a full list when [expanded]. Tapping the active card toggles, tapping another
 * card makes it active.
 */
@Composable
fun CardStack(
    cards: ImmutableList<CreditCardUi>,
    activeCardId: AccountId?,
    expanded: Boolean,
    callbacks: CardStackCallbacks,
    modifier: Modifier = Modifier,
    activeExtras: @Composable ColumnScope.(CreditCardUi) -> Unit = {},
) {
    val active = cards.firstOrNull { it.id == activeCardId } ?: cards.firstOrNull() ?: return
    val others = cards.filter { it.id != active.id }.toImmutableList()
    Column(modifier = modifier.animateContentSize()) {
        CreditCardFace(
            card = active,
            modifier = Modifier.clickable { callbacks.onToggleExpanded() },
            showPayNow = true,
            onPayNow = { callbacks.onPayNow(active) },
        )
        ViewDetailsPill(onClick = { callbacks.onViewDetails(active.id) })
        activeExtras(active)
        if (others.isEmpty()) return@Column
        Spacer(Modifier.height(CardStackDefaults.Gap))
        if (expanded) {
            ExpandedList(cards = others, callbacks = callbacks)
        } else {
            StackedPeek(cards = others, onSelect = callbacks.onSelectCard)
        }
    }
}

data class CardStackCallbacks(
    val onSelectCard: (AccountId) -> Unit,
    val onToggleExpanded: () -> Unit,
    val onPayNow: (CreditCardUi) -> Unit,
    val onViewDetails: (AccountId) -> Unit,
)

@Composable
private fun ExpandedList(
    cards: ImmutableList<CreditCardUi>,
    callbacks: CardStackCallbacks,
) {
    Column {
        cards.forEach { card ->
            CreditCardFace(
                card = card,
                modifier = Modifier.clickable { callbacks.onSelectCard(card.id) },
            )
            ViewDetailsPill(onClick = { callbacks.onViewDetails(card.id) })
            Spacer(Modifier.height(CardStackDefaults.Gap))
        }
    }
}

/**
 * Every inactive card is cropped to its top band (issuer, due amount, status); the last
 * one gets two bands so the stack reads as cards tucked behind each other.
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
