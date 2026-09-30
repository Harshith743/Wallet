package com.ivy.creditcards.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivy.creditcards.ui.PaymentRow
import com.ivy.navigation.CreditCardPaymentsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.component.BackButton

@Composable
fun CreditCardPaymentsScreenImpl(
    screen: CreditCardPaymentsScreen,
    viewModel: CreditCardPaymentsViewModel = screenScopedViewModel(),
) {
    LaunchedEffect(screen.cardId) {
        viewModel.onEvent(CreditCardPaymentsUiEvent.Load(screen.cardId))
    }
    CreditCardPaymentsUi(state = viewModel.uiState())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardPaymentsUi(
    state: CreditCardPaymentsUiState,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.payment_history))
                        state.cardName?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
                    }
                },
                navigationIcon = { BackButton(onClick = { nav.back() }) },
            )
        },
    ) { innerPadding ->
        if (!state.loading && state.payments.isEmpty()) {
            EmptyPayments(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.payments, key = { it.id.value }) { payment ->
                    PaymentRow(payment = payment)
                }
            }
        }
    }
}

@Composable
private fun EmptyPayments(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.no_payments_yet),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}
