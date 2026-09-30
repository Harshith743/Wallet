package com.ivy.domain.usecase.creditcard

import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.db.dao.write.WritePlannedPaymentRuleDao
import com.ivy.data.model.AccountId
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CreditCardRepository
import com.ivy.data.repository.CreditCardSecretsRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.skin.CardSkinImageStore
import javax.inject.Inject

/**
 * Deletes a credit card and everything that belongs to it, mirroring how an account is
 * deleted from the Transactions screen, plus the transfers *into* the card (repayments)
 * so no transfer is left pointing at a missing destination, and the card's photo skin.
 */
class DeleteCreditCardUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val plannedPaymentRuleWriter: WritePlannedPaymentRuleDao,
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val secretsRepository: CreditCardSecretsRepository,
    private val imageStore: CardSkinImageStore,
    private val dataObserver: DataObserver,
) {

    suspend fun delete(cardId: AccountId) {
        transactionRepository.deleteAllByAccountId(cardId)
        transactionRepository.deleteAllByToAccountId(cardId)
        plannedPaymentRuleWriter.deletedByAccountId(cardId.value)
        accountRepository.deleteById(cardId)
        creditCardRepository.deleteById(cardId)
        secretsRepository.clear(cardId)
        imageStore.delete(cardId.value)
        dataObserver.post(DataWriteEvent.AllDataChange)
    }
}
