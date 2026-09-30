@file:OptIn(ExperimentalFoundationApi::class)

package com.ivy.creditcards.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ivy.creditcards.model.CreditCardUi
import kotlinx.coroutines.flow.drop
import kotlin.math.roundToInt

enum class RevealValue {
    Closed,
    Open,
}

object RevealDefaults {
    /** How far the face slides left, as a fraction of its width. */
    const val OpenFraction = 0.55f

    /** Fraction of the travel after which a release settles on the far anchor. */
    const val PositionalThreshold = 0.4f
    val VelocityThreshold: Dp = 125.dp
    val SwipeUpThreshold: Dp = 72.dp
}

data class RevealCallbacks(
    val onClick: () -> Unit,
    val onReveal: () -> Unit,
    val onCloseReveal: () -> Unit,
    val onPayNow: () -> Unit,
    val onSwipeUp: () -> Unit,
)

/**
 * A card face that slides left to reveal [actions] behind it (swipe left to open,
 * swipe right to close), optionally swipes up, and reports settled reveal changes to the
 * caller. Opening is always user-driven; the caller closes it by flipping [revealed].
 */
@Composable
fun RevealableCard(
    card: CreditCardUi,
    revealed: Boolean,
    callbacks: RevealCallbacks,
    modifier: Modifier = Modifier,
    showPayNow: Boolean = false,
    swipeUpEnabled: Boolean = false,
    actions: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val openOffset = -constraints.maxWidth * RevealDefaults.OpenFraction
        val state = remember {
            AnchoredDraggableState(
                initialValue = if (revealed) RevealValue.Open else RevealValue.Closed,
                anchors = revealAnchors(openOffset),
                positionalThreshold = { distance -> distance * RevealDefaults.PositionalThreshold },
                velocityThreshold = { with(density) { RevealDefaults.VelocityThreshold.toPx() } },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
        LaunchedEffect(openOffset) {
            state.updateAnchors(revealAnchors(openOffset), state.targetValue)
        }
        val current by rememberUpdatedState(callbacks)

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(RevealDefaults.OpenFraction)
                .graphicsLayer { alpha = revealProgress(state.requireOffset(), openOffset) },
        ) {
            actions()
        }
        CreditCardFace(
            card = card,
            modifier = Modifier
                .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                .anchoredDraggable(state, Orientation.Horizontal)
                .swipeUpToExpand(enabled = swipeUpEnabled) { current.onSwipeUp() }
                .clickable {
                    if (state.currentValue == RevealValue.Open) current.onCloseReveal() else current.onClick()
                },
            showPayNow = showPayNow,
            onPayNow = { current.onPayNow() },
        )

        // Caller -> card: only ever closes; opening comes from the drag
        LaunchedEffect(revealed) {
            if (!revealed && state.currentValue != RevealValue.Closed) {
                state.animateTo(RevealValue.Closed)
            }
        }
        // Card -> caller: settled value changes (drop the initial emission)
        LaunchedEffect(state) {
            snapshotFlow { state.currentValue }
                .drop(1)
                .collect { value ->
                    if (value == RevealValue.Open) current.onReveal() else current.onCloseReveal()
                }
        }
    }
}

private fun revealAnchors(openOffset: Float): DraggableAnchors<RevealValue> = DraggableAnchors {
    RevealValue.Closed at 0f
    RevealValue.Open at openOffset
}

/** 0 when closed, 1 when fully open. Not [AnchoredDraggableState.progress], which is 1 at any settled anchor. */
private fun revealProgress(offset: Float, openOffset: Float): Float =
    if (openOffset == 0f) 0f else (offset / openOffset).coerceIn(0f, 1f)
