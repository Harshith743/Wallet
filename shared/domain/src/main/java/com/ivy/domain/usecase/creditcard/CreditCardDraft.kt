package com.ivy.domain.usecase.creditcard

import com.ivy.data.model.AccountId
import com.ivy.data.model.CardNetwork
import com.ivy.data.model.CardSkinMode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.repository.CardSecretsError

/**
 * Raw form input for creating or editing a credit card. Validation happens in
 * [SaveCreditCardUseCase].
 *
 * @property pan full card number as typed (spaces allowed); null when editing without
 * re-entering the number, in which case the existing last 4, BIN and secrets are kept.
 * @property openingDue amount already billed when the card is first added (create only).
 * @property openingUnbilled amount spent but not yet billed when first added (create only).
 * @property skin how the card face is painted.
 */
@Suppress("DataClassTypedIDs") // the rule does not recognise nullable typed ids
data class CreditCardDraft(
    val name: String,
    val color: ColorInt,
    val pan: String?,
    val cvv: String?,
    val cardholderName: String?,
    val issuerOverride: String?,
    val networkOverride: CardNetwork?,
    val expiryMonth: Int,
    val expiryYear: Int,
    val creditLimit: Double,
    val billingDay: Int,
    val dueDay: Int,
    val repaymentAccountId: AccountId?,
    val payeeVpa: String?,
    val openingDue: Double,
    val openingUnbilled: Double,
    val skin: CardSkinMode,
)

sealed interface CreditCardError {
    data object BlankName : CreditCardError
    data object InvalidCardNumber : CreditCardError
    data object InvalidCvv : CreditCardError
    data object InvalidExpiry : CreditCardError
    data object ExpiredCard : CreditCardError
    data object InvalidCreditLimit : CreditCardError
    data object InvalidBillingDay : CreditCardError
    data object InvalidDueDay : CreditCardError
    data object InvalidOpeningAmount : CreditCardError
    data object CardNotFound : CreditCardError
    data class SecretsSaveFailed(val error: CardSecretsError) : CreditCardError
}
