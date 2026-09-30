package com.ivy.creditcards.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.threading.DispatchersProvider
import com.ivy.creditcards.model.CreditCardUi
import com.ivy.creditcards.model.CreditCardUiMapper
import com.ivy.creditcards.model.PaymentUi
import com.ivy.creditcards.model.PaymentUiMapper
import com.ivy.creditcards.model.formatWithSymbol
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.model.AccountId
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CardSecretsError
import com.ivy.data.repository.CreditCardSecretsRepository
import com.ivy.domain.usecase.creditcard.CreditCardPaymentsUseCase
import com.ivy.domain.usecase.creditcard.CreditCardsOverviewUseCase
import com.ivy.domain.usecase.creditcard.DeleteCreditCardUseCase
import com.ivy.navigation.Navigation
import com.ivy.ui.ComposeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val RevealTimeoutMs = 30_000L
private const val PanGroupSize = 4
private const val YearInCentury = 100

@Stable
@HiltViewModel
class CreditCardDetailsViewModel @Inject constructor(
    private val overviewUseCase: CreditCardsOverviewUseCase,
    private val paymentsUseCase: CreditCardPaymentsUseCase,
    private val secretsRepository: CreditCardSecretsRepository,
    private val deleteCreditCardUseCase: DeleteCreditCardUseCase,
    private val accountRepository: AccountRepository,
    private val uiMapper: CreditCardUiMapper,
    private val paymentUiMapper: PaymentUiMapper,
    private val nav: Navigation,
    private val dataObserver: DataObserver,
    private val dispatchers: DispatchersProvider,
) : ComposeViewModel<CreditCardDetailsUiState, CreditCardDetailsUiEvent>() {

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private var cardId: AccountId? = null
    private var revealJob: Job? = null

    private var card by mutableStateOf<CreditCardUi?>(null)
    private var revealed by mutableStateOf(false)
    private var fullNumberText by mutableStateOf<String?>(null)
    private var cvvText by mutableStateOf<String?>(null)
    private var secretsMissing by mutableStateOf(false)
    private var expiryText by mutableStateOf("")
    private var dueText by mutableStateOf("")
    private var unbilledText by mutableStateOf("")
    private var outstandingText by mutableStateOf("")
    private var availableText by mutableStateOf("")
    private var limitText by mutableStateOf("")
    private var statementDateText by mutableStateOf("")
    private var dueDateText by mutableStateOf("")
    private var nextStatementDateText by mutableStateOf("")
    private var lastPaidOnText by mutableStateOf<String?>(null)
    private var repaymentAccountName by mutableStateOf<String?>(null)
    private var payments by mutableStateOf<ImmutableList<PaymentUi>>(persistentListOf())
    private var usedFraction by mutableFloatStateOf(0f)
    private var moreDetailsExpanded by mutableStateOf(false)
    private var deleteDialogVisible by mutableStateOf(false)
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
    override fun uiState(): CreditCardDetailsUiState = CreditCardDetailsUiState(
        card = card,
        revealed = revealed,
        fullNumberText = fullNumberText,
        cvvText = cvvText,
        secretsMissing = secretsMissing,
        expiryText = expiryText,
        dueText = dueText,
        unbilledText = unbilledText,
        outstandingText = outstandingText,
        availableText = availableText,
        limitText = limitText,
        statementDateText = statementDateText,
        dueDateText = dueDateText,
        nextStatementDateText = nextStatementDateText,
        lastPaidOnText = lastPaidOnText,
        repaymentAccountName = repaymentAccountName,
        payments = payments,
        usedFraction = usedFraction,
        moreDetailsExpanded = moreDetailsExpanded,
        deleteDialogVisible = deleteDialogVisible,
        loading = loading,
    )

    override fun onEvent(event: CreditCardDetailsUiEvent) {
        viewModelScope.launch(dispatchers.default) {
            when (event) {
                is CreditCardDetailsUiEvent.Load -> load(AccountId(event.cardId))
                CreditCardDetailsUiEvent.ToggleReveal -> toggleReveal()
                CreditCardDetailsUiEvent.DeleteClick -> deleteDialogVisible = true
                CreditCardDetailsUiEvent.DeleteConfirm -> delete()
                CreditCardDetailsUiEvent.DeleteDismiss -> deleteDialogVisible = false
                CreditCardDetailsUiEvent.ToggleMoreDetails -> moreDetailsExpanded = !moreDetailsExpanded
            }
        }
    }

    private suspend fun load(id: AccountId) {
        cardId = id
        val item = overviewUseCase.statement(id)
        if (item == null) {
            // The card was deleted (for example from its Transactions screen)
            nav.back()
            return
        }
        val currency = item.account.asset.code
        card = uiMapper.map(item, currency)
        expiryText = "%02d/%02d".format(item.card.expiry.monthValue, item.card.expiry.year % YearInCentury)
        dueText = formatWithSymbol(item.statement.due.value, currency)
        unbilledText = formatWithSymbol(item.statement.unbilled.value, currency)
        outstandingText = formatWithSymbol(item.statement.outstanding, currency)
        availableText = formatWithSymbol(item.statement.availableLimit, currency)
        limitText = formatWithSymbol(item.card.creditLimit.value, currency)
        usedFraction = (item.statement.outstanding / item.card.creditLimit.value).toFloat().coerceIn(0f, 1f)
        statementDateText = item.statement.dates.lastStatementDate.text()
        dueDateText = item.statement.dates.dueDate.text()
        nextStatementDateText = item.statement.dates.nextStatementDate.text()
        lastPaidOnText = item.statement.lastPaidOn?.text()
        repaymentAccountName = item.card.repaymentAccountId?.let { accountRepository.findById(it)?.name?.value }
        payments = paymentsUseCase.history(id)
            .map { paymentUiMapper.map(it, currency) }
            .toImmutableList()
        loading = false
    }

    private fun LocalDate.text(): String = format(dateFormatter)

    private suspend fun toggleReveal() {
        if (revealed) {
            hideSecrets()
            return
        }
        val id = cardId ?: return
        secretsRepository.get(id).fold(
            ifLeft = { error -> secretsMissing = error is CardSecretsError.Missing },
            ifRight = { secrets ->
                fullNumberText = secrets.pan?.value?.chunked(PanGroupSize)?.joinToString(" ")
                cvvText = secrets.cvv?.value
                revealed = true
                revealJob?.cancel()
                revealJob = viewModelScope.launch {
                    delay(RevealTimeoutMs)
                    hideSecrets()
                }
            },
        )
    }

    private fun hideSecrets() {
        revealJob?.cancel()
        revealed = false
        fullNumberText = null
        cvvText = null
    }

    private suspend fun delete() {
        val id = cardId ?: return
        deleteDialogVisible = false
        deleteCreditCardUseCase.delete(id)
        nav.back()
    }
}
