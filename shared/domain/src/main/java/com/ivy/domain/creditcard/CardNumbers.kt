package com.ivy.domain.creditcard

/**
 * Keeps only the digits of a typed card number (drops spaces, dashes and anything else).
 */
fun normalizeCardNumber(raw: String): String = raw.filter { it.isDigit() }

private const val LuhnMinLength = 12
private const val DecimalBase = 10
private const val DoubledOverflow = 9

/**
 * Luhn check for a digits-only card number. Returns false for anything shorter than
 * 12 digits, empty input or non-digit characters.
 */
fun isValidLuhn(digits: String): Boolean {
    if (digits.length < LuhnMinLength || digits.any { !it.isDigit() }) return false
    var sum = 0
    var double = false
    for (index in digits.indices.reversed()) {
        var digit = digits[index] - '0'
        if (double) {
            digit *= 2
            if (digit > DoubledOverflow) digit -= DoubledOverflow
        }
        sum += digit
        double = !double
    }
    return sum % DecimalBase == 0
}
