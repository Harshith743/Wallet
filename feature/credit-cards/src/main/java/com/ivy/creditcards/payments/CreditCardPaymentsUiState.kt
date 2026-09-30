package com.ivy.creditcards.payments

import androidx.compose.runtime.Immutable
import com.ivy.creditcards.model.PaymentUi
import kotlinx.collections.immutable.ImmutableList
import java.util.UUID

@Immutable
data class CreditCardPaymentsUiState(
    val cardName: String?,
    val payments: ImmutableList<PaymentUi>,
    val loading: Boolean,
)

sealed interface CreditCardPaymentsUiEvent {
    @Suppress("DataClassTypedIDs")
    data class Load(val cardId: UUID) : CreditCardPaymentsUiEvent
}
