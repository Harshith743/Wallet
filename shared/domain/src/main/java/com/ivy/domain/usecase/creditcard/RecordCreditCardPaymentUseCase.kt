package com.ivy.domain.usecase.creditcard

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ivy.base.time.TimeProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.model.AccountId
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CreditCardRepository
import com.ivy.data.repository.TransactionRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

sealed interface CreditCardPaymentError {
    data object CardNotFound : CreditCardPaymentError
    data object PaidFromAccountNotFound : CreditCardPaymentError
    data object PaidFromIsCard : CreditCardPaymentError
}

private const val RepaymentTitleSuffix = "Credit card repayment"

/**
 * Records a repayment as a transfer from one of the user's accounts into the card's
 * account. Nothing is stored on the card row: due and available limit are recomputed
 * from transactions by [CreditCardStatementCalculator].
 */
class RecordCreditCardPaymentUseCase @Inject constructor(
    private val creditCardRepository: CreditCardRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val timeProvider: TimeProvider,
    private val dataObserver: DataObserver,
) {

    suspend fun record(
        cardId: AccountId,
        amount: PositiveDouble,
        paidFromAccountId: AccountId,
        paidAt: Instant?,
        note: String?,
    ): Either<CreditCardPaymentError, Transfer> = either {
        ensure(paidFromAccountId != cardId) { CreditCardPaymentError.PaidFromIsCard }
        val card = ensureNotNull(creditCardRepository.findById(cardId)) {
            CreditCardPaymentError.CardNotFound
        }
        val cardAccount = ensureNotNull(accountRepository.findById(cardId)) {
            CreditCardPaymentError.CardNotFound
        }
        val paidFrom = ensureNotNull(accountRepository.findById(paidFromAccountId)) {
            CreditCardPaymentError.PaidFromAccountNotFound
        }

        val payeeName = card.issuer?.value ?: cardAccount.name.value
        val transfer = Transfer(
            id = TransactionId(UUID.randomUUID()),
            title = NotBlankTrimmedString.unsafe("$payeeName $RepaymentTitleSuffix"),
            description = note?.let(NotBlankTrimmedString::from)?.getOrNull(),
            category = null,
            time = paidAt ?: timeProvider.utcNow(),
            settled = true,
            metadata = TransactionMetadata(
                recurringRuleId = null,
                paidForDateTime = null,
                loanId = null,
                loanRecordId = null,
            ),
            tags = emptyList(),
            fromAccount = paidFrom.id,
            fromValue = PositiveValue(amount = amount, asset = paidFrom.asset),
            toAccount = cardAccount.id,
            toValue = PositiveValue(amount = amount, asset = cardAccount.asset),
        )
        transactionRepository.save(transfer)
        // TransactionRepository posts no event; refresh balances and card statements.
        dataObserver.post(DataWriteEvent.AllDataChange)
        transfer
    }
}
