package com.ivy.data.model.primitive

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.forAll
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DayOfMonthTest {
    @Test
    fun `valid when in 1 to 31`() = runTest {
        forAll(Arb.int(min = 1, max = 31)) { day ->
            DayOfMonth.from(day).getOrNull()?.value == day
        }
    }

    @Test
    fun `invalid when LT 1`() = runTest {
        forAll(Arb.int(max = 0)) { day ->
            DayOfMonth.from(day).isLeft()
        }
    }

    @Test
    fun `invalid when GT 31`() = runTest {
        forAll(Arb.int(min = 32)) { day ->
            DayOfMonth.from(day).isLeft()
        }
    }

    @Test
    fun boundaries() {
        DayOfMonth.from(1).isRight() shouldBe true
        DayOfMonth.from(31).isRight() shouldBe true
        DayOfMonth.from(0).isLeft() shouldBe true
        DayOfMonth.from(32).isLeft() shouldBe true
    }
}
