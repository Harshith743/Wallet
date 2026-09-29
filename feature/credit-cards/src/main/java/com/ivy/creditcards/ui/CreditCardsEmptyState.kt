package com.ivy.creditcards.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.IvyButton
import com.ivy.wallet.ui.theme.components.IvyIcon

@Composable
fun CreditCardsEmptyState(
    onAddCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IvyIcon(
            modifier = Modifier.size(64.dp),
            icon = R.drawable.ic_vue_money_card,
            tint = UI.colors.gray,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_credit_cards_yet),
            style = UI.typo.b1.style(textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.no_credit_cards_hint),
            style = UI.typo.c.style(color = UI.colors.gray, textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(24.dp))
        IvyButton(
            text = stringResource(R.string.add_card),
            iconStart = R.drawable.ic_add,
            onClick = onAddCard,
        )
    }
}
