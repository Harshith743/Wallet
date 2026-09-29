package com.ivy.creditcards.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.ivy.creditcards.model.StatementLabel
import com.ivy.ui.R

@Composable
fun StatementLabel.text(): String = when (this) {
    StatementLabel.DueToday -> stringResource(R.string.credit_card_due_today)
    is StatementLabel.DueInDays -> pluralStringResource(R.plurals.credit_card_due_in_days, days, days)
    is StatementLabel.DueOn -> stringResource(R.string.credit_card_due_on, dateText)
    is StatementLabel.Overdue -> if (days > 0) {
        pluralStringResource(R.plurals.credit_card_overdue_by_days, days, days)
    } else {
        stringResource(R.string.credit_card_overdue)
    }

    StatementLabel.Paid -> stringResource(R.string.credit_card_paid)
    StatementLabel.Awaited -> stringResource(R.string.credit_card_statement_awaited)
}

fun StatementLabel.isOverdue(): Boolean = this is StatementLabel.Overdue
