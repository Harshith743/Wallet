package com.ivy.domain.usecase.creditcard

import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.db.dao.write.WritePlannedPaymentRuleDao
import com.ivy.data.model.AccountId
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CreditCardRepository
import com.ivy.data.repository.CreditCardSecretsRepository
import com.ivy.data.repository.TransactionRepository
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

class DeleteCreditCardUseCaseTest {
    private val transactionRepository = mockk<TransactionRepository>(relaxed = true)
    private val plannedPaymentRuleWriter = mockk<WritePlannedPaymentRuleDao>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>(relaxed = true)
    private val creditCardRepository = mockk<CreditCardRepository>(relaxed = true)
    private val secretsRepository = mockk<CreditCardSecretsRepository>(relaxed = true)
    private val dataObserver = mockk<DataObserver>(relaxed = true)

    private val useCase = DeleteCreditCardUseCase(
        transactionRepository = transactionRepository,
        plannedPaymentRuleWriter = plannedPaymentRuleWriter,
        accountRepository = accountRepository,
        creditCardRepository = creditCardRepository,
        secretsRepository = secretsRepository,
        dataObserver = dataObserver,
    )

    @Test
    fun `deletes transactions both ways, planned payments, account, card row and secrets`() = runTest {
        val cardId = AccountId(UUID.randomUUID())

        useCase.delete(cardId)

        coVerifyOrder {
            transactionRepository.deleteAllByAccountId(cardId)
            transactionRepository.deleteAllByToAccountId(cardId)
            plannedPaymentRuleWriter.deletedByAccountId(cardId.value)
            accountRepository.deleteById(cardId)
            creditCardRepository.deleteById(cardId)
            secretsRepository.clear(cardId)
            dataObserver.post(DataWriteEvent.AllDataChange)
        }
    }
}
