package com.ivy.accounts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.base.legacy.Theme
import com.ivy.creditcards.CreditCardsContent
import com.ivy.creditcards.CreditCardsNavigation
import com.ivy.creditcards.CreditCardsOverlays
import com.ivy.creditcards.CreditCardsUiEvent
import com.ivy.creditcards.CreditCardsUiState
import com.ivy.creditcards.CreditCardsViewModel
import com.ivy.creditcards.closeRevealOnTapOutside
import com.ivy.creditcards.preview.CreditCardsPreviewData
import com.ivy.creditcards.session.AccountsSegment
import com.ivy.creditcards.ui.CarouselDefaults
import com.ivy.creditcards.ui.CreditCardsCarousel
import com.ivy.creditcards.ui.text
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.utils.thenIf
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.AccountData
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.horizontalSwipeListener
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.legacy.utils.rememberSwipeListenerState
import com.ivy.navigation.CreditCardDetailsScreen
import com.ivy.navigation.EditCreditCardScreen
import com.ivy.navigation.SettingsScreen
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.GreenLight
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.BalanceRowMini
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.components.ReorderModalSingleType
import com.ivy.wallet.ui.theme.dynamicContrast
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.toComposeColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID

@Composable
fun BoxWithConstraintsScope.AccountsTab() {
    val viewModel: AccountsViewModel = screenScopedViewModel()
    val creditCardsViewModel: CreditCardsViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()
    val creditCardsState = creditCardsViewModel.uiState()

    UI(
        state = uiState,
        creditCardsState = creditCardsState,
        onEvent = viewModel::onEvent,
        onCreditCardsEvent = creditCardsViewModel::onEvent,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoxWithConstraintsScope.UI(
    state: AccountsState,
    creditCardsState: CreditCardsUiState,
    onEvent: (AccountsEvent) -> Unit = {},
    onCreditCardsEvent: (CreditCardsUiEvent) -> Unit = {},
) {
    val nav = navigation()
    val ivyContext = com.ivy.legacy.ivyWalletCtx()
    val segment = state.segment
    // Both states are created unconditionally; only the accounts list restores its
    // position (the cards item changes height when it expands or reveals)
    val accountsListState = rememberScrollPositionListState(
        key = "accounts_lazy_column",
        initialFirstVisibleItemIndex = ivyContext.accountsListState?.firstVisibleItemIndex ?: 0,
        initialFirstVisibleItemScrollOffset = ivyContext.accountsListState?.firstVisibleItemScrollOffset
            ?: 0
    )
    val cardsListState = rememberLazyListState()
    val listState = if (segment == AccountsSegment.ACCOUNTS) accountsListState else cardsListState
    val swipeListenerState = rememberSwipeListenerState()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            // Only the Accounts segment swipes to Home; cards keep their own gestures
            .thenIf(segment == AccountsSegment.ACCOUNTS) {
                horizontalSwipeListener(
                    sensitivity = 200,
                    state = swipeListenerState,
                    onSwipeLeft = {
                        ivyContext.selectMainTab(com.ivy.legacy.data.model.MainTab.HOME)
                    },
                    onSwipeRight = {
                        ivyContext.selectMainTab(com.ivy.legacy.data.model.MainTab.HOME)
                    }
                )
            }
            .closeRevealOnTapOutside(enabled = creditCardsState.revealedCardId != null) {
                onCreditCardsEvent(CreditCardsUiEvent.CloseReveal(null))
            },
        state = listState
    ) {
        stickyHeader {
            AccountsHeaderToolbar(
                segment = segment,
                showReorder = segment == AccountsSegment.ACCOUNTS,
                dueCardsCount = creditCardsState.dueCardsCount,
                onSegmentSelect = { onEvent(AccountsEvent.OnSegmentSelected(it)) },
                onReorderClick = { onEvent(AccountsEvent.OnReorderModalVisible(reorderVisible = true)) },
                onSettingsClick = { nav.navigateTo(SettingsScreen) },
            )
        }
        item {
            HeaderSummary(
                state = state,
                creditCardsState = creditCardsState,
                onDrawerToggle = { onEvent(AccountsEvent.OnDrawerToggle) },
            )
        }
        when (segment) {
            AccountsSegment.ACCOUNTS -> items(state.accountsData) {
                Spacer(Modifier.height(16.dp))
                AccountCard(
                    baseCurrency = state.baseCurrency,
                    accountData = it,
                    compactModeEnabled = state.compactAccountsModeEnabled,
                    onBalanceClick = {
                        nav.navigateTo(
                            TransactionsScreen(
                                accountId = it.account.id.value,
                                categoryId = null
                            )
                        )
                    }
                ) {
                    nav.navigateTo(
                        TransactionsScreen(
                            accountId = it.account.id.value,
                            categoryId = null
                        )
                    )
                }
            }

            AccountsSegment.CREDIT_CARDS -> item {
                CreditCardsContent(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    state = creditCardsState,
                    onEvent = onCreditCardsEvent,
                    navigation = CreditCardsNavigation(
                        onAddCard = { nav.navigateTo(EditCreditCardScreen(cardId = null)) },
                        onViewDetails = { nav.navigateTo(CreditCardDetailsScreen(cardId = it.value)) },
                        onEditCard = { nav.navigateTo(EditCreditCardScreen(cardId = it.value)) },
                        onRecentSpends = {
                            nav.navigateTo(TransactionsScreen(accountId = it.value, categoryId = null))
                        },
                        onPaymentHistory = { nav.navigateTo(CreditCardDetailsScreen(cardId = it.value)) },
                    ),
                )
            }
        }

        item {
            // scroll hack; the cards segment also clears the carousel strip
            val bottomSpace = if (segment == AccountsSegment.CREDIT_CARDS) {
                150.dp + CarouselDefaults.StripHeight + 16.dp
            } else {
                150.dp
            }
            Spacer(Modifier.height(bottomSpace))
        }
    }

    ReorderModalSingleType(
        visible = state.reorderVisible && segment == AccountsSegment.ACCOUNTS,
        initialItems = state.accountsData,
        dismiss = {
            onEvent(AccountsEvent.OnReorderModalVisible(reorderVisible = false))
        },
        onReordered = {
            onEvent(AccountsEvent.OnReorder(reorderedList = it))
        }
    ) { _, item ->
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp)
                .padding(vertical = 8.dp),
            text = item.account.name.value,
            style = UI.typo.b1.style(
                color = item.account.color.value.toComposeColor(),
                fontWeight = FontWeight.Bold
            )
        )
    }

    if (segment == AccountsSegment.CREDIT_CARDS && creditCardsState.cards.isNotEmpty()) {
        CreditCardsCarousel(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = CarouselDefaults.BottomClearance),
            cards = creditCardsState.cards,
            activeCardId = creditCardsState.activeCardId,
            onSelectCard = { onCreditCardsEvent(CreditCardsUiEvent.SelectCard(it)) },
            onAddCard = { nav.navigateTo(EditCreditCardScreen(cardId = null)) },
        )
    }

    CreditCardsOverlays(state = creditCardsState, onEvent = onCreditCardsEvent)
}

@Composable
private fun HeaderSummary(
    state: AccountsState,
    creditCardsState: CreditCardsUiState,
    onDrawerToggle: () -> Unit,
) {
    val excludedTotal = state.currencySymbol + state.totalBalanceWithExcludedFormatted
    Column {
        Spacer(Modifier.height(16.dp))
        when (state.segment) {
            AccountsSegment.ACCOUNTS -> if (!state.hideTotalBalance) {
                AccountsHeaderSummary(
                    caption = stringResource(R.string.total_balance),
                    currencySymbol = state.currencySymbol,
                    amountText = state.totalBalanceWithoutExcludedFormatted,
                    secondaryLine = "${stringResource(R.string.total_balance_excluded)}: $excludedTotal",
                    drawerExpanded = state.drawerExpanded,
                    onDrawerToggle = onDrawerToggle,
                )
                BreakdownDrawer(expanded = state.drawerExpanded) {
                    state.accountRows.forEach { row ->
                        HeaderBreakdownRow(
                            title = row.name,
                            amountText = row.balanceText,
                            subtitle = if (row.excluded) stringResource(R.string.excluded) else null,
                            color = row.color,
                        )
                    }
                    HeaderBreakdownRow(
                        title = stringResource(R.string.total_balance_excluded),
                        amountText = excludedTotal,
                        emphasized = true,
                    )
                }
            }

            AccountsSegment.CREDIT_CARDS -> {
                AccountsHeaderSummary(
                    caption = if (creditCardsState.dueCardsCount > 0) {
                        pluralStringResource(
                            R.plurals.statement_due_for_cards,
                            creditCardsState.dueCardsCount,
                            creditCardsState.dueCardsCount,
                        )
                    } else {
                        stringResource(R.string.no_statement_due)
                    },
                    currencySymbol = state.currencySymbol,
                    amountText = creditCardsState.totalDueText.removePrefix(state.currencySymbol),
                    drawerExpanded = state.drawerExpanded,
                    onDrawerToggle = onDrawerToggle,
                )
                BreakdownDrawer(expanded = state.drawerExpanded) {
                    creditCardsState.cards.forEach { card ->
                        HeaderBreakdownRow(
                            title = card.name,
                            amountText = card.dueText,
                            subtitle = "•• ${card.last4} · ${card.statement.text().lowercase()}",
                            color = card.color,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private const val PreviewBalanceText = "лв1,250.00"

private fun previewAccountRows(vararg accounts: Account): ImmutableList<AccountBreakdownUi> =
    accounts.map { account ->
        AccountBreakdownUi(
            id = account.id,
            name = account.name.value,
            color = account.color.value.toComposeColor(),
            balanceText = PreviewBalanceText,
            excluded = !account.includeInBalance,
        )
    }.toImmutableList()

@Composable
private fun AccountCard(
    baseCurrency: String,
    accountData: AccountData,
    compactModeEnabled: Boolean,
    onBalanceClick: () -> Unit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(UI.shapes.r4)
            .border(2.dp, UI.colors.medium, UI.shapes.r4)
            .clickable(
                onClick = onClick
            )
    ) {
        val account = accountData.account
        val contrastColor = findContrastTextColor(account.color.value.toComposeColor())
        val currency = account.asset.code

        AccountHeader(
            accountData = accountData,
            currency = currency,
            baseCurrency = baseCurrency,
            contrastColor = contrastColor,
            onBalanceClick = onBalanceClick
        )

        if (!compactModeEnabled) {
            Spacer(Modifier.height(12.dp))

            IncomeExpensesRow(
                currency = currency,
                incomeLabel = stringResource(R.string.month_income),
                income = accountData.monthlyIncome,
                expensesLabel = stringResource(R.string.month_expenses),
                expenses = accountData.monthlyExpenses
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AccountHeader(
    accountData: AccountData,
    currency: String,
    baseCurrency: String,
    contrastColor: Color,
    onBalanceClick: () -> Unit
) {
    val account = accountData.account

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(account.color.value.toComposeColor(), UI.shapes.r4Top)
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(20.dp))

            ItemIconSDefaultIcon(
                iconName = account.icon?.id,
                defaultIcon = R.drawable.ic_custom_account_s,
                tint = contrastColor
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = account.name.value,
                style = UI.typo.b1.style(
                    color = contrastColor,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            if (!account.includeInBalance) {
                Spacer(Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.excluded),
                    style = UI.typo.c.style(
                        color = account.color.value.toComposeColor().dynamicContrast()
                    )
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        BalanceRow(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickableNoIndication(rememberInteractionSource()) {
                    onBalanceClick()
                },
            textColor = contrastColor,
            currency = currency,
            balance = accountData.balance,

            balanceFontSize = 30.sp,
            currencyFontSize = 30.sp,

            currencyUpfront = false
        )

        if (currency != baseCurrency && accountData.balanceBaseCurrency != null) {
            BalanceRowMini(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickableNoIndication(rememberInteractionSource()) {
                        onBalanceClick()
                    }
                    .testTag("baseCurrencyEquivalent"),
                textColor = account.color.value.toComposeColor().dynamicContrast(),
                currency = baseCurrency,
                balance = accountData.balanceBaseCurrency!!,
                currencyUpfront = false
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Preview
@Composable
private fun PreviewAccountsTabCompactModeDisabled(theme: Theme = Theme.LIGHT, drawerExpanded: Boolean = false) {
    IvyWalletPreview(theme = theme) {
        val acc1 = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Phyre"),
            color = ColorInt(Green.toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = null,
            includeInBalance = true,
            orderNum = 0.0,
        )

        val acc2 = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("DSK"),
            color = ColorInt(GreenLight.toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = null,
            includeInBalance = true,
            orderNum = 0.0,
        )

        val acc3 = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Revolut"),
            color = ColorInt(Green.toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = IconAsset.unsafe("revolut"),
            includeInBalance = true,
            orderNum = 0.0,
        )

        val acc4 = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Cash"),
            color = ColorInt(Green.toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = IconAsset.unsafe("cash"),
            includeInBalance = true,
            orderNum = 0.0,
        )
        val state = AccountsState(
            segment = AccountsSegment.ACCOUNTS,
            baseCurrency = "BGN",
            currencySymbol = "лв",
            accountRows = previewAccountRows(acc1, acc2, acc3, acc4),
            drawerExpanded = drawerExpanded,
            accountsData = persistentListOf(
                AccountData(
                    account = acc1,
                    balance = 2125.0,
                    balanceBaseCurrency = null,
                    monthlyExpenses = 920.0,
                    monthlyIncome = 3045.0
                ),
                AccountData(
                    account = acc2,
                    balance = 12125.21,
                    balanceBaseCurrency = null,
                    monthlyExpenses = 1350.50,
                    monthlyIncome = 8000.48
                ),
                AccountData(
                    account = acc3,
                    balance = 1200.0,
                    balanceBaseCurrency = 1979.64,
                    monthlyExpenses = 750.0,
                    monthlyIncome = 1000.30
                ),
                AccountData(
                    account = acc4,
                    balance = 820.0,
                    balanceBaseCurrency = null,
                    monthlyExpenses = 340.0,
                    monthlyIncome = 400.0
                ),
            ),
            totalBalanceWithExcluded = "25.54",
            totalBalanceWithExcludedText = "BGN 25.54",
            totalBalanceWithoutExcluded = "25.54",
            totalBalanceWithoutExcludedText = "BGN 25.54",
            totalBalanceWithoutExcludedFormatted = "16,270.21",
            totalBalanceWithExcludedFormatted = "16,270.21",
            reorderVisible = false,
            compactAccountsModeEnabled = false,
            hideTotalBalance = false
        )
        UI(state = state, creditCardsState = CreditCardsPreviewData.cardsState(persistentListOf()))
    }
}

@Preview
@Composable
private fun PreviewAccountsTabCompactModeEnabled(theme: Theme = Theme.LIGHT, drawerExpanded: Boolean = false) {
    IvyWalletPreview(theme = theme) {
        val acc1 = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Phyre"),
            color = ColorInt(Green.toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = null,
            includeInBalance = true,
            orderNum = 0.0,
        )

        val acc2 = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("DSK"),
            color = ColorInt(GreenLight.toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = null,
            includeInBalance = true,
            orderNum = 0.0,
        )

        val acc3 = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Revolut"),
            color = ColorInt(Green.toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = IconAsset.unsafe("revolut"),
            includeInBalance = true,
            orderNum = 0.0,
        )

        val acc4 = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Cash"),
            color = ColorInt(Green.toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = IconAsset.unsafe("cash"),
            includeInBalance = true,
            orderNum = 0.0,
        )
        val state = AccountsState(
            segment = AccountsSegment.ACCOUNTS,
            baseCurrency = "BGN",
            currencySymbol = "лв",
            accountRows = previewAccountRows(acc1, acc2, acc3, acc4),
            drawerExpanded = drawerExpanded,
            accountsData = persistentListOf(
                AccountData(
                    account = acc1,
                    balance = 2125.0,
                    balanceBaseCurrency = null,
                    monthlyExpenses = 920.0,
                    monthlyIncome = 3045.0
                ),
                AccountData(
                    account = acc2,
                    balance = 12125.21,
                    balanceBaseCurrency = null,
                    monthlyExpenses = 1350.50,
                    monthlyIncome = 8000.48
                ),
                AccountData(
                    account = acc3,
                    balance = 1200.0,
                    balanceBaseCurrency = 1979.64,
                    monthlyExpenses = 750.0,
                    monthlyIncome = 1000.30
                ),
                AccountData(
                    account = acc4,
                    balance = 820.0,
                    balanceBaseCurrency = null,
                    monthlyExpenses = 340.0,
                    monthlyIncome = 400.0
                ),
            ),
            totalBalanceWithExcluded = "25.54",
            totalBalanceWithExcludedText = "BGN 25.54",
            totalBalanceWithoutExcluded = "25.54",
            totalBalanceWithoutExcludedText = "BGN 25.54",
            totalBalanceWithoutExcludedFormatted = "16,270.21",
            totalBalanceWithExcludedFormatted = "16,270.21",
            reorderVisible = false,
            compactAccountsModeEnabled = true,
            hideTotalBalance = false
        )
        UI(state = state, creditCardsState = CreditCardsPreviewData.cardsState(persistentListOf()))
    }
}

/** For screen shot testing **/
@Composable
fun AccountsTabNonCompactUITest(dark: Boolean, drawerExpanded: Boolean = false) {
    val theme = when (dark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    PreviewAccountsTabCompactModeDisabled(theme, drawerExpanded)
}

/** For screen shot testing **/
@Composable
fun AccountsTabCompactUITest(dark: Boolean) {
    val theme = when (dark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    PreviewAccountsTabCompactModeEnabled(theme)
}

@Preview
@Composable
private fun PreviewAccountsTabCreditCards(
    theme: Theme = Theme.LIGHT,
    empty: Boolean = false,
    drawerExpanded: Boolean = false,
) {
    IvyWalletPreview(theme = theme) {
        val state = AccountsState(
            segment = AccountsSegment.CREDIT_CARDS,
            baseCurrency = "INR",
            currencySymbol = "₹",
            accountsData = persistentListOf(),
            accountRows = persistentListOf(),
            drawerExpanded = drawerExpanded,
            totalBalanceWithExcluded = "0.0",
            totalBalanceWithExcludedText = "",
            totalBalanceWithoutExcluded = "0.0",
            totalBalanceWithoutExcludedText = "",
            totalBalanceWithoutExcludedFormatted = "0.00",
            totalBalanceWithExcludedFormatted = "0.00",
            reorderVisible = false,
            compactAccountsModeEnabled = false,
            hideTotalBalance = false
        )
        UI(
            state = state,
            creditCardsState = CreditCardsPreviewData.cardsState(
                cards = if (empty) persistentListOf() else CreditCardsPreviewData.cards,
            ),
        )
    }
}

/** For screen shot testing **/
@Composable
fun AccountsTabCreditCardsUITest(dark: Boolean, empty: Boolean = false, drawerExpanded: Boolean = false) {
    val theme = when (dark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    PreviewAccountsTabCreditCards(theme = theme, empty = empty, drawerExpanded = drawerExpanded)
}