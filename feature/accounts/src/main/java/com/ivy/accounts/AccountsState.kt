package com.ivy.accounts

import androidx.compose.runtime.Immutable
import com.ivy.creditcards.session.AccountsSegment
import com.ivy.legacy.data.model.AccountData
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class AccountsState(
    val segment: AccountsSegment,
    val baseCurrency: String,
    val currencySymbol: String,
    val accountsData: ImmutableList<AccountData>,
    val totalBalanceWithExcluded: String,
    val totalBalanceWithExcludedText: String,
    val totalBalanceWithoutExcluded: String,
    val totalBalanceWithoutExcludedText: String,
    val totalBalanceWithoutExcludedFormatted: String,
    val totalBalanceWithExcludedFormatted: String,
    val reorderVisible: Boolean,
    val compactAccountsModeEnabled: Boolean,
    val hideTotalBalance: Boolean,
)
