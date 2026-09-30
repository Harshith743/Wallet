package com.ivy.creditcards.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

/**
 * Fires [onSwipeUp] once when the pointer is dragged up past [RevealDefaults.SwipeUpThreshold].
 *
 * Only an upward, mostly vertical touch slop is consumed. Downward drags are left alone so
 * the enclosing list still scrolls, and mostly horizontal drags are left to the horizontal
 * reveal (Compose arbitrates pointer input purely by consumption).
 */
fun Modifier.swipeUpToExpand(
    enabled: Boolean,
    onSwipeUp: () -> Unit,
): Modifier = if (!enabled) {
    this
} else {
    pointerInput(onSwipeUp) {
        val threshold = RevealDefaults.SwipeUpThreshold.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var total = 0f
            val start = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
                val dx = change.position.x - down.position.x
                val dy = change.position.y - down.position.y
                if (overSlop < 0f && abs(dy) > abs(dx)) {
                    total = overSlop
                    change.consume()
                }
            } ?: return@awaitEachGesture
            var fired = false
            verticalDrag(start.id) { change ->
                total += change.positionChange().y
                change.consume()
                if (!fired && total <= -threshold) {
                    fired = true
                    onSwipeUp()
                }
            }
        }
    }
}
