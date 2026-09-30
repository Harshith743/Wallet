@file:Suppress("DataClassTypedIDs") // the rule does not recognise the app's typed ids

package com.ivy.creditcards.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.ivy.creditcards.skin.CardSkinUi
import com.ivy.data.model.AccountId
import com.ivy.data.model.CardNetwork

/**
 * Everything a card face or list row needs, already formatted. Built by the ViewModels
 * so composables (and screenshot tests) never touch ICU, locale formatting or the DB.
 */
@Immutable
data class CreditCardUi(
    val id: AccountId,
    val name: String,
    val issuer: String,
    val network: CardNetwork,
    val last4: String,
    val cardholderName: String,
    val color: Color,
    val dueAmount: Double,
    val dueText: String,
    val availableText: String,
    val limitText: String,
    val statement: StatementLabel,
    val repaymentAccountId: AccountId?,
    val payeeVpa: String?,
    val tier: String?,
    val skin: CardSkinUi,
)

@Immutable
sealed interface StatementLabel {
    data object DueToday : StatementLabel
    data class DueInDays(val days: Int) : StatementLabel
    data class DueOn(val dateText: String) : StatementLabel
    data class Overdue(val days: Int) : StatementLabel
    data object Paid : StatementLabel
    data object Awaited : StatementLabel
}

@Immutable
data class AccountChipUi(
    val id: AccountId,
    val name: String,
    val color: Color,
)
