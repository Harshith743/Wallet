package com.ivy.creditcards.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ivy.data.model.AccountId
import javax.inject.Inject
import javax.inject.Singleton

enum class AccountsSegment {
    ACCOUNTS,
    CREDIT_CARDS,
}

/**
 * Session-only (in-memory) state of the Accounts tab: which segment is open and which
 * card is active. Lives in a singleton because ViewModels on the Main screen are
 * recreated whenever a non-legacy screen is entered. Resets on process death.
 */
@Singleton
class AccountsSegmentSession @Inject constructor() {
    var segment: AccountsSegment by mutableStateOf(AccountsSegment.ACCOUNTS)
    var activeCreditCardId: AccountId? by mutableStateOf(null)
}
