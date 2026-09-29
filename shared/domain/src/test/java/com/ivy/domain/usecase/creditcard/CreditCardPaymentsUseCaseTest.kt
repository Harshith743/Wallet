package com.ivy.domain.usecase.creditcard

import arrow.core.Some
import com.ivy.data.model.AccountId
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.model.testing.account
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.TransactionRepository
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.util.UUID

class CreditCardPaymentsUseCaseTest {
    private val transactionRepository = mockk<TransactionRepository>()
    private val accountRepository = mockk<AccountRepository>()
    private val useCase = CreditCardPaymentsUseCase(transactionRepository, accountRepository)

    private val inr = AssetCode.unsafe("INR")

    @Test
    fun `settled repayments newest first with their source account`() = runTest {
        val cardId = AccountId(UUID.randomUUID())
        val bankId = AccountId(UUID.randomUUID())
        val goneId = AccountId(UUID.randomUUID())
        val bank = Arb.account(accountId = Some(bankId)).next()
        val older = transfer(bankId, cardId, Instant.parse("2026-08-01T00:00:00Z"), settled = true)
        val newer = transfer(goneId, cardId, Instant.parse("2026-09-01T00:00:00Z"), settled = true)
        val planned = transfer(bankId, cardId, Instant.parse("2026-10-01T00:00:00Z"), settled = false)
        coEvery { transactionRepository.findAllTransfersToAccount(cardId) } returns listOf(older, planned, newer)
        coEvery { accountRepository.findAll() } returns listOf(bank)

        val history = useCase.history(cardId)

        history.map { it.transfer } shouldBe listOf(newer, older)
        history[0].paidFrom shouldBe null
        history[1].paidFrom shouldBe bank
    }

    private fun transfer(from: AccountId, to: AccountId, time: Instant, settled: Boolean) = Transfer(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = time,
        settled = settled,
        metadata = TransactionMetadata(null, null, null, null),
        tags = emptyList(),
        fromAccount = from,
        fromValue = PositiveValue(PositiveDouble.unsafe(1.0), inr),
        toAccount = to,
        toValue = PositiveValue(PositiveDouble.unsafe(1.0), inr),
    )
}
