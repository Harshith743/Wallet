package com.ivy.ui.money

import java.util.Locale

/**
 * The display symbol for a currency code, e.g. "₹" for INR or "$" for USD.
 * Falls back to the code itself when the platform has no symbol for it.
 *
 * @param lookup injectable for JVM tests (the default uses ICU on Android).
 */
fun currencySymbol(
    code: String,
    locale: Locale = Locale.getDefault(),
    lookup: (String, Locale) -> String? = ::icuSymbol,
): String {
    val symbol = lookup(code, locale)
    return symbol?.takeIf { it.isNotBlank() && !it.equals(code, ignoreCase = true) } ?: code
}

private fun icuSymbol(code: String, locale: Locale): String? = runCatching {
    android.icu.util.Currency.getInstance(code).getSymbol(locale)
}.getOrNull()
