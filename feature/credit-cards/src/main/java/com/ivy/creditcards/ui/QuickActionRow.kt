package com.ivy.creditcards.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.CircleButtonFilled

data class QuickActionCallbacks(
    val onMarkAsPaid: () -> Unit,
    val onRecentSpends: () -> Unit,
    val onEdit: () -> Unit,
    val onDelete: () -> Unit,
)

@Composable
fun QuickActionRow(
    callbacks: QuickActionCallbacks,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        QuickAction(icon = R.drawable.ic_check, label = R.string.mark_as_paid, onClick = callbacks.onMarkAsPaid)
        QuickAction(
            icon = R.drawable.ic_statistics_s,
            label = R.string.recent_spends,
            onClick = callbacks.onRecentSpends,
        )
        QuickAction(icon = R.drawable.ic_edit, label = R.string.edit, onClick = callbacks.onEdit)
        QuickAction(icon = R.drawable.ic_delete, label = R.string.delete, onClick = callbacks.onDelete)
    }
}

@Composable
private fun QuickAction(
    @DrawableRes icon: Int,
    label: Int,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircleButtonFilled(icon = icon, onClick = onClick)
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(label),
            style = UI.typo.c.style(color = UI.colors.gray, textAlign = TextAlign.Center),
            maxLines = 2,
        )
    }
}
