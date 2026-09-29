package com.ivy.domain.usecase.creditcard

import arrow.core.Some
import com.ivy.base.time.TimeProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.model.AccountId
import com.ivy.data.model.Transaction
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.model.testing.account
import com.ivy.data.model.testing.creditCard
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CreditCardRepository
import com.ivy.data.repository.TransactionRepository
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

class RecordCreditCardPaymentUseCaseTest {
    private val creditCardRepository = mockk<CreditCardRepository>()
    private val accountRepository = mockk<AccountRepository>()
    private val transactionRepository = mockk<TransactionRepository>(relaxed = true)
    private val timeProvider = mockk<TimeProvider>()
    private val dataObserver = mockk<DataObserver>(relaxed = true)

    private val cardId = AccountId(UUID.randomUUID())
    private val bankId = AccountId(UUID.randomUUID())
    private val inr = AssetCode.unsafe("INR")
    private val now = Instant.parse("2026-09-10T06:30:00Z")

    private val card = Arb.creditCard(id = Some(cardId)).next()
        .copy(issuer = NotBlankTrimmedString.unsafe("HDFC Bank"))
    private val cardAccount = Arb.account(accountId = Some(cardId), asset = Some(inr)).next()
    private val bank = Arb.account(accountId = Some(bankId), asset = Some(inr)).next()

    private lateinit var useCase: RecordCreditCardPaymentUseCase

    @Before
    fun setup() {
        coEvery { creditCardRepository.findById(cardId) } returns card
        coEvery { accountRepository.findById(cardId) } returns cardAccount
        coEvery { accountRepository.findById(bankId) } returns bank
        every { timeProvider.utcNow() } returns now
        useCase = RecordCreditCardPaymentUseCase(
            creditCardRepository = creditCardRepository,
            accountRepository = accountRepository,
            transactionRepository = transactionRepository,
            timeProvider = timeProvider,
            dataObserver = dataObserver,
        )
    }

    @Test
    fun `records a transfer from the bank into the card`() = runTest {
        val saved = slot<Transaction>()
        coEvery { transactionRepository.save(capture(saved)) } returns Unit

        val transfer = useCase.record(
            cardId = cardId,
            amount = PositiveDouble.unsafe(500.0),
            paidFromAccountId = bankId,
            paidAt = null,
            note = "September bill",
        ).shouldBeRight()

        saved.captured shouldBe transfer
        transfer.fromAccount shouldBe bankId
        transfer.toAccount shouldBe cardId
        transfer.fromValue.amount.value shouldBe 500.0
        transfer.toValue.amount.value shouldBe 500.0
        transfer.fromValue.asset shouldBe inr
        transfer.toValue.asset shouldBe inr
        transfer.title?.value shouldBe "HDFC Bank Credit card repayment"
        transfer.description?.value shouldBe "September bill"
        transfer.settled shouldBe true
        transfer.category shouldBe null
        transfer.time shouldBe now
        coVerify { dataObserver.post(DataWriteEvent.AllDataChange) }
    }

    @Test
    fun `uses the account name when the card has no issuer`() = runTest {
        coEvery { creditCardRepository.findById(cardId) } returns card.copy(issuer = null)

        val transfer = useCase.record(cardId, PositiveDouble.unsafe(1.0), bankId, null, null).shouldBeRight()

        transfer.title?.value shouldBe "${cardAccount.name.value} Credit card repayment"
        transfer.description shouldBe null
    }

    @Test
    fun `explicit paid-at is kept`() = runTest {
        val paidAt = Instant.parse("2026-09-01T00:00:00Z")

        val transfer = useCase.record(cardId, PositiveDouble.unsafe(1.0), bankId, paidAt, null).shouldBeRight()

        transfer.time shouldBe paidAt
    }

    @Test
    fun `errors`() = runTest {
        useCase.record(cardId, PositiveDouble.unsafe(1.0), cardId, null, null)
            .shouldBeLeft() shouldBe CreditCardPaymentError.PaidFromIsCard

        val other = AccountId(UUID.randomUUID())
        coEvery { accountRepository.findById(other) } returns null
        useCase.record(cardId, PositiveDouble.unsafe(1.0), other, null, null)
            .shouldBeLeft() shouldBe CreditCardPaymentError.PaidFromAccountNotFound

        coEvery { creditCardRepository.findById(cardId) } returns null
        useCase.record(cardId, PositiveDouble.unsafe(1.0), bankId, null, null)
            .shouldBeLeft() shouldBe CreditCardPaymentError.CardNotFound

        coVerify(exactly = 0) { transactionRepository.save(any()) }
    }
}
