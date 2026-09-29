package com.ivy.accounts

import com.ivy.creditcards.session.AccountsSegment

sealed interface AccountsEvent {
    data class OnReorder(val reorderedList: List<com.ivy.legacy.data.model.AccountData>) :
        AccountsEvent
    data class OnReorderModalVisible(val reorderVisible: Boolean) : AccountsEvent
    data class OnSegmentSelected(val segment: AccountsSegment) : AccountsEvent
}
