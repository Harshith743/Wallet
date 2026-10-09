package com.ivy.creditcards.ui

import androidx.compose.ui.unit.dp
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.Test

class CardStackUnfoldTest {
    @Test
    fun `progress follows the first item's scroll offset and saturates`() {
        CardStackUnfold.progress(0, 0, 200f) shouldBe 0f
        CardStackUnfold.progress(0, 50, 200f) shouldBe 0.25f
        CardStackUnfold.progress(0, 200, 200f) shouldBe 1f
        CardStackUnfold.progress(0, 900, 200f) shouldBe 1f
        CardStackUnfold.progress(1, 0, 200f) shouldBe 1f
    }

    @Test
    fun `settle - a fast fling decides by direction, otherwise the nearer end wins`() {
        CardStackUnfold.settleTarget(progress = 0.2f, velocityPx = 900f, thresholdPx = 300f) shouldBe 1f
        CardStackUnfold.settleTarget(progress = 0.8f, velocityPx = -900f, thresholdPx = 300f) shouldBe 0f
        CardStackUnfold.settleTarget(progress = 0.2f, velocityPx = 100f, thresholdPx = 300f) shouldBe 0f
        CardStackUnfold.settleTarget(progress = 0.5f, velocityPx = 0f, thresholdPx = 300f) shouldBe 1f
        CardStackUnfold.settleTarget(progress = 0.7f, velocityPx = -100f, thresholdPx = 300f) shouldBe 1f
    }

    @Test
    fun `trailing space lets the page scroll the unfold distance, except for a lone card`() {
        val viewport = 700.dp
        val stack = 300.dp
        CardStackUnfold.trailingSpace(viewport, stack, cardCount = 3) shouldBe
            viewport + CardStackUnfold.Distance - CardStackUnfold.SummaryEstimate - stack
        CardStackUnfold.trailingSpace(viewport = 100.dp, collapsedStackHeight = 900.dp, cardCount = 3) shouldBe
            CardStackUnfold.MinimumTrailingSpace
        CardStackUnfold.trailingSpace(viewport, stack, cardCount = 1) shouldBe CardStackUnfold.MinimumTrailingSpace
    }

    @Test
    fun `lerp and smoothstep`() {
        CardStackUnfold.lerp(10f, 20f, 0.25f) shouldBe 12.5f
        CardStackUnfold.smoothstep(0.6f, 1f, 0.5f) shouldBe 0f
        CardStackUnfold.smoothstep(0.6f, 1f, 1f) shouldBe 1f
        CardStackUnfold.smoothstep(0.6f, 1f, 0.8f) shouldBe (0.5f plusOrMinus 0.001f)
    }
}
