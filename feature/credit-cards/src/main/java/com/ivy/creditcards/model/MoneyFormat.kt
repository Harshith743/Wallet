package com.ivy.creditcards.model

import com.ivy.legacy.utils.format
import com.ivy.ui.money.currencySymbol

/**
 * "₹15,859.10" style: the currency symbol followed by the locale-formatted amount.
 */
fun formatWithSymbol(amount: Double, currencyCode: String): String =
    currencySymbol(currencyCode) + amount.format(currencyCode)
