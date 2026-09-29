package com.ivy.domain.usecase.creditcard

import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.Transfer
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.TransactionRepository
import javax.inject.Inject

/**
 * A repayment recorded on a card: the transfer and the account it was paid from
 * (null when that account no longer exists).
 */
data class CreditCardRepayment(
    val transfer: Transfer,
    val paidFrom: Account?,
)

class CreditCardPaymentsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
) {
    /**
     * Settled transfers into the card, newest first.
     */
    suspend fun history(cardId: AccountId): List<CreditCardRepayment> {
        val transfers = transactionRepository.findAllTransfersToAccount(cardId)
            .filter { it.settled }
            .sortedByDescending { it.time }
        val accountsById = accountRepository.findAll().associateBy { it.id }
        return transfers.map { transfer ->
            CreditCardRepayment(
                transfer = transfer,
                paidFrom = accountsById[transfer.fromAccount],
            )
        }
    }
}
