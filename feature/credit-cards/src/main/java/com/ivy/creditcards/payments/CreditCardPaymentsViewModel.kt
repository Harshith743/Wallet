package com.ivy.creditcards.payments

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.threading.DispatchersProvider
import com.ivy.creditcards.model.PaymentUi
import com.ivy.creditcards.model.PaymentUiMapper
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.model.AccountId
import com.ivy.data.repository.AccountRepository
import com.ivy.domain.usecase.creditcard.CreditCardPaymentsUseCase
import com.ivy.ui.ComposeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@HiltViewModel
class CreditCardPaymentsViewModel @Inject constructor(
    private val paymentsUseCase: CreditCardPaymentsUseCase,
    private val accountRepository: AccountRepository,
    private val paymentUiMapper: PaymentUiMapper,
    private val dataObserver: DataObserver,
    private val dispatchers: DispatchersProvider,
) : ComposeViewModel<CreditCardPaymentsUiState, CreditCardPaymentsUiEvent>() {

    private var cardId: AccountId? = null
    private var cardName by mutableStateOf<String?>(null)
    private var payments by mutableStateOf<ImmutableList<PaymentUi>>(persistentListOf())
    private var loading by mutableStateOf(true)

    init {
        viewModelScope.launch {
            dataObserver.writeEvents.collectLatest { event ->
                if (event is DataWriteEvent.CreditCardChange || event is DataWriteEvent.AccountChange) {
                    cardId?.let { load(it) }
                }
            }
        }
    }

    @Composable
    override fun uiState(): CreditCardPaymentsUiState = CreditCardPaymentsUiState(
        cardName = cardName,
        payments = payments,
        loading = loading,
    )

    override fun onEvent(event: CreditCardPaymentsUiEvent) {
        viewModelScope.launch(dispatchers.default) {
            when (event) {
                is CreditCardPaymentsUiEvent.Load -> load(AccountId(event.cardId))
            }
        }
    }

    private suspend fun load(id: AccountId) {
        cardId = id
        val account = accountRepository.findById(id)
        cardName = account?.name?.value
        val currency = account?.asset?.code.orEmpty()
        payments = paymentsUseCase.history(id)
            .map { paymentUiMapper.map(it, currency) }
            .toImmutableList()
        loading = false
    }
}
