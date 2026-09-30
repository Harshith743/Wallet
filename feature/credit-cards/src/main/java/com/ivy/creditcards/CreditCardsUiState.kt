@file:Suppress("DataClassTypedIDs") // the rule does not recognise the app's typed ids

package com.ivy.creditcards

import androidx.compose.runtime.Immutable
import com.ivy.creditcards.model.AccountChipUi
import com.ivy.creditcards.model.CreditCardUi
import com.ivy.creditcards.pay.UpiPaymentRequest
import com.ivy.data.model.AccountId
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class CreditCardsUiState(
    val cards: ImmutableList<CreditCardUi>,
    val activeCardId: AccountId?,
    val expanded: Boolean,
    /** The card whose quick-action grid is revealed (at most one). */
    val revealedCardId: AccountId?,
    val totalDueText: String,
    val dueCardsCount: Int,
    val paySheet: PaySheetUi?,
    val deleteConfirmCardId: AccountId?,
    val pendingUpi: UpiPaymentRequest?,
    val reorderVisible: Boolean,
    val loading: Boolean,
)

@Immutable
data class PaySheetUi(
    val cardId: AccountId,
    val cardName: String,
    val payeeVpa: String?,
    val currencySymbol: String,
    val amountText: String,
    val launchUpi: Boolean,
    val accounts: ImmutableList<AccountChipUi>,
    val selectedAccountId: AccountId?,
    val note: String,
    val amountError: Boolean,
    val accountError: Boolean,
)

sealed interface CreditCardsUiEvent {
    data class SelectCard(val id: AccountId) : CreditCardsUiEvent

    /** Page dragged up: the stack unfolds into the list. */
    data object ExpandStack : CreditCardsUiEvent

    /** List pulled down at the top: the list folds back into the stack. */
    data object CollapseStack : CreditCardsUiEvent

    data class Reveal(val id: AccountId) : CreditCardsUiEvent

    /** Close the revealed card; with an id only if that card is the revealed one. */
    data class CloseReveal(val id: AccountId?) : CreditCardsUiEvent
    data class PayNowClick(val id: AccountId) : CreditCardsUiEvent
    data class MarkAsPaidClick(val id: AccountId) : CreditCardsUiEvent

    /** Events of the open pay sheet. */
    sealed interface PaySheetEvent : CreditCardsUiEvent
    data class PaySheetAmountChange(val text: String) : PaySheetEvent
    data class PaySheetAccountSelect(val id: AccountId) : PaySheetEvent
    data class PaySheetNoteChange(val note: String) : PaySheetEvent
    data object PaySheetConfirm : PaySheetEvent
    data object PaySheetDismiss : PaySheetEvent
    data class DeleteClick(val id: AccountId) : CreditCardsUiEvent
    data object DeleteConfirm : CreditCardsUiEvent
    data object DeleteDismiss : CreditCardsUiEvent
    data object UpiLaunched : CreditCardsUiEvent
    data class ReorderModalVisible(val visible: Boolean) : CreditCardsUiEvent

    /** The cards in their new order (every current card id, once). */
    data class Reorder(val orderedIds: List<AccountId>) : CreditCardsUiEvent
    data object Refresh : CreditCardsUiEvent
}
