package com.ivy.accounts

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.creditcards.session.AccountsSegment
import com.ivy.creditcards.session.AccountsSegmentSession
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.repository.AccountRepository
import com.ivy.domain.features.Features
import com.ivy.domain.usecase.creditcard.CreditCardsOverviewUseCase
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.data.model.AccountData
import com.ivy.legacy.data.model.toCloseTimeRange
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.ioThread
import com.ivy.ui.ComposeViewModel
import com.ivy.ui.R
import com.ivy.ui.money.currencySymbol
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.action.viewmodel.account.AccountDataAct
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@SuppressLint("StaticFieldLeak")
@HiltViewModel
class AccountsViewModel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val ivyContext: IvyWalletCtx,
    private val sharedPrefs: SharedPrefs,
    private val baseCurrencyAct: BaseCurrencyAct,
    private val accountDataAct: AccountDataAct,
    private val accountRepository: AccountRepository,
    private val creditCardsOverviewUseCase: CreditCardsOverviewUseCase,
    private val segmentSession: AccountsSegmentSession,
    private val dataObserver: DataObserver,
    private val features: Features,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
) : ComposeViewModel<AccountsState, AccountsEvent>() {
    private var baseCurrency by mutableStateOf("")
    private var currencySymbolText by mutableStateOf("")
    private var accountsData by mutableStateOf(listOf<AccountData>())
    private var totalBalanceWithExcluded by mutableStateOf("")
    private var totalBalanceWithExcludedText by mutableStateOf("")
    private var totalBalanceWithoutExcluded by mutableStateOf("")
    private var totalBalanceWithoutExcludedText by mutableStateOf("")
    private var totalBalanceWithoutExcludedFormatted by mutableStateOf("")
    private var totalBalanceWithExcludedFormatted by mutableStateOf("")
    private var reorderVisible by mutableStateOf(false)

    init {
        viewModelScope.launch {
            dataObserver.writeEvents.collectLatest { event ->
                when (event) {
                    // Cards own an account row, so a card change can change which
                    // accounts belong in this list
                    is DataWriteEvent.AccountChange, is DataWriteEvent.CreditCardChange -> onStart()
                    else -> {
                        // do nothing
                    }
                }
            }
        }
    }

    @Composable
    override fun uiState(): AccountsState {
        LaunchedEffect(Unit) {
            onStart()
        }

        return AccountsState(
            segment = getSegment(),
            baseCurrency = getBaseCurrency(),
            currencySymbol = getCurrencySymbol(),
            accountsData = getAccountsData(),
            totalBalanceWithExcluded = getTotalBalanceWithExcluded(),
            totalBalanceWithExcludedText = getTotalBalanceWithExcludedText(),
            totalBalanceWithoutExcluded = getTotalBalanceWithoutExcluded(),
            totalBalanceWithoutExcludedText = getTotalBalanceWithoutExcludedText(),
            totalBalanceWithoutExcludedFormatted = getTotalBalanceWithoutExcludedFormatted(),
            totalBalanceWithExcludedFormatted = getTotalBalanceWithExcludedFormatted(),
            reorderVisible = getReorderVisible(),
            compactAccountsModeEnabled = getCompactAccountsMode(),
            hideTotalBalance = getHideTotalBalance()
        )
    }

    @Composable
    private fun getSegment(): AccountsSegment {
        return segmentSession.segment
    }

    @Composable
    private fun getHideTotalBalance(): Boolean {
        return features.hideTotalBalance.asEnabledState()
    }

    @Composable
    private fun getBaseCurrency(): String {
        return baseCurrency
    }

    @Composable
    private fun getCurrencySymbol(): String {
        return currencySymbolText
    }

    @Composable
    private fun getAccountsData(): ImmutableList<AccountData> {
        return accountsData.toImmutableList()
    }

    @Composable
    private fun getTotalBalanceWithExcluded(): String {
        return totalBalanceWithExcluded
    }

    @Composable
    private fun getTotalBalanceWithExcludedText(): String {
        return totalBalanceWithExcludedText
    }

    @Composable
    private fun getTotalBalanceWithoutExcluded(): String {
        return totalBalanceWithoutExcluded
    }

    @Composable
    private fun getTotalBalanceWithoutExcludedText(): String {
        return totalBalanceWithoutExcludedText
    }

    @Composable
    private fun getTotalBalanceWithoutExcludedFormatted(): String {
        return totalBalanceWithoutExcludedFormatted
    }

    @Composable
    private fun getTotalBalanceWithExcludedFormatted(): String {
        return totalBalanceWithExcludedFormatted
    }

    @Composable
    private fun getReorderVisible(): Boolean {
        return reorderVisible
    }

    @Composable
    private fun getCompactAccountsMode(): Boolean {
        return features.compactAccountsMode.asEnabledState()
    }

    override fun onEvent(event: AccountsEvent) {
        viewModelScope.launch(Dispatchers.Default) {
            when (event) {
                is AccountsEvent.OnReorder -> reorder(event.reorderedList)
                is AccountsEvent.OnReorderModalVisible -> reorderModalVisible(event.reorderVisible)
                is AccountsEvent.OnSegmentSelected -> segmentSession.segment = event.segment
            }
        }
    }

    private suspend fun reorder(newOrder: List<AccountData>) {
        ioThread {
            newOrder.mapIndexed { index, accountData ->
                accountRepository.save(accountData.account.copy(orderNum = index.toDouble()))
            }
        }

        startInternally()
    }

    private fun onStart() {
        viewModelScope.launch(Dispatchers.Default) {
            startInternally()
        }
    }

    private suspend fun startInternally() {
        val period = com.ivy.legacy.data.model.TimePeriod.currentMonth(
            startDayOfMonth = ivyContext.startDayOfMonth
        ) // this must be monthly
        val range = period.toRange(ivyContext.startDayOfMonth, timeConverter, timeProvider)

        val baseCurrencyCode = baseCurrencyAct(Unit)
        // Credit cards own an account row but live in their own segment
        val creditCardIds = creditCardsOverviewUseCase.creditCardIds()
        val accounts = accountRepository.findAll()
            .filter { it.id !in creditCardIds }
            .toImmutableList()

        val includeTransfersInCalc =
            sharedPrefs.getBoolean(SharedPrefs.TRANSFERS_AS_INCOME_EXPENSE, false)

        val accountsDataList = accountDataAct(
            AccountDataAct.Input(
                accounts = accounts,
                range = range.toCloseTimeRange(),
                baseCurrency = baseCurrencyCode,
                includeTransfersInCalc = includeTransfersInCalc
            )
        )

        // Same maths as CalcWalletBalanceAct (a failed exchange counts as 0), minus the cards
        fun AccountData.balanceInBase(): Double = if (account.asset.code == baseCurrencyCode) {
            balance
        } else {
            balanceBaseCurrency ?: 0.0
        }
        val totalBalanceWithExcludedAccounts = accountsDataList.sumOf { it.balanceInBase() }
        val totalBalanceWithoutExcludedAccounts = accountsDataList
            .filter { it.account.includeInBalance }
            .sumOf { it.balanceInBase() }

        baseCurrency = baseCurrencyCode
        currencySymbolText = currencySymbol(baseCurrencyCode)
        accountsData = accountsDataList
        totalBalanceWithExcluded = totalBalanceWithExcludedAccounts.toString()
        totalBalanceWithExcludedFormatted = totalBalanceWithExcludedAccounts.format(baseCurrencyCode)
        totalBalanceWithExcludedText = context.getString(
            R.string.total,
            baseCurrencyCode,
            totalBalanceWithExcludedAccounts.format(
                baseCurrencyCode
            )
        )
        totalBalanceWithoutExcluded = totalBalanceWithoutExcludedAccounts.toString()
        totalBalanceWithoutExcludedFormatted = totalBalanceWithoutExcludedAccounts.format(baseCurrencyCode)
        totalBalanceWithoutExcludedText = context.getString(
            R.string.total_exclusive,
            baseCurrencyCode,
            totalBalanceWithoutExcludedAccounts.format(
                baseCurrencyCode
            )
        )
    }

    private fun reorderModalVisible(visible: Boolean) {
        reorderVisible = visible
    }
}
