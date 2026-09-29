package com.ivy.domain.usecase.creditcard

import com.ivy.base.time.TimeConverter
import com.ivy.data.model.AccountId
import com.ivy.data.model.CreditCard
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.DayOfMonth
import com.ivy.data.model.primitive.NonNegativeDouble
import com.ivy.domain.model.CreditCardStatement
import com.ivy.domain.model.StatementDates
import com.ivy.domain.model.StatementStatus
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * Pure statement maths for a credit card, computed from the card account's transactions.
 *
 * Dates (S = last statement date, D = due date, S' = next statement date):
 * 1. S = clamp(this month, billingDay); if S is after today, S = clamp(previous month, billingDay).
 * 2. D = clamp(month of S, dueDay); if D <= S, D = clamp(month of S + 1, dueDay).
 * 3. S' = clamp(month of S + 1, billingDay). Invariant: S <= today < S' and S < D <= S'.
 *
 * Amounts (only settled transactions count; spends on the statement day itself are unbilled):
 * - outstandingAtS = -(balance of transactions dated before S)
 * - paymentsSinceS = transfers into the card dated on/after S
 * - due            = max(0, outstandingAtS - paymentsSinceS)
 * - outstandingNow = -(balance of all transactions)
 * - unbilled       = max(0, outstandingNow - due)
 * - availableLimit = creditLimit - outstandingNow
 *
 * Status: no statement balance -> StatementAwaited; due == 0 -> Paid; otherwise Due until D, then Overdue.
 */
class CreditCardStatementCalculator @Inject constructor(
    private val timeConverter: TimeConverter,
) {

    fun statementDates(
        billingDay: DayOfMonth,
        dueDay: DayOfMonth,
        today: LocalDate,
    ): StatementDates {
        val thisMonth = YearMonth.from(today)
        var lastStatement = clamp(thisMonth, billingDay)
        if (lastStatement.isAfter(today)) {
            lastStatement = clamp(thisMonth.minusMonths(1), billingDay)
        }
        val statementMonth = YearMonth.from(lastStatement)
        var dueDate = clamp(statementMonth, dueDay)
        if (!dueDate.isAfter(lastStatement)) {
            dueDate = clamp(statementMonth.plusMonths(1), dueDay)
        }
        val nextStatement = clamp(statementMonth.plusMonths(1), billingDay)
        return StatementDates(
            lastStatementDate = lastStatement,
            dueDate = dueDate,
            nextStatementDate = nextStatement,
        )
    }

    fun calculate(
        card: CreditCard,
        transactions: List<Transaction>,
        today: LocalDate,
    ): CreditCardStatement {
        val dates = statementDates(card.billingDay, card.dueDay, today)
        val settled = transactions.filter { it.settled }
        val dated = settled.map { trn -> trn to with(timeConverter) { trn.time.toLocalDate() } }

        // "0.0 - x" instead of "-x" so an empty sum yields +0.0, not -0.0
        val outstandingAtStatement = 0.0 - dated
            .filter { (_, date) -> date.isBefore(dates.lastStatementDate) }
            .sumOf { (trn, _) -> trn.signedAmountFor(card.id) }
        val paymentsSinceStatement = dated
            .filter { (trn, date) ->
                trn.isPaymentInto(card.id) && !date.isBefore(dates.lastStatementDate)
            }
            .sumOf { (trn, _) -> (trn as Transfer).toValue.amount.value }
        val outstandingNow = 0.0 - settled.sumOf { it.signedAmountFor(card.id) }

        val due = max(0.0, outstandingAtStatement - paymentsSinceStatement)
        val unbilled = max(0.0, outstandingNow - due)
        val lastPaidOn = dated
            .filter { (trn, _) -> trn.isPaymentInto(card.id) }
            .maxOfOrNull { (_, date) -> date }

        val status = when {
            outstandingAtStatement <= 0.0 -> StatementStatus.StatementAwaited(dates.nextStatementDate)
            due <= 0.0 -> StatementStatus.Paid(dates.nextStatementDate)
            !today.isAfter(dates.dueDate) -> StatementStatus.Due(
                amount = NonNegativeDouble.unsafe(due),
                dueDate = dates.dueDate,
                daysLeft = ChronoUnit.DAYS.between(today, dates.dueDate),
            )

            else -> StatementStatus.Overdue(
                amount = NonNegativeDouble.unsafe(due),
                dueDate = dates.dueDate,
                daysOverdue = ChronoUnit.DAYS.between(dates.dueDate, today),
            )
        }

        return CreditCardStatement(
            due = NonNegativeDouble.unsafe(due),
            unbilled = NonNegativeDouble.unsafe(unbilled),
            outstanding = outstandingNow,
            availableLimit = card.creditLimit.value - outstandingNow,
            dates = dates,
            lastPaidOn = lastPaidOn,
            status = status,
        )
    }

    private fun clamp(month: YearMonth, day: DayOfMonth): LocalDate =
        month.atDay(min(day.value, month.lengthOfMonth()))

    private fun Transaction.isPaymentInto(cardId: AccountId): Boolean =
        this is Transfer && toAccount == cardId

    /**
     * Effect of a transaction on the card account's balance, using the same sign
     * convention as the account balance: spends are negative, money in is positive.
     */
    private fun Transaction.signedAmountFor(cardId: AccountId): Double = when (this) {
        is Expense -> if (account == cardId) -value.amount.value else 0.0
        is Income -> if (account == cardId) value.amount.value else 0.0
        is Transfer -> {
            val incoming = if (toAccount == cardId) toValue.amount.value else 0.0
            val outgoing = if (fromAccount == cardId) fromValue.amount.value else 0.0
            incoming - outgoing
        }
    }
}
