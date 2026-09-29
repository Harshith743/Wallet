package com.ivy.creditcards.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.creditcards.DeleteCardDialog
import com.ivy.navigation.EditCreditCardScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.component.BackButton

@Composable
fun EditCreditCardScreenImpl(
    screen: EditCreditCardScreen,
    viewModel: EditCreditCardViewModel = screenScopedViewModel(),
) {
    LaunchedEffect(screen.cardId) {
        viewModel.onEvent(EditCreditCardUiEvent.Load(screen.cardId))
    }
    EditCreditCardUi(
        state = viewModel.uiState(),
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCreditCardUi(
    state: EditCreditCardUiState,
    onEvent: (EditCreditCardUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (state.isEdit) R.string.edit_credit_card else R.string.add_credit_card
                        )
                    )
                },
                navigationIcon = { BackButton(onClick = { nav.back() }) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { CardNumberSection(state = state, onEvent = onEvent) }
            item { NetworkIssuerSection(state = state, onEvent = onEvent) }
            item { NamesSection(state = state, onEvent = onEvent) }
            item { ExpiryCvvRow(state = state, onEvent = onEvent) }
            item { LimitAndDaysSection(state = state, onEvent = onEvent) }
            if (!state.isEdit) {
                item { OpeningBalanceSection(state = state, onEvent = onEvent) }
            }
            item {
                ColorSwatchRow(
                    palette = state.palette,
                    selected = state.color,
                    onSelect = { onEvent(EditCreditCardUiEvent.ColorSelect(it)) },
                )
            }
            item { RepaymentAccountSection(state = state, onEvent = onEvent) }
            item { PayeeVpaField(state = state, onEvent = onEvent) }
            item { Actions(state = state, onEvent = onEvent) }
        }
    }
    if (state.deleteDialogVisible) {
        DeleteCardDialog(
            cardName = state.cardName,
            onConfirm = { onEvent(EditCreditCardUiEvent.DeleteConfirm) },
            onDismiss = { onEvent(EditCreditCardUiEvent.DeleteDismiss) },
        )
    }
}

@Composable
private fun Actions(
    state: EditCreditCardUiState,
    onEvent: (EditCreditCardUiEvent) -> Unit,
) {
    Column {
        Spacer(Modifier.height(8.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.saving && !state.loading,
            onClick = { onEvent(EditCreditCardUiEvent.Save) },
        ) {
            Text(stringResource(R.string.save))
        }
        if (state.isEdit) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                onClick = { onEvent(EditCreditCardUiEvent.DeleteClick) },
            ) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}
