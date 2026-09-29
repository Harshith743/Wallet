package com.ivy.creditcards

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ivy.creditcards.pay.PaySheet
import com.ivy.creditcards.pay.launchUpiPayment
import com.ivy.creditcards.ui.CardStack
import com.ivy.creditcards.ui.CardStackCallbacks
import com.ivy.creditcards.ui.CreditCardsEmptyState
import com.ivy.creditcards.ui.QuickActionCallbacks
import com.ivy.creditcards.ui.QuickActionRow
import com.ivy.data.model.AccountId
import com.ivy.ui.R

/**
 * Navigation hooks the host (the Accounts tab) provides; the segment itself never
 * navigates so it stays previewable.
 */
data class CreditCardsNavigation(
    val onAddCard: () -> Unit,
    val onViewDetails: (AccountId) -> Unit,
    val onEditCard: (AccountId) -> Unit,
    val onRecentSpends: (AccountId) -> Unit,
)

/**
 * The Credit Cards segment of the Accounts tab: empty state, or the card stack with the
 * quick-action row under the active card.
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
            callbacks = CardStackCallbacks(
                onSelectCard = { onEvent(CreditCardsUiEvent.SelectCard(it)) },
                onToggleExpanded = { onEvent(CreditCardsUiEvent.ToggleExpanded) },
                onPayNow = { onEvent(CreditCardsUiEvent.PayNowClick(it.id)) },
                onViewDetails = navigation.onViewDetails,
            ),
            modifier = modifier,
        ) { active ->
            QuickActionRow(
                callbacks = QuickActionCallbacks(
                    onMarkAsPaid = { onEvent(CreditCardsUiEvent.MarkAsPaidClick(active.id)) },
                    onRecentSpends = { navigation.onRecentSpends(active.id) },
                    onEdit = { navigation.onEditCard(active.id) },
                    onDelete = { onEvent(CreditCardsUiEvent.DeleteClick(active.id)) },
                )
            )
        }
    }
}

/**
 * Sheets and dialogs of the segment (rendered outside the scrolling list) plus the
 * one-shot UPI launch after "Pay now".
 */
@Composable
fun CreditCardsOverlays(
    state: CreditCardsUiState,
    onEvent: (CreditCardsUiEvent) -> Unit,
) {
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
    val context = LocalContext.current
    val currentOnEvent by rememberUpdatedState(onEvent)
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
