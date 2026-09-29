package com.ivy.data.model.primitive

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import com.ivy.data.model.exact.Exact

/**
 * A day of the month in the range 1..31.
 * Months with fewer days clamp it to their last day when a date is derived.
 */
@JvmInline
value class DayOfMonth private constructor(val value: Int) {
    companion object : Exact<Int, DayOfMonth> {
        const val MIN = 1
        const val MAX = 31

        override val exactName = "DayOfMonth"

        override fun Raise<String>.spec(raw: Int): DayOfMonth {
            ensure(raw in MIN..MAX) { "$raw is not in $MIN..$MAX" }
            return DayOfMonth(raw)
        }
    }
}
