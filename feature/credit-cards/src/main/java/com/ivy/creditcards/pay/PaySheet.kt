package com.ivy.creditcards.pay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ivy.creditcards.CreditCardsUiEvent
import com.ivy.creditcards.PaySheetUi
import com.ivy.creditcards.model.AccountChipUi
import com.ivy.ui.R
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaySheet(
    sheet: PaySheetUi,
    onEvent: (CreditCardsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = { onEvent(CreditCardsUiEvent.PaySheetDismiss) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(if (sheet.launchUpi) R.string.pay_now else R.string.mark_as_paid),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(text = sheet.cardName, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            AmountField(sheet = sheet, onEvent = onEvent)
            Spacer(Modifier.height(16.dp))
            PaidFromChips(
                accounts = sheet.accounts,
                selectedId = sheet.selectedAccountId,
                error = sheet.accountError,
                onSelect = { onEvent(CreditCardsUiEvent.PaySheetAccountSelect(it.id)) },
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = sheet.note,
                onValueChange = { onEvent(CreditCardsUiEvent.PaySheetNoteChange(it)) },
                label = { Text(stringResource(R.string.payment_note)) },
                singleLine = true,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onEvent(CreditCardsUiEvent.PaySheetConfirm) },
            ) {
                Text(stringResource(if (sheet.launchUpi) R.string.pay_now else R.string.record_payment))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AmountField(
    sheet: PaySheetUi,
    onEvent: (CreditCardsUiEvent) -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = sheet.amountText,
        onValueChange = { onEvent(CreditCardsUiEvent.PaySheetAmountChange(it)) },
        label = { Text(stringResource(R.string.payment_amount)) },
        prefix = { Text(sheet.currencySymbol) },
        isError = sheet.amountError,
        supportingText = if (sheet.amountError) {
            { Text(stringResource(R.string.error_amount_invalid)) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaidFromChips(
    accounts: ImmutableList<AccountChipUi>,
    selectedId: com.ivy.data.model.AccountId?,
    error: Boolean,
    onSelect: (AccountChipUi) -> Unit,
) {
    Column {
        Text(text = stringResource(R.string.paid_from), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            accounts.forEach { account ->
                FilterChip(
                    selected = account.id == selectedId,
                    onClick = { onSelect(account) },
                    label = { Text(account.name) },
                )
            }
        }
        if (error) {
            Text(
                text = stringResource(R.string.error_paid_from_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
