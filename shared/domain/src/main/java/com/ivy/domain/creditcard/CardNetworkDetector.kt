package com.ivy.domain.creditcard

import com.ivy.data.model.CardNetwork

/**
 * Detects the card network from the leading digits of a card number.
 *
 * Ranges are matched longest-prefix-first, which resolves overlaps deterministically:
 * `6011…` is Discover, but every other `65…`/`60…` number is RuPay (an India-first
 * choice: Discover and RuPay share the 65 range).
 */
@Suppress("MagicNumber")
object CardNetworkDetector {
    private data class PrefixRange(
        val lo: Int,
        val hi: Int,
        val network: CardNetwork,
    ) {
        val length: Int = lo.toString().length
    }

    private val ranges: List<PrefixRange> = listOf(
        // 4-digit prefixes
        PrefixRange(6011, 6011, CardNetwork.DISCOVER),
        PrefixRange(2221, 2720, CardNetwork.MASTERCARD),
        PrefixRange(6521, 6522, CardNetwork.RUPAY),
        PrefixRange(6528, 6529, CardNetwork.RUPAY),
        // 3-digit prefixes
        PrefixRange(644, 649, CardNetwork.DISCOVER),
        PrefixRange(300, 305, CardNetwork.DINERS),
        PrefixRange(508, 508, CardNetwork.RUPAY),
        // 2-digit prefixes
        PrefixRange(34, 34, CardNetwork.AMEX),
        PrefixRange(37, 37, CardNetwork.AMEX),
        PrefixRange(36, 36, CardNetwork.DINERS),
        PrefixRange(38, 38, CardNetwork.DINERS),
        PrefixRange(51, 55, CardNetwork.MASTERCARD),
        PrefixRange(60, 60, CardNetwork.RUPAY),
        PrefixRange(65, 65, CardNetwork.RUPAY),
        PrefixRange(81, 82, CardNetwork.RUPAY),
        // 1-digit prefixes
        PrefixRange(4, 4, CardNetwork.VISA),
    )

    private val maxPrefixLength: Int = ranges.maxOf { it.length }

    fun detect(cardNumber: String): CardNetwork {
        val digits = normalizeCardNumber(cardNumber)
        return (maxPrefixLength downTo 1)
            .firstNotNullOfOrNull { length -> matchPrefix(digits, length) }
            ?: CardNetwork.UNKNOWN
    }

    private fun matchPrefix(digits: String, length: Int): CardNetwork? {
        val prefix = digits.takeIf { it.length >= length }?.take(length)?.toIntOrNull()
        return prefix?.let { p ->
            ranges.firstOrNull { it.length == length && p in it.lo..it.hi }?.network
        }
    }
}
