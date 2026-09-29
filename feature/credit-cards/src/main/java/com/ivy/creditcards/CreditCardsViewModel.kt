package com.ivy.creditcards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.threading.DispatchersProvider
import com.ivy.creditcards.model.AccountChipUi
import com.ivy.creditcards.model.CreditCardUi
import com.ivy.creditcards.model.StatementLabelMapper
import com.ivy.creditcards.model.formatWithSymbol
import com.ivy.creditcards.pay.UpiPaymentRequest
import com.ivy.creditcards.session.AccountsSegmentSession
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.domain.model.CreditCardWithStatement
import com.ivy.domain.usecase.creditcard.CreditCardsOverviewUseCase
import com.ivy.domain.usecase.creditcard.DeleteCreditCardUseCase
import com.ivy.domain.usecase.creditcard.RecordCreditCardPaymentUseCase
import com.ivy.legacy.utils.amountToDoubleOrNull
import com.ivy.legacy.utils.format
import com.ivy.ui.ComposeViewModel
import com.ivy.ui.money.currencySymbol
import com.ivy.wallet.ui.theme.toComposeColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@HiltViewModel
class CreditCardsViewModel @Inject constructor(
    private val overviewUseCase: CreditCardsOverviewUseCase,
    private val recordPaymentUseCase: RecordCreditCardPaymentUseCase,
    private val deleteCreditCardUseCase: DeleteCreditCardUseCase,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val statementLabelMapper: StatementLabelMapper,
    private val session: AccountsSegmentSession,
    private val dataObserver: DataObserver,
    private val dispatchers: DispatchersProvider,
) : ComposeViewModel<CreditCardsUiState, CreditCardsUiEvent>() {

    private var baseCurrency by mutableStateOf("")
    private var cards by mutableStateOf<ImmutableList<CreditCardUi>>(persistentListOf())
    private var activeCardId by mutableStateOf<AccountId?>(null)
    private var expanded by mutableStateOf(false)
    private var totalDueText by mutableStateOf("")
    private var dueCardsCount by mutableIntStateOf(0)
    private var paySheet by mutableStateOf<PaySheetUi?>(null)
    private var deleteConfirmCardId by mutableStateOf<AccountId?>(null)
    private var pendingUpi by mutableStateOf<UpiPaymentRequest?>(null)
    private var loading by mutableStateOf(true)

    init {
        viewModelScope.launch {
            dataObserver.writeEvents.collectLatest { event ->
                when (event) {
                    is DataWriteEvent.CreditCardChange, is DataWriteEvent.AccountChange -> load()
                    else -> {
                        // not relevant
                    }
                }
            }
        }
    }

    @Composable
    override fun uiState(): CreditCardsUiState {
        LaunchedEffect(Unit) {
            load()
        }
        return CreditCardsUiState(
            cards = cards,
            activeCardId = activeCardId,
            expanded = expanded,
            totalDueText = totalDueText,
            dueCardsCount = dueCardsCount,
            paySheet = paySheet,
            deleteConfirmCardId = deleteConfirmCardId,
            pendingUpi = pendingUpi,
            loading = loading,
        )
    }

    override fun onEvent(event: CreditCardsUiEvent) {
        viewModelScope.launch(dispatchers.default) {
            when (event) {
                is CreditCardsUiEvent.SelectCard -> selectCard(event.id)
                CreditCardsUiEvent.ToggleExpanded -> expanded = !expanded
                is CreditCardsUiEvent.PayNowClick -> openPaySheet(event.id, launchUpi = true)
                is CreditCardsUiEvent.MarkAsPaidClick -> openPaySheet(event.id, launchUpi = false)
                is CreditCardsUiEvent.PaySheetAmountChange -> updateSheet {
                    copy(amountText = event.text, amountError = false)
                }

                is CreditCardsUiEvent.PaySheetAccountSelect -> updateSheet {
                    copy(selectedAccountId = event.id, accountError = false)
                }
                is CreditCardsUiEvent.PaySheetNoteChange -> updateSheet { copy(note = event.note) }
                CreditCardsUiEvent.PaySheetConfirm -> confirmPayment()
                CreditCardsUiEvent.PaySheetDismiss -> paySheet = null
                is CreditCardsUiEvent.DeleteClick -> deleteConfirmCardId = event.id
                CreditCardsUiEvent.DeleteConfirm -> confirmDelete()
                CreditCardsUiEvent.DeleteDismiss -> deleteConfirmCardId = null
                CreditCardsUiEvent.UpiLaunched -> pendingUpi = null
                CreditCardsUiEvent.Refresh -> load()
            }
        }
    }

    private suspend fun load() {
        baseCurrency = currencyRepository.getBaseCurrency().code
        val overview = overviewUseCase.overview()
        cards = overview.cards.map(::toUi).toImmutableList()
        totalDueText = formatWithSymbol(overview.totalDue, baseCurrency)
        dueCardsCount = overview.cardsWithDueCount
        activeCardId = session.activeCreditCardId?.takeIf { id -> cards.any { it.id == id } }
            ?: cards.firstOrNull()?.id
        loading = false
    }

    private fun toUi(item: CreditCardWithStatement): CreditCardUi = CreditCardUi(
        id = item.card.id,
        name = item.account.name.value,
        issuer = item.card.issuer?.value ?: item.account.name.value,
        network = item.card.network,
        last4 = item.card.last4.value,
        cardholderName = item.card.cardholderName?.value.orEmpty(),
        color = item.account.color.value.toComposeColor(),
        dueAmount = item.statement.due.value,
        dueText = formatWithSymbol(item.statement.due.value, baseCurrency),
        availableText = formatWithSymbol(item.statement.availableLimit, baseCurrency),
        limitText = formatWithSymbol(item.card.creditLimit.value, baseCurrency),
        statement = statementLabelMapper.map(item.statement.status),
        repaymentAccountId = item.card.repaymentAccountId,
        payeeVpa = item.card.payeeVpa?.value,
    )

    private fun selectCard(id: AccountId) {
        activeCardId = id
        session.activeCreditCardId = id
        expanded = false
    }

    private suspend fun openPaySheet(cardId: AccountId, launchUpi: Boolean) {
        val card = cards.firstOrNull { it.id == cardId } ?: return
        val cardIds = cards.map { it.id }.toSet()
        val accounts = accountRepository.findAll()
            .filter { it.id !in cardIds }
            .map { AccountChipUi(id = it.id, name = it.name.value, color = it.color.value.toComposeColor()) }
        paySheet = PaySheetUi(
            cardId = cardId,
            cardName = card.name,
            payeeVpa = card.payeeVpa,
            currencySymbol = currencySymbol(baseCurrency),
            amountText = card.dueAmount.format(baseCurrency),
            launchUpi = launchUpi,
            accounts = accounts.toImmutableList(),
            selectedAccountId = card.repaymentAccountId?.takeIf { id -> accounts.any { it.id == id } },
            note = "",
            amountError = false,
            accountError = false,
        )
    }

    private fun updateSheet(transform: PaySheetUi.() -> PaySheetUi) {
        paySheet = paySheet?.transform()
    }

    private suspend fun confirmPayment() {
        val sheet = paySheet ?: return
        val amount = sheet.amountText.amountToDoubleOrNull()?.let { PositiveDouble.from(it).getOrNull() }
        val paidFrom = sheet.selectedAccountId
        if (amount == null || paidFrom == null) {
            paySheet = sheet.copy(amountError = amount == null, accountError = paidFrom == null)
            return
        }
        recordPaymentUseCase.record(
            cardId = sheet.cardId,
            amount = amount,
            paidFromAccountId = paidFrom,
            paidAt = null,
            note = sheet.note.takeIf { it.isNotBlank() },
        ).onRight {
            if (sheet.launchUpi) {
                pendingUpi = UpiPaymentRequest(
                    payeeVpa = sheet.payeeVpa,
                    payeeName = sheet.cardName,
                    amount = amount.value,
                    note = sheet.note,
                )
            }
        }
        paySheet = null
        load()
    }

    private suspend fun confirmDelete() {
        val id = deleteConfirmCardId ?: return
        deleteConfirmCardId = null
        deleteCreditCardUseCase.delete(id)
        if (session.activeCreditCardId == id) session.activeCreditCardId = null
        load()
    }
}
