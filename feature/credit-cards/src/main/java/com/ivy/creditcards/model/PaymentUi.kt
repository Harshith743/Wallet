@file:Suppress("DataClassTypedIDs") // the rule does not recognise the app's typed ids

package com.ivy.creditcards.model

import androidx.compose.runtime.Immutable
import com.ivy.data.model.TransactionId
import com.ivy.domain.usecase.creditcard.CreditCardRepayment
import com.ivy.ui.time.TimeFormatter
import javax.inject.Inject

@Immutable
data class PaymentUi(
    val id: TransactionId,
    val dateText: String,
    val amountText: String,
    val fromAccountName: String?,
    val note: String?,
)

class PaymentUiMapper @Inject constructor(
    private val timeFormatter: TimeFormatter,
) {
    fun map(repayment: CreditCardRepayment, currency: String): PaymentUi = PaymentUi(
        id = repayment.transfer.id,
        dateText = with(timeFormatter) {
            repayment.transfer.time.formatLocal(TimeFormatter.Style.DateOnly(includeWeekDay = true))
        },
        amountText = formatWithSymbol(repayment.transfer.toValue.amount.value, currency),
        fromAccountName = repayment.paidFrom?.name?.value,
        note = repayment.transfer.description?.value,
    )
}
