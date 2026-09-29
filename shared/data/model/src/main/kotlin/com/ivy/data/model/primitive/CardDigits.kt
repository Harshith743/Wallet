package com.ivy.data.model.primitive

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import com.ivy.data.model.exact.Exact

private const val MASK = "****"

private fun Raise<String>.ensureDigits(raw: String, min: Int, max: Int, what: String) {
    ensure(raw.length in min..max) { "$what must have $min..$max digits" }
    ensure(raw.all { it.isDigit() }) { "$what must contain only digits" }
}

/**
 * Full card number (PAN), digits only, 12..19 characters.
 * Never printed: [toString] is masked.
 */
@JvmInline
value class CardPan private constructor(val value: String) {
    override fun toString(): String = MASK

    companion object : Exact<String, CardPan> {
        const val MIN_LENGTH = 12
        const val MAX_LENGTH = 19

        override val exactName = "CardPan"

        override fun Raise<String>.spec(raw: String): CardPan {
            ensureDigits(raw, MIN_LENGTH, MAX_LENGTH, "Card number")
            return CardPan(raw)
        }
    }
}

/**
 * Card verification value, 3..4 digits. Never printed: [toString] is masked.
 */
@JvmInline
value class CardCvv private constructor(val value: String) {
    override fun toString(): String = MASK

    companion object : Exact<String, CardCvv> {
        const val MIN_LENGTH = 3
        const val MAX_LENGTH = 4

        override val exactName = "CardCvv"

        override fun Raise<String>.spec(raw: String): CardCvv {
            ensureDigits(raw, MIN_LENGTH, MAX_LENGTH, "CVV")
            return CardCvv(raw)
        }
    }
}

/**
 * The last four digits of a card number, safe to display.
 */
@JvmInline
value class CardLast4 private constructor(val value: String) {
    companion object : Exact<String, CardLast4> {
        const val LENGTH = 4

        override val exactName = "CardLast4"

        override fun Raise<String>.spec(raw: String): CardLast4 {
            ensureDigits(raw, LENGTH, LENGTH, "Last 4 digits")
            return CardLast4(raw)
        }
    }
}

/**
 * Bank identification number: the first six digits of a card number.
 */
@JvmInline
value class CardBin private constructor(val value: String) {
    companion object : Exact<String, CardBin> {
        const val LENGTH = 6

        override val exactName = "CardBin"

        override fun Raise<String>.spec(raw: String): CardBin {
            ensureDigits(raw, LENGTH, LENGTH, "BIN")
            return CardBin(raw)
        }
    }
}
