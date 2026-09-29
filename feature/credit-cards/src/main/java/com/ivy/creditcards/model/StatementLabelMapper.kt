package com.ivy.creditcards.model

import com.ivy.domain.model.StatementStatus
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

private const val DueSoonDays = 7L

class StatementLabelMapper @Inject constructor() {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    fun map(status: StatementStatus): StatementLabel = when (status) {
        is StatementStatus.Due -> when {
            status.daysLeft == 0L -> StatementLabel.DueToday
            status.daysLeft <= DueSoonDays -> StatementLabel.DueInDays(status.daysLeft.toInt())
            else -> StatementLabel.DueOn(status.dueDate.format(dateFormatter))
        }

        is StatementStatus.Overdue -> StatementLabel.Overdue(status.daysOverdue.toInt())
        is StatementStatus.Paid -> StatementLabel.Paid
        is StatementStatus.StatementAwaited -> StatementLabel.Awaited
    }
}
