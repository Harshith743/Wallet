package com.ivy.domain.usecase.creditcard

import arrow.core.Some
import com.ivy.base.TestDispatchersProvider
import com.ivy.base.time.TimeProvider
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.data.model.AccountId
import com.ivy.data.model.Expense
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.DayOfMonth
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.model.testing.account
import com.ivy.data.model.testing.creditCard
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CreditCardRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.model.StatementStatus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.UUID

class CreditCardsOverviewUseCaseTest {
    private val creditCardRepository = mockk<CreditCardRepository>()
    private val accountRepository = mockk<AccountRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val timeProvider = mockk<TimeProvider>()

    private val inr = AssetCode.unsafe("INR")
    private val today = LocalDate.of(2026, 9, 10)

    private val useCase = CreditCardsOverviewUseCase(
        creditCardRepository = creditCardRepository,
        accountRepository = accountRepository,
        transactionRepository = transactionRepository,
        calculator = CreditCardStatementCalculator(TestTimeConverter()),
        timeProvider = timeProvider,
        dispatchers = TestDispatchersProvider,
    )

    @Test
    fun `joins cards with accounts, computes statements and totals, skips orphans`() = runTest {
        val firstId = AccountId(UUID.randomUUID())
        val secondId = AccountId(UUID.randomUUID())
        val orphanId = AccountId(UUID.randomUUID())
        val bankId = AccountId(UUID.randomUUID())
        val first = card(firstId)
        val second = card(secondId)
        val orphan = card(orphanId)
        every { timeProvider.localDateNow() } returns today
        coEvery { creditCardRepository.findAll() } returns listOf(second, first, orphan)
        coEvery { accountRepository.findAll() } returns listOf(
            Arb.account(accountId = Some(firstId), orderNum = Some(1.0)).next(),
            Arb.account(accountId = Some(secondId), orderNum = Some(2.0)).next(),
        )
        // first: 1000 billed, 300 repaid -> due 700
        coEvery { transactionRepository.findAllByAccount(firstId) } returns listOf(
            expense(firstId, 1_000.0, LocalDate.of(2026, 8, 20)),
        )
        coEvery { transactionRepository.findAllTransfersToAccount(firstId) } returns listOf(
            repayment(bankId, firstId, 300.0, LocalDate.of(2026, 9, 6)),
        )
        // second: nothing billed, 200 unbilled
        coEvery { transactionRepository.findAllByAccount(secondId) } returns listOf(
            expense(secondId, 200.0, LocalDate.of(2026, 9, 7)),
        )
        coEvery { transactionRepository.findAllTransfersToAccount(secondId) } returns emptyList()

        val overview = useCase.overview()

        overview.cards.map { it.card.id } shouldBe listOf(firstId, secondId)
        overview.cards[0].statement.due.value shouldBe 700.0
        overview.cards[0].statement.status.shouldBeInstanceOf<StatementStatus.Due>()
        overview.cards[1].statement.unbilled.value shouldBe 200.0
        overview.cards[1].statement.status.shouldBeInstanceOf<StatementStatus.StatementAwaited>()
        overview.totalDue shouldBe 700.0
        overview.cardsWithDueCount shouldBe 1
    }

    @Test
    fun `isCreditCard and creditCardIds come from the card repository`() = runTest {
        val cardId = AccountId(UUID.randomUUID())
        coEvery { creditCardRepository.findAllIds() } returns setOf(cardId)

        useCase.isCreditCard(cardId) shouldBe true
        useCase.isCreditCard(AccountId(UUID.randomUUID())) shouldBe false
        useCase.creditCardIds() shouldBe setOf(cardId)
    }

    @Test
    fun `statement for a single card, null when missing`() = runTest {
        val cardId = AccountId(UUID.randomUUID())
        every { timeProvider.localDateNow() } returns today
        coEvery { creditCardRepository.findById(cardId) } returns card(cardId)
        coEvery { accountRepository.findById(cardId) } returns Arb.account(accountId = Some(cardId)).next()
        coEvery { transactionRepository.findAllByAccount(cardId) } returns emptyList()
        coEvery { transactionRepository.findAllTransfersToAccount(cardId) } returns emptyList()

        useCase.statement(cardId)?.statement?.availableLimit shouldBe 36_000.0

        coEvery { creditCardRepository.findById(cardId) } returns null
        useCase.statement(cardId) shouldBe null
    }

    private fun card(id: AccountId) = Arb.creditCard(
        id = Some(id),
        billingDay = Some(DayOfMonth.unsafe(5)),
        dueDay = Some(DayOfMonth.unsafe(25)),
        creditLimit = Some(PositiveDouble.unsafe(36_000.0)),
    ).next()

    private fun expense(account: AccountId, amount: Double, date: LocalDate) = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = date.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC),
        settled = true,
        metadata = TransactionMetadata(null, null, null, null),
        tags = emptyList(),
        value = PositiveValue(PositiveDouble.unsafe(amount), inr),
        account = account,
    )

    private fun repayment(from: AccountId, to: AccountId, amount: Double, date: LocalDate) = Transfer(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = date.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC),
        settled = true,
        metadata = TransactionMetadata(null, null, null, null),
        tags = emptyList(),
        fromAccount = from,
        fromValue = PositiveValue(PositiveDouble.unsafe(amount), inr),
        toAccount = to,
        toValue = PositiveValue(PositiveDouble.unsafe(amount), inr),
    )
}
