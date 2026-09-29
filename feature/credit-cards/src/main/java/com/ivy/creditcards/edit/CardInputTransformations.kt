package com.ivy.creditcards.edit

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.ivy.data.model.CardNetwork

private const val MaxCardNumberDigits = 19
private const val ExpiryDigits = 4
private const val ExpirySplit = 2
private val DefaultGroups = listOf(4, 4, 4, 4, 3)
private val AmexGroups = listOf(4, 6, 5)

/**
 * Shows digits in groups ("4111 1111 1111 1111"; "3782 822463 10005" for Amex) while the
 * underlying value stays digits only.
 */
class CardNumberVisualTransformation(
    private val network: CardNetwork,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.take(MaxCardNumberDigits)
        val groups = if (network == CardNetwork.AMEX) AmexGroups else DefaultGroups
        val boundaries = groups.runningReduce { acc, size -> acc + size }.filter { it < digits.length }
        val formatted = buildString {
            digits.forEachIndexed { index, char ->
                if (index in boundaries) append(' ')
                append(char)
            }
        }
        return TransformedText(AnnotatedString(formatted), GroupedOffsetMapping(boundaries))
    }

    private class GroupedOffsetMapping(private val boundaries: List<Int>) : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int = offset + boundaries.count { it <= offset && it > 0 }

        override fun transformedToOriginal(offset: Int): Int {
            var spaces = 0
            boundaries.forEach { boundary -> if (boundary + spaces < offset) spaces++ }
            return (offset - spaces).coerceAtLeast(0)
        }
    }
}

/**
 * "MMYY" digits shown as "MM/YY".
 */
class ExpiryVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.take(ExpiryDigits)
        val formatted = if (digits.length > ExpirySplit) {
            digits.substring(0, ExpirySplit) + "/" + digits.substring(ExpirySplit)
        } else {
            digits
        }
        return TransformedText(AnnotatedString(formatted), ExpiryOffsetMapping)
    }

    private object ExpiryOffsetMapping : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int = if (offset > ExpirySplit) offset + 1 else offset

        override fun transformedToOriginal(offset: Int): Int = if (offset > ExpirySplit) offset - 1 else offset
    }
}
