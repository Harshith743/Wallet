package com.ivy.creditcards.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.creditcards.DeleteCardDialog
import com.ivy.creditcards.model.CreditCardUi
import com.ivy.creditcards.model.PaymentUi
import com.ivy.creditcards.ui.CardFaceSecrets
import com.ivy.creditcards.ui.CreditCardFace
import com.ivy.creditcards.ui.PaymentRow
import com.ivy.creditcards.ui.isOverdue
import com.ivy.creditcards.ui.text
import com.ivy.navigation.CreditCardDetailsScreen
import com.ivy.navigation.CreditCardPaymentsScreen
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
            item {
                SecretsFace(
                    card = card,
                    state = state,
                    onToggleReveal = { onEvent(CreditCardDetailsUiEvent.ToggleReveal) },
                )
            }
            item { StatementHero(card = card, state = state) }
            item { StatTiles(state = state) }
            item {
                MoreDetails(
                    state = state,
                    onToggle = { onEvent(CreditCardDetailsUiEvent.ToggleMoreDetails) },
                )
            }
            item {
                RepaymentsSection(
                    payments = state.payments,
                    onViewAll = { nav.navigateTo(CreditCardPaymentsScreen(cardId = card.id.value)) },
                )
            }
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

private const val MaskedCvv = "XXX"

private fun maskedNumber(last4: String): String = "XXXX XXXX XXXX $last4"

/** The card face with the number, expiry and CVV on it; the eye button reveals or hides them. */
@Composable
private fun SecretsFace(
    card: CreditCardUi,
    state: CreditCardDetailsUiState,
    onToggleReveal: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    Column {
        CreditCardFace(
            card = card,
            secrets = CardFaceSecrets(
                numberText = state.fullNumberText ?: maskedNumber(card.last4),
                expiryText = state.expiryText,
                cvvText = state.cvvText ?: MaskedCvv,
                revealed = state.revealed,
                onToggleReveal = onToggleReveal,
                onCopyNumber = { state.fullNumberText?.let { clipboard.setText(AnnotatedString(it)) } },
            ),
        )
        if (state.secretsMissing) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.card_secrets_missing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private val LimitBarHeight = 6.dp
private const val LimitTrackAlpha = 0.1f
private const val ChevronDownDegrees = 180f

/** Status, the due amount and its date, then how much of the limit is in use. */
@Composable
private fun StatementHero(
    card: CreditCardUi,
    state: CreditCardDetailsUiState,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = card.statement.text().uppercase(),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                color = if (card.statement.isOverdue()) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.height(4.dp))
            Text(text = state.dueText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(R.string.current_due),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.available_of_limit, state.availableText, state.limitText),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LimitBarHeight)
                    .clip(RoundedCornerShape(LimitBarHeight)),
                progress = { state.usedFraction },
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = LimitTrackAlpha),
            )
        }
    }
}

/** The four figures worth a glance, as a 2 x 2 grid of tiles. */
@Composable
private fun StatTiles(
    state: CreditCardDetailsUiState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(label = R.string.unbilled, value = state.unbilledText)
            StatTile(label = R.string.total_outstanding, value = state.outstandingText)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(label = R.string.statement_date, value = state.statementDateText)
            StatTile(label = R.string.next_statement_date, value = state.nextStatementDateText)
        }
    }
}

@Composable
private fun RowScope.StatTile(
    label: Int,
    value: String,
) {
    Card(modifier = Modifier.weight(1f)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** The rest of the figures behind a "More details" disclosure, collapsed by default. */
@Composable
private fun MoreDetails(
    state: CreditCardDetailsUiState,
    onToggle: () -> Unit,
) {
    Column {
        TextButton(onClick = onToggle) {
            Text(stringResource(if (state.moreDetailsExpanded) R.string.less_details else R.string.more_details))
            Icon(
                modifier = Modifier.rotate(if (state.moreDetailsExpanded) ChevronDownDegrees else 0f),
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = null,
            )
        }
        AnimatedVisibility(visible = state.moreDetailsExpanded) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow(label = R.string.credit_limit, value = state.limitText)
                    InfoRow(label = R.string.due_date, value = state.dueDateText)
                    state.lastPaidOnText?.let { InfoRow(label = R.string.last_paid_on, value = it) }
                    state.repaymentAccountName?.let { InfoRow(label = R.string.repayment_account_short, value = it) }
                    state.card?.payeeVpa?.let { InfoRow(label = R.string.upi_id_short, value = it) }
                }
            }
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

private const val RecentPaymentsLimit = 3

@Composable
private fun RepaymentsSection(
    payments: ImmutableList<PaymentUi>,
    onViewAll: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.repayments),
                style = MaterialTheme.typography.titleMedium,
            )
            if (payments.isNotEmpty()) {
                TextButton(onClick = onViewAll) { Text(stringResource(R.string.view_all)) }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (payments.isEmpty()) {
            Text(text = stringResource(R.string.no_payments_yet), style = MaterialTheme.typography.bodyMedium)
        }
        payments.take(RecentPaymentsLimit).forEach { payment ->
            PaymentRow(payment = payment)
            Spacer(Modifier.height(8.dp))
        }
    }
}
