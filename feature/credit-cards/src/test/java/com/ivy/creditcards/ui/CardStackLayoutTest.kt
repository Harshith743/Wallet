package com.ivy.creditcards.ui

import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Test

class CardStackLayoutTest {
    private val width = 312.dp
    private val height = 200.dp
    private val pillRow = CardStackLayout.PillRow
    private val peek = CardStackDefaults.PeekHeight

    private fun slots(count: Int, active: Int, expanded: Boolean) = CardStackLayout.slots(
        count = count,
        activeIndex = active,
        expanded = expanded,
        cardWidth = width,
        cardHeight = height,
    )

    @Test
    fun `pill row is the pill with a gap above and below`() {
        pillRow shouldBe CardStackDefaults.PillHeight + CardStackDefaults.Gap * 2
    }

    @Test
    fun `collapsed - active on top, the others tucked behind each other`() {
        val (active, behind, front) = slots(count = 3, active = 0, expanded = false)

        active shouldBe CardSlot(y = 0.dp, scale = 1f, dim = 0f, zIndex = 3f, pillAlpha = 1f)
        behind.y shouldBe height + pillRow
        behind.scale shouldBe (width - CardStackDefaults.StackInset * 2) / width
        behind.dim shouldBe CardStackDefaults.BehindScrimAlpha
        behind.zIndex shouldBe 0f
        behind.pillAlpha shouldBe 0f
        front shouldBe CardSlot(y = height + pillRow + peek, scale = 1f, dim = 0f, zIndex = 1f, pillAlpha = 0f)
    }

    @Test
    fun `collapsed - an active card in the middle leaves the others in list order`() {
        val (first, active, last) = slots(count = 3, active = 1, expanded = false)

        active.y shouldBe 0.dp
        active.zIndex shouldBe 3f
        first.y shouldBe height + pillRow
        first.zIndex shouldBe 0f
        last.y shouldBe height + pillRow + peek
        last.zIndex shouldBe 1f
        last.scale shouldBe 1f
    }

    @Test
    fun `expanded - every card full size under the previous one, each with its pill`() {
        val (first, active, last) = slots(count = 3, active = 1, expanded = true)

        active shouldBe CardSlot(y = 0.dp, scale = 1f, dim = 0f, zIndex = 3f, pillAlpha = 1f)
        first shouldBe CardSlot(y = height + pillRow, scale = 1f, dim = 0f, zIndex = 0f, pillAlpha = 1f)
        last shouldBe CardSlot(y = (height + pillRow) * 2, scale = 1f, dim = 0f, zIndex = 1f, pillAlpha = 1f)
    }

    @Test
    fun `z order of the other cards is the same collapsed and expanded`() {
        val collapsed = slots(count = 4, active = 2, expanded = false).map { it.zIndex }
        val expanded = slots(count = 4, active = 2, expanded = true).map { it.zIndex }

        collapsed shouldBe expanded
    }

    @Test
    fun `an unknown active index falls back to the first card`() {
        slots(count = 2, active = -1, expanded = false) shouldBe slots(count = 2, active = 0, expanded = false)
        slots(count = 2, active = 7, expanded = false) shouldBe slots(count = 2, active = 0, expanded = false)
        slots(count = 0, active = 0, expanded = false) shouldBe emptyList()
    }

    @Test
    fun `heights of both arrangements`() {
        CardStackLayout.height(count = 0, expanded = false, cardHeight = height) shouldBe 0.dp
        CardStackLayout.height(count = 1, expanded = false, cardHeight = height) shouldBe height + pillRow
        CardStackLayout.height(count = 2, expanded = false, cardHeight = height) shouldBe height + pillRow + height
        CardStackLayout.height(count = 3, expanded = false, cardHeight = height) shouldBe
            height + pillRow + peek + height
        CardStackLayout.height(count = 3, expanded = true, cardHeight = height) shouldBe (height + pillRow) * 3
    }

    @Test
    fun `the front stacked card ends exactly at the collapsed height`() {
        val front = slots(count = 3, active = 0, expanded = false).last()

        front.y + height shouldBe CardStackLayout.height(count = 3, expanded = false, cardHeight = height)
    }
}
