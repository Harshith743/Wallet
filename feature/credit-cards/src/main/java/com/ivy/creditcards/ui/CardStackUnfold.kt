package com.ivy.creditcards.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The maths of the scroll-driven unfold: the stack opens into the list in step with how far
 * the page has been scrolled from the top, and settles to one end when the finger lifts.
 */
object CardStackUnfold {
    /** Scroll distance from the top over which the stack fully unfolds. */
    val Distance: Dp = 220.dp

    /** From this progress on, the other cards count as listed (reveal gestures enabled). */
    const val ListedThreshold = 0.5f

    /** The other cards' pills fade in over the last part of the unfold. */
    const val PillRevealStart = 0.6f

    /** A fling faster than this (per second) settles in its own direction instead of the nearer end. */
    val SettleVelocity: Dp = 300.dp

    /** The cards segment's trailing space is at least this. */
    val MinimumTrailingSpace: Dp = 150.dp

    /** A safe (low) guess of the summary block above the stack, used only to size the trailing space. */
    val SummaryEstimate: Dp = 100.dp

    private const val SmoothstepA = 3f
    private const val SmoothstepB = 2f
    private const val Half = 0.5f

    /** 0 at the top, 1 once the first item has scrolled [distancePx] (or out of view). */
    fun progress(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int, distancePx: Float): Float =
        if (firstVisibleItemIndex > 0) 1f else (firstVisibleItemScrollOffset / distancePx).coerceIn(0f, 1f)

    /**
     * Where to settle when the finger lifts mid-unfold: a fling faster than [thresholdPx]
     * (positive = scrolling on) decides by direction, otherwise the nearer end. 1 = list.
     */
    fun settleTarget(progress: Float, velocityPx: Float, thresholdPx: Float): Float = when {
        velocityPx >= thresholdPx -> 1f
        velocityPx <= -thresholdPx -> 0f
        else -> if (progress >= Half) 1f else 0f
    }

    /**
     * The space after the stack so the page can always scroll [distance] from the top (the
     * unfold needs that even with two cards); a lone card has nothing to unfold.
     */
    fun trailingSpace(viewport: Dp, collapsedStackHeight: Dp, cardCount: Int): Dp =
        if (cardCount > 1) {
            maxOf(MinimumTrailingSpace, viewport + Distance - SummaryEstimate - collapsedStackHeight)
        } else {
            MinimumTrailingSpace
        }

    fun lerp(from: Float, to: Float, fraction: Float): Float = from + (to - from) * fraction

    /** 0 before [edge0], 1 after [edge1], eased in between. */
    fun smoothstep(edge0: Float, edge1: Float, value: Float): Float {
        val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (SmoothstepA - SmoothstepB * t)
    }
}
