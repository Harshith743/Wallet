package com.ivy.creditcards

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ivy.creditcards.pay.PaySheet
import com.ivy.creditcards.pay.launchUpiPayment
import com.ivy.creditcards.ui.CardStack
import com.ivy.creditcards.ui.CardStackCallbacks
import com.ivy.creditcards.ui.CreditCardsEmptyState
import com.ivy.creditcards.ui.QuickActionCallbacks
import com.ivy.data.model.AccountId
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.modal.AddModalBackHandling
import java.util.UUID

/**
 * Navigation hooks the host (the Accounts tab) provides; the segment itself never
 * navigates so it stays previewable.
 */
data class CreditCardsNavigation(
    val onAddCard: () -> Unit,
    val onViewDetails: (AccountId) -> Unit,
    val onEditCard: (AccountId) -> Unit,
    val onRecentSpends: (AccountId) -> Unit,
    val onPaymentHistory: (AccountId) -> Unit,
)

/**
 * The Credit Cards segment of the Accounts tab: empty state, or the card stack whose
 * cards reveal a quick-action grid when swiped left.
 */
@Composable
fun CreditCardsContent(
    state: CreditCardsUiState,
    onEvent: (CreditCardsUiEvent) -> Unit,
    navigation: CreditCardsNavigation,
    modifier: Modifier = Modifier,
) {
    when {
        state.loading && state.cards.isEmpty() -> Unit
        state.cards.isEmpty() -> CreditCardsEmptyState(onAddCard = navigation.onAddCard, modifier = modifier)
        else -> CardStack(
            cards = state.cards,
            activeCardId = state.activeCardId,
            expanded = state.expanded,
            revealedCardId = state.revealedCardId,
            callbacks = CardStackCallbacks(
                onSelectCard = { onEvent(CreditCardsUiEvent.SelectCard(it)) },
                onToggleExpanded = { onEvent(CreditCardsUiEvent.ToggleExpanded) },
                onExpand = { onEvent(CreditCardsUiEvent.ExpandStack) },
                onPayNow = { onEvent(CreditCardsUiEvent.PayNowClick(it.id)) },
                onViewDetails = navigation.onViewDetails,
                onReveal = { onEvent(CreditCardsUiEvent.Reveal(it)) },
                onCloseReveal = { onEvent(CreditCardsUiEvent.CloseReveal(it)) },
            ),
            quickActions = quickActionCallbacks(onEvent, navigation),
            modifier = modifier,
        )
    }
}

/** Every quick action closes the reveal first so navigation returns to a closed stack. */
private fun quickActionCallbacks(
    onEvent: (CreditCardsUiEvent) -> Unit,
    navigation: CreditCardsNavigation,
): QuickActionCallbacks {
    fun closeThen(action: (AccountId) -> Unit): (AccountId) -> Unit = { id ->
        onEvent(CreditCardsUiEvent.CloseReveal(id))
        action(id)
    }
    return QuickActionCallbacks(
        onMarkAsPaid = closeThen { onEvent(CreditCardsUiEvent.MarkAsPaidClick(it)) },
        onPaymentHistory = closeThen(navigation.onPaymentHistory),
        onRecentSpends = closeThen(navigation.onRecentSpends),
        onViewDetails = closeThen(navigation.onViewDetails),
        onEdit = closeThen(navigation.onEditCard),
        onDelete = closeThen { onEvent(CreditCardsUiEvent.DeleteClick(it)) },
    )
}

/**
 * Closes the revealed card when the user taps anywhere else in the list. Children's own
 * clickables consume their taps first, and a scroll cancels the tap.
 */
fun Modifier.closeRevealOnTapOutside(
    enabled: Boolean,
    onClose: () -> Unit,
): Modifier = if (enabled) pointerInput(onClose) { detectTapGestures { onClose() } } else this

/**
 * Sheets, dialogs and the reorder modal of the segment (rendered outside the scrolling list),
 * the back-press handling for a revealed card, and the one-shot UPI launch after "Pay now".
 */
@Composable
fun BoxScope.CreditCardsOverlays(
    state: CreditCardsUiState,
    onEvent: (CreditCardsUiEvent) -> Unit,
) {
    val currentOnEvent by rememberUpdatedState(onEvent)
    val revealBackId = remember { UUID.randomUUID() }
    AddModalBackHandling(modalId = revealBackId, visible = state.revealedCardId != null) {
        currentOnEvent(CreditCardsUiEvent.CloseReveal(null))
    }
    state.paySheet?.let { sheet ->
        PaySheet(sheet = sheet, onEvent = onEvent)
    }
    state.deleteConfirmCardId?.let { id ->
        val card = state.cards.firstOrNull { it.id == id }
        DeleteCardDialog(
            cardName = card?.name.orEmpty(),
            onConfirm = { onEvent(CreditCardsUiEvent.DeleteConfirm) },
            onDismiss = { onEvent(CreditCardsUiEvent.DeleteDismiss) },
        )
    }
    CreditCardsReorderModal(visible = state.reorderVisible, cards = state.cards, onEvent = onEvent)
    val context = LocalContext.current
    LaunchedEffect(state.pendingUpi) {
        state.pendingUpi?.let { request ->
            context.launchUpiPayment(request)
            currentOnEvent(CreditCardsUiEvent.UpiLaunched)
        }
    }
}

@Composable
fun DeleteCardDialog(
    cardName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_credit_card_title)) },
        text = { Text(stringResource(R.string.delete_credit_card_description, cardName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
