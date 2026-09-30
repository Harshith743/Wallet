@file:Suppress("DataClassTypedIDs") // the rule does not recognise the app's typed ids

package com.ivy.creditcards.details

import androidx.compose.runtime.Immutable
import com.ivy.creditcards.model.CreditCardUi
import com.ivy.creditcards.model.PaymentUi
import kotlinx.collections.immutable.ImmutableList
import java.util.UUID

@Immutable
data class CreditCardDetailsUiState(
    val card: CreditCardUi?,
    val revealed: Boolean,
    val fullNumberText: String?,
    val cvvText: String?,
    val secretsMissing: Boolean,
    val expiryText: String,
    val dueText: String,
    val unbilledText: String,
    val outstandingText: String,
    val availableText: String,
    val limitText: String,
    val statementDateText: String,
    val dueDateText: String,
    val nextStatementDateText: String,
    val lastPaidOnText: String?,
    val repaymentAccountName: String?,
    val payments: ImmutableList<PaymentUi>,
    val deleteDialogVisible: Boolean,
    val loading: Boolean,
)

sealed interface CreditCardDetailsUiEvent {
    data class Load(val cardId: UUID) : CreditCardDetailsUiEvent
    data object ToggleReveal : CreditCardDetailsUiEvent
    data object DeleteClick : CreditCardDetailsUiEvent
    data object DeleteConfirm : CreditCardDetailsUiEvent
    data object DeleteDismiss : CreditCardDetailsUiEvent
}
