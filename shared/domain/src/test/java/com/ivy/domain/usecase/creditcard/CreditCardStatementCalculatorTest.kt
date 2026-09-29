package com.ivy.domain.usecase.creditcard

import arrow.core.Some
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.data.model.AccountId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.Transaction
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.DayOfMonth
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.model.testing.creditCard
import com.ivy.data.model.testing.dayOfMonth
import com.ivy.domain.model.StatementStatus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.next
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.UUID

@RunWith(TestParameterInjector::class)
class CreditCardStatementCalculatorTest {

    private val calculator = CreditCardStatementCalculator(TestTimeConverter())
    private val cardId = AccountId(UUID.randomUUID())
    private val bankId = AccountId(UUID.randomUUID())
    private val inr = AssetCode.unsafe("INR")

    enum class DatesCase(
        val billingDay: Int,
        val dueDay: Int,
        val today: LocalDate,
        val statement: LocalDate,
        val due: LocalDate,
        val next: LocalDate,
    ) {
        DueAfterStatementSameMonth(
            billingDay = 5,
            dueDay = 25,
            today = LocalDate.of(2026, 9, 10),
            statement = LocalDate.of(2026, 9, 5),
            due = LocalDate.of(2026, 9, 25),
            next = LocalDate.of(2026, 10, 5),
        ),
        DueBeforeStatementRollsToNextMonth(
            billingDay = 15,
            dueDay = 5,
            today = LocalDate.of(2026, 9, 29),
            statement = LocalDate.of(2026, 9, 15),
            due = LocalDate.of(2026, 10, 5),
            next = LocalDate.of(2026, 10, 15),
        ),
        StatementNotYetThisMonth(
            billingDay = 20,
            dueDay = 10,
            today = LocalDate.of(2026, 1, 5),
            statement = LocalDate.of(2025, 12, 20),
            due = LocalDate.of(2026, 1, 10),
            next = LocalDate.of(2026, 1, 20),
        ),
        StatementIsToday(
            billingDay = 1,
            dueDay = 20,
            today = LocalDate.of(2026, 1, 1),
            statement = LocalDate.of(2026, 1, 1),
            due = LocalDate.of(2026, 1, 20),
            next = LocalDate.of(2026, 2, 1),
        ),
        SameDayRollsToNextMonth(
            billingDay = 31,
            dueDay = 31,
            today = LocalDate.of(2026, 4, 15),
            statement = LocalDate.of(2026, 3, 31),
            due = LocalDate.of(2026, 4, 30),
            next = LocalDate.of(2026, 4, 30),
        ),
        ClampsToFebruary(
            billingDay = 31,
            dueDay = 15,
            today = LocalDate.of(2026, 3, 1),
            statement = LocalDate.of(2026, 2, 28),
            due = LocalDate.of(2026, 3, 15),
            next = LocalDate.of(2026, 3, 31),
        ),
        LeapYear(
            billingDay = 29,
            dueDay = 15,
            today = LocalDate.of(2024, 3, 1),
            statement = LocalDate.of(2024, 2, 29),
            due = LocalDate.of(2024, 3, 15),
            next = LocalDate.of(2024, 3, 29),
        ),
    }

    @Test
    fun `statement dates`(@TestParameter case: DatesCase) {
        val dates = calculator.statementDates(
            billingDay = DayOfMonth.unsafe(case.billingDay),
            dueDay = DayOfMonth.unsafe(case.dueDay),
            today = case.today,
        )

        dates.lastStatementDate shouldBe case.statement
        dates.dueDate shouldBe case.due
        dates.nextStatementDate shouldBe case.next
    }

    @Test
    fun `property - date invariants`() = runTest {
        val arbDate = arbitrary {
            LocalDate.of(
                Arb.int(min = 2000, max = 2099).bind(),
                Arb.int(min = 1, max = 12).bind(),
                Arb.int(min = 1, max = 28).bind(),
            )
        }
        checkAll(Arb.dayOfMonth(), Arb.dayOfMonth(), arbDate) { billing, due, today ->
            val dates = calculator.statementDates(billing, due, today)

            (!dates.lastStatementDate.isAfter(today)) shouldBe true
            today.isBefore(dates.nextStatementDate) shouldBe true
            dates.dueDate.isAfter(dates.lastStatementDate) shouldBe true
            (!dates.dueDate.isAfter(dates.nextStatementDate)) shouldBe true
        }
    }

    // Card: statement on the 5th, due on the 25th; "today" is Sep 10 -> S = Sep 5, D = Sep 25
    private val today = LocalDate.of(2026, 9, 10)
    private val card = Arb.creditCard(
        id = Some(cardId),
        billingDay = Some(DayOfMonth.unsafe(5)),
        dueDay = Some(DayOfMonth.unsafe(25)),
        creditLimit = Some(PositiveDouble.unsafe(36_000.0)),
    ).next()

    @Test
    fun `no transactions - statement awaited and full limit available`() {
        val statement = calculator.calculate(card, emptyList(), today)

        statement.due.value shouldBe 0.0
        statement.unbilled.value shouldBe 0.0
        statement.outstanding shouldBe 0.0
        statement.availableLimit shouldBe 36_000.0
        statement.lastPaidOn shouldBe null
        statement.status shouldBe StatementStatus.StatementAwaited(LocalDate.of(2026, 10, 5))
    }

    @Test
    fun `spends before the statement date are due, after it unbilled`() {
        val trns = listOf(
            expense(1_000.0, LocalDate.of(2026, 8, 20)),
            expense(500.0, LocalDate.of(2026, 9, 4)),
            expense(250.0, LocalDate.of(2026, 9, 5)), // on the statement day: unbilled
            expense(300.0, LocalDate.of(2026, 9, 9)),
        )

        val statement = calculator.calculate(card, trns, today)

        statement.due.value shouldBe 1_500.0
        statement.unbilled.value shouldBe 550.0
        statement.outstanding shouldBe 2_050.0
        statement.availableLimit shouldBe 36_000.0 - 2_050.0
        statement.status shouldBe StatementStatus.Due(
            amount = statement.due,
            dueDate = LocalDate.of(2026, 9, 25),
            daysLeft = 15,
        )
    }

    @Test
    fun `partial repayment reduces the due`() {
        val trns = listOf(
            expense(1_000.0, LocalDate.of(2026, 8, 20)),
            repayment(500.0, LocalDate.of(2026, 9, 8)),
        )

        val statement = calculator.calculate(card, trns, today)

        statement.due.value shouldBe 500.0
        statement.unbilled.value shouldBe 0.0
        statement.outstanding shouldBe 500.0
        statement.lastPaidOn shouldBe LocalDate.of(2026, 9, 8)
        statement.status.shouldBeInstanceOf<StatementStatus.Due>()
    }

    @Test
    fun `full repayment marks the statement paid and restores the limit`() {
        val trns = listOf(
            expense(1_000.0, LocalDate.of(2026, 8, 20)),
            repayment(1_000.0, LocalDate.of(2026, 9, 8)),
        )

        val statement = calculator.calculate(card, trns, today)

        statement.due.value shouldBe 0.0
        statement.outstanding shouldBe 0.0
        statement.availableLimit shouldBe 36_000.0
        statement.status shouldBe StatementStatus.Paid(LocalDate.of(2026, 10, 5))
    }

    @Test
    fun `over payment reduces unbilled and never makes due negative`() {
        val trns = listOf(
            expense(1_000.0, LocalDate.of(2026, 8, 20)),
            expense(400.0, LocalDate.of(2026, 9, 7)),
            repayment(1_200.0, LocalDate.of(2026, 9, 8)),
        )

        val statement = calculator.calculate(card, trns, today)

        statement.due.value shouldBe 0.0
        statement.unbilled.value shouldBe 200.0
        statement.outstanding shouldBe 200.0
        statement.status shouldBe StatementStatus.Paid(LocalDate.of(2026, 10, 5))
    }

    @Test
    fun `refund income reduces the outstanding amount`() {
        val trns = listOf(
            expense(1_000.0, LocalDate.of(2026, 8, 20)),
            income(300.0, LocalDate.of(2026, 8, 25)),
            expense(100.0, LocalDate.of(2026, 9, 7)),
            income(150.0, LocalDate.of(2026, 9, 8)),
        )

        val statement = calculator.calculate(card, trns, today)

        statement.due.value shouldBe 700.0
        statement.unbilled.value shouldBe 0.0
        statement.outstanding shouldBe 650.0
    }

    @Test
    fun `overdue after the due date`() {
        val trns = listOf(expense(1_000.0, LocalDate.of(2026, 8, 20)))

        val statement = calculator.calculate(card, trns, LocalDate.of(2026, 9, 28))

        statement.status shouldBe StatementStatus.Overdue(
            amount = statement.due,
            dueDate = LocalDate.of(2026, 9, 25),
            daysOverdue = 3,
        )
    }

    @Test
    fun `due today has zero days left`() {
        val trns = listOf(expense(1_000.0, LocalDate.of(2026, 8, 20)))

        val statement = calculator.calculate(card, trns, LocalDate.of(2026, 9, 25))

        statement.status shouldBe StatementStatus.Due(
            amount = statement.due,
            dueDate = LocalDate.of(2026, 9, 25),
            daysLeft = 0,
        )
    }

    @Test
    fun `unsettled and other accounts transactions are ignored`() {
        val trns = listOf(
            expense(1_000.0, LocalDate.of(2026, 8, 20), settled = false),
            Expense(
                id = TransactionId(UUID.randomUUID()),
                title = null,
                description = null,
                category = null,
                time = LocalDate.of(2026, 8, 20).atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC),
                settled = true,
                metadata = TransactionMetadata(null, null, null, null),
                tags = emptyList(),
                value = PositiveValue(PositiveDouble.unsafe(999.0), inr),
                account = bankId,
            ),
        )

        val statement = calculator.calculate(card, trns, today)

        statement.outstanding shouldBe 0.0
        statement.status.shouldBeInstanceOf<StatementStatus.StatementAwaited>()
    }

    @Test
    fun `transfer out of the card counts as a spend`() {
        val trns = listOf(
            Transfer(
                id = TransactionId(UUID.randomUUID()),
                title = null,
                description = null,
                category = null,
                time = LocalDate.of(2026, 9, 7).atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC),
                settled = true,
                metadata = TransactionMetadata(null, null, null, null),
                tags = emptyList(),
                fromAccount = cardId,
                fromValue = PositiveValue(PositiveDouble.unsafe(200.0), inr),
                toAccount = bankId,
                toValue = PositiveValue(PositiveDouble.unsafe(200.0), inr),
            ),
        )

        val statement = calculator.calculate(card, trns, today)

        statement.unbilled.value shouldBe 200.0
        statement.outstanding shouldBe 200.0
    }

    @Test
    fun `property - due plus unbilled covers the outstanding amount`() = runTest {
        val arbAmount = Arb.int(min = 0, max = 5_000)
        val arbPaid = Arb.int(min = 0, max = 8_000)
        checkAll(arbAmount, arbAmount, arbPaid) { before, after, paid ->
            val trns = buildList {
                if (before > 0) add(expense(before.toDouble(), LocalDate.of(2026, 8, 20)))
                if (after > 0) add(expense(after.toDouble(), LocalDate.of(2026, 9, 7)))
                if (paid > 0) add(repayment(paid.toDouble(), LocalDate.of(2026, 9, 8)))
            }

            val statement = calculator.calculate(card, trns, today)

            val covered = statement.due.value + statement.unbilled.value
            if (statement.outstanding >= 0.0) {
                covered shouldBe statement.outstanding
            } else {
                covered shouldBe 0.0
            }
            statement.availableLimit shouldBe 36_000.0 - statement.outstanding
        }
    }

    private fun expense(amount: Double, date: LocalDate, settled: Boolean = true): Transaction = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = date.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC),
        settled = settled,
        metadata = TransactionMetadata(null, null, null, null),
        tags = emptyList(),
        value = PositiveValue(PositiveDouble.unsafe(amount), inr),
        account = cardId,
    )

    private fun income(amount: Double, date: LocalDate): Transaction = Income(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = date.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC),
        settled = true,
        metadata = TransactionMetadata(null, null, null, null),
        tags = emptyList(),
        value = PositiveValue(PositiveDouble.unsafe(amount), inr),
        account = cardId,
    )

    private fun repayment(amount: Double, date: LocalDate): Transaction = Transfer(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = date.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC),
        settled = true,
        metadata = TransactionMetadata(null, null, null, null),
        tags = emptyList(),
        fromAccount = bankId,
        fromValue = PositiveValue(PositiveDouble.unsafe(amount), inr),
        toAccount = cardId,
        toValue = PositiveValue(PositiveDouble.unsafe(amount), inr),
    )
}
