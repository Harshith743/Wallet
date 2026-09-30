package com.ivy.creditcards.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.data.model.AccountId
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.CircleButtonFilled

data class QuickActionCallbacks(
    val onMarkAsPaid: (AccountId) -> Unit,
    val onPaymentHistory: (AccountId) -> Unit,
    val onRecentSpends: (AccountId) -> Unit,
    val onViewDetails: (AccountId) -> Unit,
    val onEdit: (AccountId) -> Unit,
    val onDelete: (AccountId) -> Unit,
)

private val CellWidth = 56.dp

/**
 * The 2 x 3 grid of circular actions revealed behind a swiped card.
 */
@Composable
fun QuickActionGrid(
    cardId: AccountId,
    callbacks: QuickActionCallbacks,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            QuickAction(R.drawable.ic_check, R.string.mark_as_paid) { callbacks.onMarkAsPaid(cardId) }
            QuickAction(R.drawable.ic_time, R.string.payment_history) { callbacks.onPaymentHistory(cardId) }
            QuickAction(R.drawable.ic_statistics_s, R.string.recent_spends) { callbacks.onRecentSpends(cardId) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            QuickAction(R.drawable.ic_vue_security_eye, R.string.view_details) { callbacks.onViewDetails(cardId) }
            QuickAction(R.drawable.ic_edit, R.string.edit) { callbacks.onEdit(cardId) }
            QuickAction(R.drawable.ic_delete, R.string.delete) { callbacks.onDelete(cardId) }
        }
    }
}

@Composable
private fun QuickAction(
    @DrawableRes icon: Int,
    label: Int,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(CellWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircleButtonFilled(icon = icon, onClick = onClick)
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(label),
            style = UI.typo.c.style(color = UI.colors.gray, textAlign = TextAlign.Center).copy(fontSize = 10.sp),
            maxLines = 2,
        )
    }
}
