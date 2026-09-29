package com.ivy.domain.model

import com.ivy.data.model.Account
import com.ivy.data.model.CreditCard
import com.ivy.data.model.primitive.NonNegativeDouble
import java.time.LocalDate

sealed interface StatementStatus {
    data class Due(
        val amount: NonNegativeDouble,
        val dueDate: LocalDate,
        val daysLeft: Long,
    ) : StatementStatus

    data class Overdue(
        val amount: NonNegativeDouble,
        val dueDate: LocalDate,
        val daysOverdue: Long,
    ) : StatementStatus

    data class Paid(
        val nextStatementDate: LocalDate,
    ) : StatementStatus

    data class StatementAwaited(
        val nextStatementDate: LocalDate,
    ) : StatementStatus
}

data class StatementDates(
    val lastStatementDate: LocalDate,
    val dueDate: LocalDate,
    val nextStatementDate: LocalDate,
)

/**
 * Everything derived from a card's transactions for the current billing cycle.
 *
 * - [due]: statement balance minus repayments made since the statement date.
 * - [unbilled]: spends since the statement date, net of refunds and over-payments.
 * - [outstanding]: what is owed right now (negative account balance); may be negative
 *   when the card has been over-paid.
 * - [availableLimit]: credit limit minus [outstanding].
 */
data class CreditCardStatement(
    val due: NonNegativeDouble,
    val unbilled: NonNegativeDouble,
    val outstanding: Double,
    val availableLimit: Double,
    val dates: StatementDates,
    val lastPaidOn: LocalDate?,
    val status: StatementStatus,
)

data class CreditCardWithAccount(
    val card: CreditCard,
    val account: Account,
)

data class CreditCardWithStatement(
    val card: CreditCard,
    val account: Account,
    val statement: CreditCardStatement,
)
