package com.ivy.creditcards.preview

import androidx.compose.ui.graphics.Color
import com.ivy.creditcards.CreditCardsUiState
import com.ivy.creditcards.details.CreditCardDetailsUiState
import com.ivy.creditcards.model.PaymentUi
import com.ivy.creditcards.payments.CreditCardPaymentsUiState
import com.ivy.creditcards.edit.CardField
import com.ivy.creditcards.edit.EditCreditCardUiState
import com.ivy.creditcards.edit.FieldError
import com.ivy.creditcards.model.AccountChipUi
import com.ivy.creditcards.model.CreditCardUi
import com.ivy.creditcards.model.StatementLabel
import com.ivy.data.model.AccountId
import com.ivy.data.model.CardNetwork
import com.ivy.data.model.TransactionId
import com.ivy.design.IVY_COLOR_PICKER_COLORS_FREE
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID

/**
 * Deterministic sample data for previews and screenshot tests.
 */
object CreditCardsPreviewData {
    private val hdfcId = AccountId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private val sliceId = AccountId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
    private val oneCardId = AccountId(UUID.fromString("00000000-0000-0000-0000-000000000003"))
    private val bankId = AccountId(UUID.fromString("00000000-0000-0000-0000-000000000010"))

    val hdfc = CreditCardUi(
        id = hdfcId,
        name = "HDFC Pixel Go",
        issuer = "HDFC Bank",
        network = CardNetwork.VISA,
        last4 = "6304",
        cardholderName = "Harshith Reddy",
        color = Color(0xFF2B2C2D),
        dueAmount = 4_469.40,
        dueText = "₹4,469.40",
        availableText = "₹31,530.60",
        limitText = "₹36,000.00",
        statement = StatementLabel.DueOn("11 Oct"),
        repaymentAccountId = bankId,
        payeeVpa = "hdfccard@upi",
    )

    val slice = CreditCardUi(
        id = sliceId,
        name = "Slice",
        issuer = "Slice",
        network = CardNetwork.RUPAY,
        last4 = "6231",
        cardholderName = "Thippireddy",
        color = Color(0xFFA020F0),
        dueAmount = 9_722.28,
        dueText = "₹9,722.28",
        availableText = "₹40,277.72",
        limitText = "₹50,000.00",
        statement = StatementLabel.DueInDays(6),
        repaymentAccountId = null,
        payeeVpa = null,
    )

    val oneCard = CreditCardUi(
        id = oneCardId,
        name = "OneCard",
        issuer = "Federal Bank",
        network = CardNetwork.VISA,
        last4 = "8963",
        cardholderName = "T Harshith",
        color = Color(0xFF111114),
        dueAmount = 0.0,
        dueText = "₹0.00",
        availableText = "₹1,00,000.00",
        limitText = "₹1,00,000.00",
        statement = StatementLabel.Awaited,
        repaymentAccountId = null,
        payeeVpa = null,
    )

    val cards: ImmutableList<CreditCardUi> = persistentListOf(hdfc, slice, oneCard)

    private val cashId = AccountId(UUID.fromString("00000000-0000-0000-0000-000000000011"))

    val accounts: ImmutableList<AccountChipUi> = persistentListOf(
        AccountChipUi(id = bankId, name = "HDFC Savings", color = Color(0xFF14CC9E)),
        AccountChipUi(id = cashId, name = "Cash", color = Color(0xFFF29F30)),
    )

    fun cardsState(
        cards: ImmutableList<CreditCardUi> = this.cards,
        expanded: Boolean = false,
        revealedCardId: AccountId? = null,
    ): CreditCardsUiState = CreditCardsUiState(
        cards = cards,
        activeCardId = cards.firstOrNull()?.id,
        expanded = expanded,
        revealedCardId = revealedCardId,
        totalDueText = "₹14,191.68",
        dueCardsCount = 2,
        paySheet = null,
        deleteConfirmCardId = null,
        pendingUpi = null,
        loading = false,
    )

    fun editState(
        isEdit: Boolean = false,
        withErrors: Boolean = false,
    ): EditCreditCardUiState = EditCreditCardUiState(
        isEdit = isEdit,
        last4 = if (isEdit) "6304" else null,
        numberEntryVisible = !isEdit,
        cardNumber = if (isEdit) "" else "4111111111111111",
        detectedNetwork = CardNetwork.VISA,
        networkOverride = null,
        detectedIssuer = "HDFC Bank",
        issuer = "HDFC Bank",
        cardholderName = "Harshith Reddy",
        cardName = if (withErrors) "" else "HDFC Pixel Go",
        expiry = "1230",
        cvv = if (isEdit) "" else "123",
        creditLimit = "36,000.00",
        statementDay = "5",
        dueDay = if (withErrors) "45" else "25",
        openingDue = "",
        openingUnbilled = "",
        color = IVY_COLOR_PICKER_COLORS_FREE.first(),
        palette = IVY_COLOR_PICKER_COLORS_FREE.toImmutableList(),
        accounts = accounts,
        repaymentAccountId = bankId,
        payeeVpa = "hdfccard@upi",
        errors = if (withErrors) {
            persistentMapOf(CardField.NAME to FieldError.REQUIRED, CardField.DUE_DAY to FieldError.DAY_RANGE)
        } else {
            persistentMapOf()
        },
        deleteDialogVisible = false,
        saving = false,
        loading = false,
    )

    fun detailsState(
        revealed: Boolean = false,
    ): CreditCardDetailsUiState = CreditCardDetailsUiState(
        card = hdfc,
        revealed = revealed,
        fullNumberText = if (revealed) "4375 5112 3456 6304" else null,
        cvvText = if (revealed) "123" else null,
        secretsMissing = false,
        expiryText = "12/30",
        dueText = "₹4,469.40",
        unbilledText = "₹1,200.00",
        outstandingText = "₹5,669.40",
        availableText = "₹30,330.60",
        limitText = "₹36,000.00",
        statementDateText = "20 Sep 2026",
        dueDateText = "11 Oct 2026",
        nextStatementDateText = "20 Oct 2026",
        lastPaidOnText = "8 Sep 2026",
        repaymentAccountName = "HDFC Savings",
        payments = payments,
        deleteDialogVisible = false,
        loading = false,
    )

    val payments: ImmutableList<PaymentUi> = persistentListOf(
        PaymentUi(
            id = TransactionId(UUID.fromString("00000000-0000-0000-0000-000000000100")),
            dateText = "Tue, Sep 8 2026",
            amountText = "₹3,000.00",
            fromAccountName = "HDFC Savings",
            note = "August bill",
        ),
        PaymentUi(
            id = TransactionId(UUID.fromString("00000000-0000-0000-0000-000000000101")),
            dateText = "Sat, Aug 8 2026",
            amountText = "₹4,120.50",
            fromAccountName = "HDFC Savings",
            note = null,
        ),
        PaymentUi(
            id = TransactionId(UUID.fromString("00000000-0000-0000-0000-000000000102")),
            dateText = "Wed, Jul 8 2026",
            amountText = "₹2,000.00",
            fromAccountName = null,
            note = "Partial",
        ),
        PaymentUi(
            id = TransactionId(UUID.fromString("00000000-0000-0000-0000-000000000103")),
            dateText = "Mon, Jun 8 2026",
            amountText = "₹5,600.00",
            fromAccountName = "Cash",
            note = null,
        ),
    )

    fun paymentsState(
        empty: Boolean = false,
    ): CreditCardPaymentsUiState = CreditCardPaymentsUiState(
        cardName = hdfc.name,
        payments = if (empty) persistentListOf() else payments,
        loading = false,
    )
}
