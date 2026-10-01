package com.ivy.creditcards.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ivy.creditcards.model.CreditCardUi
import com.ivy.data.model.AccountId
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.IvyIcon
import kotlinx.collections.immutable.ImmutableList

object CardStackDefaults {
    /** How much of each card behind the front one stays visible. */
    val PeekHeight: Dp = 72.dp

    /** Air between a card and the "View details" pill on either side of it. */
    val Gap: Dp = 12.dp

    /** The height reserved for the pill itself (its row adds [Gap] above and below). */
    val PillHeight: Dp = 30.dp

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

/** Pills sit below every card so cards pass over them while moving. */
private const val PillZIndex = -1f
private val TopCenter = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)

/**
 * How cards travel between slots: a spring without bounce (about 400 ms) that retargets
 * smoothly when another change arrives mid-flight.
 */
private val CardMotion = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)
private val HeightMotion = spring<Dp>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

/** A pill leaves quickly when its card starts moving and returns once the cards have nearly settled. */
private val PillFadeOut = tween<Float>(durationMillis = 120)
private val PillFadeIn = tween<Float>(durationMillis = 200, delayMillis = 220)

data class CardStackCallbacks(
    val onSelectCard: (AccountId) -> Unit,
    val onPayNow: (CreditCardUi) -> Unit,
    val onViewDetails: (AccountId) -> Unit,
    val onReveal: (AccountId) -> Unit,
    val onCloseReveal: (AccountId) -> Unit,
)

/**
 * Every card of the segment in one layout. The active card sits on top (tap opens its
 * details, swipe left reveals the quick actions) with a "View details" pill under it; the
 * others are tucked behind each other below it when collapsed, or listed full size, each
 * with its own pill, when [expanded]. Expanding and collapsing is driven by the page scroll
 * (see the Accounts tab); tapping another card makes it active.
 *
 * Each card is the same keyed node in both arrangements and glides to its [CardSlot] (see
 * [CardStackLayout]) whenever the arrangement or the active card changes: the stack unfolds
 * into the list and back, and a tapped card rises to the top in front of the old active
 * card, which sinks into the stack, while the pill fades out and back in.
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
    if (cards.isEmpty()) return
    val activeIndex = cards.indexOfFirst { it.id == activeCardId }.coerceAtLeast(0)
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cardWidth = maxWidth
        val cardHeight = maxWidth / CreditCardFaceDefaults.AspectRatio
        val slots = remember(cards.size, activeIndex, expanded, cardWidth) {
            CardStackLayout.slots(
                count = cards.size,
                activeIndex = activeIndex,
                expanded = expanded,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
            )
        }
        val density = LocalDensity.current
        val cardHeightPx = with(density) { cardHeight.toPx() }
        // Growing (expand) is immediate so the ongoing drag is never clamped; shrinking glides
        val stackHeight = animateDpAsState(
            targetValue = CardStackLayout.height(count = cards.size, expanded = expanded, cardHeight = cardHeight),
            animationSpec = if (expanded) snap() else HeightMotion,
            label = "card stack height",
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // The stack's height is set in the layout phase; children keep fixed constraints
                .layout { measurable, constraints ->
                    val height = stackHeight.value.roundToPx()
                    val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                    layout(placeable.width, height) { placeable.place(0, 0) }
                }
        ) {
            cards.forEachIndexed { index, card ->
                val slot = slots[index]
                val isActive = index == activeIndex
                key(card.id) {
                    // Read only inside draw/layer lambdas: moving a card never recomposes it
                    val y = animateFloatAsState(
                        targetValue = with(density) { slot.y.toPx() },
                        animationSpec = CardMotion,
                        label = "card y",
                    )
                    val scale = animateFloatAsState(slot.scale, CardMotion, label = "card scale")
                    val dim = animateFloatAsState(slot.dim, CardMotion, label = "card dim")
                    val pillAlpha = animateFloatAsState(
                        targetValue = slot.pillAlpha,
                        animationSpec = if (slot.pillAlpha == 1f) PillFadeIn else PillFadeOut,
                        label = "pill alpha",
                    )
                    RevealableCard(
                        card = card,
                        revealed = revealedCardId == card.id,
                        callbacks = RevealCallbacks(
                            onClick = {
                                if (isActive) callbacks.onViewDetails(card.id) else callbacks.onSelectCard(card.id)
                            },
                            onReveal = { callbacks.onReveal(card.id) },
                            onCloseReveal = { callbacks.onCloseReveal(card.id) },
                            onPayNow = { callbacks.onPayNow(card) },
                        ),
                        modifier = Modifier
                            .zIndex(slot.zIndex)
                            // Unbounded: the card keeps the same constraints whatever the stack's height
                            .wrapContentHeight(align = Alignment.Top, unbounded = true)
                            .graphicsLayer {
                                translationY = y.value
                                scaleX = scale.value
                                scaleY = scale.value
                                transformOrigin = TopCenter
                            },
                        showPayNow = isActive,
                        // Stacked cards are tap-only; the active card and listed cards can reveal
                        revealEnabled = isActive || expanded,
                        dim = { dim.value },
                    ) {
                        QuickActionGrid(cardId = card.id, callbacks = quickActions)
                    }
                    ViewDetailsPill(
                        enabled = slot.pillAlpha == 1f,
                        onClick = { callbacks.onViewDetails(card.id) },
                        modifier = Modifier
                            .zIndex(PillZIndex)
                            .graphicsLayer {
                                translationY = y.value + cardHeightPx * scale.value
                                alpha = pillAlpha.value
                            },
                    )
                }
            }
        }
    }
}

/**
 * A quiet caption-sized chip ("View details ›") centred in a row of
 * [CardStackLayout.PillRow] height. A plain row rather than a Material chip or button:
 * those enforce a 48 dp touch height that would break the equal spacing.
 */
@Composable
private fun ViewDetailsPill(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CardStackLayout.PillRow),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                // Never clipped at large font sizes: the pill may grow past the reserved height
                .wrapContentHeight(unbounded = true)
                .clip(UI.shapes.rFull)
                .border(width = PillBorder, color = UI.colors.medium, shape = UI.shapes.rFull)
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
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
