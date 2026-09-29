package com.ivy.creditcards.model

import com.ivy.data.model.primitive.NonNegativeDouble
import com.ivy.domain.model.StatementStatus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test
import java.time.LocalDate

class StatementLabelMapperTest {
    private val mapper = StatementLabelMapper()
    private val amount = NonNegativeDouble.unsafe(100.0)
    private val date = LocalDate.of(2026, 10, 11)

    @Test
    fun `due today`() {
        mapper.map(StatementStatus.Due(amount, date, daysLeft = 0)) shouldBe StatementLabel.DueToday
    }

    @Test
    fun `due within a week shows days`() {
        mapper.map(StatementStatus.Due(amount, date, daysLeft = 6)) shouldBe StatementLabel.DueInDays(6)
        mapper.map(StatementStatus.Due(amount, date, daysLeft = 7)) shouldBe StatementLabel.DueInDays(7)
    }

    @Test
    fun `due later shows the date`() {
        mapper.map(StatementStatus.Due(amount, date, daysLeft = 8)).shouldBeInstanceOf<StatementLabel.DueOn>()
    }

    @Test
    fun `overdue, paid and awaited`() {
        mapper.map(StatementStatus.Overdue(amount, date, daysOverdue = 3)) shouldBe StatementLabel.Overdue(3)
        mapper.map(StatementStatus.Paid(date)) shouldBe StatementLabel.Paid
        mapper.map(StatementStatus.StatementAwaited(date)) shouldBe StatementLabel.Awaited
    }
}
