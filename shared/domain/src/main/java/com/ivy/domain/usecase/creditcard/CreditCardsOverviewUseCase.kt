package com.ivy.domain.usecase.creditcard

import com.ivy.base.threading.DispatchersProvider
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.CreditCard
import com.ivy.data.model.Transaction
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CreditCardRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.model.CreditCardWithStatement
import com.ivy.domain.model.StatementStatus
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

data class CreditCardsOverview(
    val cards: List<CreditCardWithStatement>,
    val totalDue: Double,
    val cardsWithDueCount: Int,
)

/**
 * Joins card rows with their accounts and transactions and computes each statement.
 * Cards whose account row is missing (for example dropped on backup import) are skipped.
 */
class CreditCardsOverviewUseCase @Inject constructor(
    private val creditCardRepository: CreditCardRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val calculator: CreditCardStatementCalculator,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatchersProvider,
) {

    suspend fun overview(): CreditCardsOverview = withContext(dispatchers.default) {
        val accountsById = accountRepository.findAll().associateBy { it.id }
        val today = timeProvider.localDateNow()
        val cards = creditCardRepository.findAll()
            .sortedBy { accountsById[it.id]?.orderNum ?: Double.MAX_VALUE }
            .mapNotNull { card ->
                val account = accountsById[card.id]
                if (account == null) {
                    Timber.d("Credit card ${card.id.value} has no account row; skipping")
                    null
                } else {
                    withStatement(card, account, today)
                }
            }
        val dueAmounts = cards.mapNotNull { it.statement.status.dueAmount() }
        CreditCardsOverview(
            cards = cards,
            totalDue = dueAmounts.sum(),
            cardsWithDueCount = dueAmounts.size,
        )
    }

    suspend fun statement(cardId: AccountId): CreditCardWithStatement? =
        withContext(dispatchers.default) {
            val card = creditCardRepository.findById(cardId) ?: return@withContext null
            val account = accountRepository.findById(cardId) ?: return@withContext null
            withStatement(card, account, timeProvider.localDateNow())
        }

    suspend fun isCreditCard(accountId: AccountId): Boolean =
        creditCardRepository.findAllIds().contains(accountId)

    suspend fun creditCardIds(): Set<AccountId> = creditCardRepository.findAllIds()

    private suspend fun withStatement(
        card: CreditCard,
        account: Account,
        today: java.time.LocalDate,
    ): CreditCardWithStatement = CreditCardWithStatement(
        card = card,
        account = account,
        statement = calculator.calculate(card, cardTransactions(card.id), today),
    )

    /**
     * Everything that touches the card account: what it paid for, plus transfers into it.
     * The two sets are disjoint (self-transfers are rejected by the mapper); distinctBy is
     * defensive.
     */
    private suspend fun cardTransactions(cardId: AccountId): List<Transaction> =
        (transactionRepository.findAllByAccount(cardId) + transactionRepository.findAllTransfersToAccount(cardId))
            .filter { it.settled }
            .distinctBy { it.id }

    private fun StatementStatus.dueAmount(): Double? = when (this) {
        is StatementStatus.Due -> amount.value
        is StatementStatus.Overdue -> amount.value
        is StatementStatus.Paid, is StatementStatus.StatementAwaited -> null
    }
}
