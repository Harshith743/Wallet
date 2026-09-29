package com.ivy.data.model

import com.ivy.data.model.primitive.CardBin
import com.ivy.data.model.primitive.CardCvv
import com.ivy.data.model.primitive.CardLast4
import com.ivy.data.model.primitive.CardPan
import com.ivy.data.model.primitive.DayOfMonth
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.model.sync.Identifiable
import java.time.YearMonth

enum class CardNetwork {
    VISA,
    MASTERCARD,
    RUPAY,
    AMEX,
    DINERS,
    DISCOVER,
    UNKNOWN,
}

/**
 * Card-specific details of a credit card.
 *
 * A credit card is backed by an [Account] with the same [id]: the account row holds the
 * name, colour, icon, currency and order and receives the card's transactions; this
 * model holds only what is specific to the card. Amounts (due, unbilled, outstanding,
 * available limit) are derived from the account's transactions and are not stored.
 */
@Suppress("DataClassTypedIDs") // the rule does not recognise nullable typed ids
data class CreditCard(
    override val id: AccountId,
    val cardholderName: NotBlankTrimmedString?,
    val issuer: NotBlankTrimmedString?,
    val network: CardNetwork,
    val last4: CardLast4,
    val bin: CardBin?,
    val expiry: YearMonth,
    val creditLimit: PositiveDouble,
    val billingDay: DayOfMonth,
    val dueDay: DayOfMonth,
    val repaymentAccountId: AccountId?,
    val payeeVpa: NotBlankTrimmedString?,
) : Identifiable<AccountId>

/**
 * Sensitive card data, stored encrypted outside the database.
 */
data class CardSecrets(
    val pan: CardPan?,
    val cvv: CardCvv?,
)
