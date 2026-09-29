package com.ivy.creditcards.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.creditcards.DeleteCardDialog
import com.ivy.creditcards.ui.CreditCardFace
import com.ivy.creditcards.ui.text
import com.ivy.navigation.CreditCardDetailsScreen
import com.ivy.navigation.EditCreditCardScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.component.BackButton
import kotlinx.collections.immutable.ImmutableList

@Composable
fun CreditCardDetailsScreenImpl(
    screen: CreditCardDetailsScreen,
    viewModel: CreditCardDetailsViewModel = screenScopedViewModel(),
) {
    LaunchedEffect(screen.cardId) {
        viewModel.onEvent(CreditCardDetailsUiEvent.Load(screen.cardId))
    }
    CreditCardDetailsUi(
        state = viewModel.uiState(),
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardDetailsUi(
    state: CreditCardDetailsUiState,
    onEvent: (CreditCardDetailsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    val card = state.card
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(card?.name ?: stringResource(R.string.credit_card_details)) },
                navigationIcon = { BackButton(onClick = { nav.back() }) },
                actions = {
                    if (card != null) {
                        IconButton(onClick = { nav.navigateTo(EditCreditCardScreen(cardId = card.id.value)) }) {
                            Icon(painter = painterResource(R.drawable.ic_edit), contentDescription = "edit")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (card == null) return@Scaffold
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { CreditCardFace(card = card) }
            item {
                RevealSection(
                    state = state,
                    onToggle = { onEvent(CreditCardDetailsUiEvent.ToggleReveal) },
                )
            }
            item { StatementCard(state = state) }
            item { RepaymentsSection(payments = state.payments) }
            item {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    onClick = { onEvent(CreditCardDetailsUiEvent.DeleteClick) },
                ) {
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
    if (state.deleteDialogVisible && card != null) {
        DeleteCardDialog(
            cardName = card.name,
            onConfirm = { onEvent(CreditCardDetailsUiEvent.DeleteConfirm) },
            onDismiss = { onEvent(CreditCardDetailsUiEvent.DeleteDismiss) },
        )
    }
}

@Composable
private fun RevealSection(
    state: CreditCardDetailsUiState,
    onToggle: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.fullNumberText
                            ?: stringResource(R.string.card_ending_in, state.card?.last4.orEmpty()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${stringResource(R.string.expiry_mm_yy)}: ${state.expiryText}" +
                            (state.cvvText?.let { "   ${stringResource(R.string.cvv)}: $it" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                TextButton(onClick = onToggle) {
                    Text(stringResource(if (state.revealed) R.string.hide_card_details else R.string.show_card_details))
                }
            }
            if (state.secretsMissing) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.card_secrets_missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun StatementCard(
    state: CreditCardDetailsUiState,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.card?.let { Text(text = it.statement.text(), style = MaterialTheme.typography.titleMedium) }
            InfoRow(label = R.string.current_due, value = state.dueText)
            InfoRow(label = R.string.unbilled, value = state.unbilledText)
            InfoRow(label = R.string.total_outstanding, value = state.outstandingText)
            InfoRow(label = R.string.available_limit, value = state.availableText)
            InfoRow(label = R.string.credit_limit, value = state.limitText)
            InfoRow(label = R.string.statement_date, value = state.statementDateText)
            InfoRow(label = R.string.due_date, value = state.dueDateText)
            InfoRow(label = R.string.next_statement_date, value = state.nextStatementDateText)
            state.lastPaidOnText?.let { InfoRow(label = R.string.last_paid_on, value = it) }
            state.repaymentAccountName?.let { InfoRow(label = R.string.repayment_account, value = it) }
            state.card?.payeeVpa?.let { InfoRow(label = R.string.upi_payee_id, value = it) }
        }
    }
}

@Composable
private fun InfoRow(
    label: Int,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(label),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RepaymentsSection(
    payments: ImmutableList<PaymentUi>,
) {
    Column {
        Text(text = stringResource(R.string.repayments), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (payments.isEmpty()) {
            Text(text = stringResource(R.string.no_payments_yet), style = MaterialTheme.typography.bodyMedium)
        }
        payments.forEach { payment ->
            PaymentRow(payment = payment)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PaymentRow(
    payment: PaymentUi,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                val subtitle = listOfNotNull(payment.fromAccountName, payment.note).joinToString(" · ")
                if (subtitle.isNotEmpty()) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(text = payment.amountText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}
