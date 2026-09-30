package com.ivy.creditcards.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ivy.creditcards.skin.CardSkinUi
import com.ivy.creditcards.skin.brush
import com.ivy.data.model.CardNetwork
import com.ivy.data.model.CardSkinMode
import com.ivy.ui.R
import kotlinx.collections.immutable.ImmutableList

private val SwatchSize = 40.dp
private val SelectedSwatchBorder = 3.dp
private val SkinPreviewHeight = 56.dp

@Composable
private fun FieldError.text(): String = when (this) {
    FieldError.REQUIRED -> stringResource(R.string.error_field_required)
    FieldError.INVALID_NUMBER -> stringResource(R.string.error_card_number_invalid)
    FieldError.INVALID_EXPIRY -> stringResource(R.string.error_expiry_invalid)
    FieldError.EXPIRED -> stringResource(R.string.error_card_expired)
    FieldError.INVALID_CVV -> stringResource(R.string.error_cvv_invalid)
    FieldError.INVALID_AMOUNT -> stringResource(R.string.error_amount_invalid)
    FieldError.DAY_RANGE -> stringResource(R.string.error_day_out_of_range)
    FieldError.SECRETS -> stringResource(R.string.error_secrets_save_failed)
}

@Composable
private fun CardTextField(
    value: String,
    label: Int,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: FieldError? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    prefix: String? = null,
) {
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        prefix = prefix?.let { { Text(it) } },
        isError = error != null,
        supportingText = error?.let { { Text(it.text()) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        singleLine = true,
    )
}

@Composable
internal fun CardNumberSection(
    state: EditCreditCardUiState,
    onEvent: (EditCreditCardUiEvent) -> Unit,
) {
    if (state.numberEntryVisible) {
        CardTextField(
            value = state.cardNumber,
            label = R.string.card_number,
            onValueChange = { onEvent(EditCreditCardUiEvent.FieldChange(CardField.NUMBER, it)) },
            error = state.errors[CardField.NUMBER],
            keyboardType = KeyboardType.Number,
            visualTransformation = CardNumberVisualTransformation(state.network),
        )
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.card_ending_in, state.last4.orEmpty()),
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = { onEvent(EditCreditCardUiEvent.ReenterNumber) }) {
                Text(stringResource(R.string.re_enter_card_number))
            }
        }
    }
}

@Composable
internal fun NetworkIssuerSection(
    state: EditCreditCardUiState,
    onEvent: (EditCreditCardUiEvent) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NetworkChip(state = state, onEvent = onEvent)
            state.detectedIssuer?.let { issuer ->
                Spacer(Modifier.width(8.dp))
                AssistChip(
                    onClick = { onEvent(EditCreditCardUiEvent.FieldChange(CardField.ISSUER, issuer)) },
                    label = {
                        val tier = state.detectedTier
                        Text(
                            if (tier == null) {
                                stringResource(R.string.detected, issuer)
                            } else {
                                stringResource(R.string.detected_with_tier, issuer, tier)
                            }
                        )
                    },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        CardTextField(
            value = state.issuer,
            label = R.string.issuer,
            onValueChange = { onEvent(EditCreditCardUiEvent.FieldChange(CardField.ISSUER, it)) },
        )
    }
}

@Composable
private fun NetworkChip(
    state: EditCreditCardUiState,
    onEvent: (EditCreditCardUiEvent) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    AssistChip(
        onClick = { menuOpen = true },
        label = { Text("${stringResource(R.string.network)}: ${state.network.name}") },
    )
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        CardNetwork.entries.forEach { network ->
            DropdownMenuItem(
                text = { Text(network.name) },
                onClick = {
                    menuOpen = false
                    onEvent(EditCreditCardUiEvent.NetworkOverride(network))
                },
            )
        }
    }
}

@Composable
internal fun NamesSection(
    state: EditCreditCardUiState,
    onEvent: (EditCreditCardUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CardTextField(
            value = state.cardName,
            label = R.string.card_name,
            onValueChange = { onEvent(EditCreditCardUiEvent.FieldChange(CardField.NAME, it)) },
            error = state.errors[CardField.NAME],
        )
        CardTextField(
            value = state.cardholderName,
            label = R.string.cardholder_name,
            onValueChange = { onEvent(EditCreditCardUiEvent.FieldChange(CardField.HOLDER, it)) },
        )
    }
}

@Composable
internal fun ExpiryCvvRow(
    state: EditCreditCardUiState,
    onEvent: (EditCreditCardUiEvent) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CardTextField(
            modifier = Modifier.weight(1f),
            value = state.expiry,
            label = R.string.expiry_mm_yy,
            onValueChange = { onEvent(EditCreditCardUiEvent.FieldChange(CardField.EXPIRY, it)) },
            error = state.errors[CardField.EXPIRY],
            keyboardType = KeyboardType.Number,
            visualTransformation = ExpiryVisualTransformation(),
        )
        if (state.numberEntryVisible) {
            CardTextField(
                modifier = Modifier.weight(1f),
                value = state.cvv,
                label = R.string.cvv,
                onValueChange = { onEvent(EditCreditCardUiEvent.FieldChange(CardField.CVV, it)) },
                error = state.errors[CardField.CVV],
                keyboardType = KeyboardType.NumberPassword,
                visualTransformation = PasswordVisualTransformation(),
            )
        }
    }
}

@Composable
internal fun LimitAndDaysSection(
    state: EditCreditCardUiState,
    onEvent: (EditCreditCardUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CardTextField(
            value = state.creditLimit,
            label = R.string.credit_limit,
            onValueChange = { onEvent(EditCreditCardUiEvent.FieldChange(CardField.LIMIT, it)) },
            error = state.errors[CardField.LIMIT],
            keyboardType = KeyboardType.Decimal,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardTextField(
                modifier = Modifier.weight(1f),
                value = state.statementDay,
                label = R.string.statement_day,
                onValueChange = { onEvent(EditCreditCardUiEvent.FieldChange(CardField.STATEMENT_DAY, it)) },
                error = state.errors[CardField.STATEMENT_DAY],
                keyboardType = KeyboardType.Number,
            )
            CardTextField(
                modifier = Modifier.weight(1f),
                value = state.dueDay,
                label = R.string.payment_due_day,
                onValueChange = { onEvent(EditCreditCardUiEvent.FieldChange(CardField.DUE_DAY, it)) },
                error = state.errors[CardField.DUE_DAY],
                keyboardType = KeyboardType.Number,
            )
        }
    }
}

@Composable
internal fun OpeningBalanceSection(
    state: EditCreditCardUiState,
    onEvent: (EditCreditCardUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.opening_balance_hint),
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardTextField(
                modifier = Modifier.weight(1f),
                value = state.openingDue,
                label = R.string.opening_due,
                onValueChange = { onEvent(EditCreditCardUiEvent.FieldChange(CardField.OPENING_DUE, it)) },
                error = state.errors[CardField.OPENING_DUE],
                keyboardType = KeyboardType.Decimal,
            )
            CardTextField(
                modifier = Modifier.weight(1f),
                value = state.openingUnbilled,
                label = R.string.opening_unbilled,
                onValueChange = { onEvent(EditCreditCardUiEvent.FieldChange(CardField.OPENING_UNBILLED, it)) },
                error = state.errors[CardField.OPENING_UNBILLED],
                keyboardType = KeyboardType.Decimal,
            )
        }
    }
}

@Composable
internal fun DesignSection(
    state: EditCreditCardUiState,
    onEvent: (EditCreditCardUiEvent) -> Unit,
) {
    Column {
        Text(text = stringResource(R.string.card_design), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.skinMode == CardSkinMode.AUTO,
                onClick = { onEvent(EditCreditCardUiEvent.SkinModeSelect(CardSkinMode.AUTO)) },
                label = { Text(stringResource(R.string.design_auto)) },
            )
            FilterChip(
                selected = state.skinMode == CardSkinMode.COLOR,
                onClick = { onEvent(EditCreditCardUiEvent.SkinModeSelect(CardSkinMode.COLOR)) },
                label = { Text(stringResource(R.string.design_colour)) },
            )
        }
        Spacer(Modifier.height(8.dp))
        if (state.skinMode == CardSkinMode.AUTO) {
            SkinPreviewStrip(skin = state.skinPreview)
            Spacer(Modifier.height(4.dp))
            Text(text = stringResource(R.string.bank_theme_hint), style = MaterialTheme.typography.bodySmall)
        } else {
            ColorSwatchRow(
                palette = state.palette,
                selected = state.color,
                onSelect = { onEvent(EditCreditCardUiEvent.ColorSelect(it)) },
            )
        }
    }
}

@Composable
private fun SkinPreviewStrip(
    skin: CardSkinUi,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SkinPreviewHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(skin.brush())
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = skin.wordmark ?: stringResource(R.string.bank_theme),
            color = skin.textColor,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun ColorSwatchRow(
    palette: ImmutableList<Color>,
    selected: Color,
    onSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(text = stringResource(R.string.card_colour), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(palette.size) { index ->
                val color = palette[index]
                val border = if (color == selected) SelectedSwatchBorder else 0.dp
                Spacer(
                    modifier = Modifier
                        .size(SwatchSize)
                        .clip(CircleShape)
                        .background(color)
                        .border(border, MaterialTheme.colorScheme.onSurface, CircleShape)
                        .clickable { onSelect(color) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RepaymentAccountSection(
    state: EditCreditCardUiState,
    onEvent: (EditCreditCardUiEvent) -> Unit,
) {
    Column {
        Text(text = stringResource(R.string.repayment_account), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.repaymentAccountId == null,
                onClick = { onEvent(EditCreditCardUiEvent.RepaymentAccountSelect(null)) },
                label = { Text(stringResource(R.string.repayment_account_none)) },
            )
            state.accounts.forEach { account ->
                FilterChip(
                    selected = state.repaymentAccountId == account.id,
                    onClick = { onEvent(EditCreditCardUiEvent.RepaymentAccountSelect(account.id)) },
                    label = { Text(account.name) },
                )
            }
        }
    }
}

@Composable
internal fun PayeeVpaField(
    state: EditCreditCardUiState,
    onEvent: (EditCreditCardUiEvent) -> Unit,
) {
    CardTextField(
        value = state.payeeVpa,
        label = R.string.upi_payee_id,
        onValueChange = { onEvent(EditCreditCardUiEvent.FieldChange(CardField.PAYEE_VPA, it)) },
        keyboardType = KeyboardType.Email,
    )
}
