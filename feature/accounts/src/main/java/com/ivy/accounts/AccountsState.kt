@file:Suppress("DataClassTypedIDs") // the rule does not recognise the app's typed ids

package com.ivy.accounts

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.ivy.creditcards.session.AccountsSegment
import com.ivy.data.model.AccountId
import com.ivy.legacy.data.model.AccountData
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class AccountsState(
    val segment: AccountsSegment,
    val baseCurrency: String,
    val currencySymbol: String,
    val accountsData: ImmutableList<AccountData>,
    val accountRows: ImmutableList<AccountBreakdownUi>,
    val totalBalanceWithExcluded: String,
    val totalBalanceWithExcludedText: String,
    val totalBalanceWithoutExcluded: String,
    val totalBalanceWithoutExcludedText: String,
    val totalBalanceWithoutExcludedFormatted: String,
    val totalBalanceWithExcludedFormatted: String,
    val drawerExpanded: Boolean,
    val reorderVisible: Boolean,
    val compactAccountsModeEnabled: Boolean,
    val hideTotalBalance: Boolean,
)

/** One account in the header breakdown drawer. */
@Immutable
data class AccountBreakdownUi(
    val id: AccountId,
    val name: String,
    val color: Color,
    val balanceText: String,
    val excluded: Boolean,
)
