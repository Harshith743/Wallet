package com.ivy.domain.usecase.creditcard

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.CardNetwork
import com.ivy.data.model.CardSecrets
import com.ivy.data.model.CreditCard
import com.ivy.data.model.Expense
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.primitive.CardBin
import com.ivy.data.model.primitive.CardCvv
import com.ivy.data.model.primitive.CardLast4
import com.ivy.data.model.primitive.CardPan
import com.ivy.data.model.primitive.DayOfMonth
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CreditCardRepository
import com.ivy.data.repository.CreditCardSecretsRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.creditcard.BinLookup
import com.ivy.domain.creditcard.CardNetworkDetector
import com.ivy.domain.creditcard.defaultIssuerFor
import com.ivy.domain.creditcard.isValidLuhn
import com.ivy.domain.creditcard.normalizeCardNumber
import com.ivy.domain.model.CreditCardWithAccount
import java.time.Instant
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

private const val FirstMonth = 1
private const val LastMonth = 12
private const val CardIconAsset = "ic_vue_money_card"
private const val OpeningBalanceTitle = "Opening balance"

/**
 * Creates or updates a credit card together with the account row that backs it.
 *
 * Write order matters: the card row is saved before the account row, because the
 * account save event makes the Accounts tab reload and its filter must already know
 * that the new account is a card.
 */
class SaveCreditCardUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val secretsRepository: CreditCardSecretsRepository,
    private val transactionRepository: TransactionRepository,
    private val currencyRepository: CurrencyRepository,
    private val calculator: CreditCardStatementCalculator,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val dataObserver: DataObserver,
    private val binLookup: BinLookup,
) {

    suspend fun save(
        draft: CreditCardDraft,
        existing: CreditCardWithAccount?,
    ): Either<CreditCardError, CreditCardWithAccount> = either {
        val name = NotBlankTrimmedString.from(draft.name)
            .mapLeft { CreditCardError.BlankName }.bind()
        val expiry = validateExpiry(draft)
        val creditLimit = PositiveDouble.from(draft.creditLimit)
            .mapLeft { CreditCardError.InvalidCreditLimit }.bind()
        val billingDay = DayOfMonth.from(draft.billingDay)
            .mapLeft { CreditCardError.InvalidBillingDay }.bind()
        val dueDay = DayOfMonth.from(draft.dueDay)
            .mapLeft { CreditCardError.InvalidDueDay }.bind()
        ensure(draft.openingDue.isValidAmount() && draft.openingUnbilled.isValidAmount()) {
            CreditCardError.InvalidOpeningAmount
        }
        val identity = resolveIdentity(draft, existing?.card)
        val cvv = draft.cvv?.takeIf { it.isNotBlank() }?.let {
            CardCvv.from(it).mapLeft { CreditCardError.InvalidCvv }.bind()
        }

        val id = existing?.card?.id ?: AccountId(UUID.randomUUID())
        val account = Account(
            id = id,
            name = name,
            asset = existing?.account?.asset ?: currencyRepository.getBaseCurrency(),
            color = draft.color,
            icon = existing?.account?.icon ?: IconAsset.unsafe(CardIconAsset),
            includeInBalance = false,
            orderNum = existing?.account?.orderNum ?: (accountRepository.findMaxOrderNum() + 1),
        )
        val card = CreditCard(
            id = id,
            cardholderName = draft.cardholderName?.let(NotBlankTrimmedString::from)?.getOrNull(),
            issuer = identity.issuer?.let(NotBlankTrimmedString::from)?.getOrNull(),
            network = identity.network,
            last4 = identity.last4,
            bin = identity.bin,
            expiry = expiry,
            creditLimit = creditLimit,
            billingDay = billingDay,
            dueDay = dueDay,
            repaymentAccountId = draft.repaymentAccountId,
            payeeVpa = draft.payeeVpa?.let(NotBlankTrimmedString::from)?.getOrNull(),
            tier = identity.tier?.let(NotBlankTrimmedString::from)?.getOrNull(),
            skin = draft.skin,
        )

        creditCardRepository.save(card)
        accountRepository.save(account)
        if (existing == null) {
            createOpeningTransactions(card, account, draft)
        }
        if (identity.pan != null) {
            secretsRepository.save(id, CardSecrets(pan = identity.pan, cvv = cvv))
                .mapLeft { CreditCardError.SecretsSaveFailed(it) }.bind()
        }

        CreditCardWithAccount(card = card, account = account)
    }

    private fun Raise<CreditCardError>.validateExpiry(draft: CreditCardDraft): YearMonth {
        ensure(draft.expiryMonth in FirstMonth..LastMonth) { CreditCardError.InvalidExpiry }
        val expiry = Either.catch { YearMonth.of(draft.expiryYear, draft.expiryMonth) }
            .mapLeft { CreditCardError.InvalidExpiry }.bind()
        ensure(!expiry.isBefore(YearMonth.from(timeProvider.localDateNow()))) {
            CreditCardError.ExpiredCard
        }
        return expiry
    }

    private suspend fun Raise<CreditCardError>.resolveIdentity(
        draft: CreditCardDraft,
        existing: CreditCard?,
    ): CardIdentity {
        val digits = draft.pan?.let(::normalizeCardNumber)
        return if (digits != null) {
            ensure(isValidLuhn(digits)) { CreditCardError.InvalidCardNumber }
            val pan = CardPan.from(digits).mapLeft { CreditCardError.InvalidCardNumber }.bind()
            val network = draft.networkOverride ?: CardNetworkDetector.detect(digits)
            val record = binLookup.lookup(digits)
            CardIdentity(
                pan = pan,
                last4 = CardLast4.unsafe(digits.takeLast(CardLast4.LENGTH)),
                bin = CardBin.from(digits.take(CardBin.LENGTH)).getOrNull(),
                network = network,
                issuer = draft.issuerOverride?.takeIf { it.isNotBlank() }
                    ?: record?.issuer
                    ?: defaultIssuerFor(network),
                tier = record?.tier,
            )
        } else {
            ensureNotNull(existing) { CreditCardError.CardNotFound }
            CardIdentity(
                pan = null,
                last4 = existing.last4,
                bin = existing.bin,
                network = draft.networkOverride ?: existing.network,
                issuer = draft.issuerOverride?.takeIf { it.isNotBlank() } ?: existing.issuer?.value,
                tier = existing.tier?.value,
            )
        }
    }

    private suspend fun createOpeningTransactions(
        card: CreditCard,
        account: Account,
        draft: CreditCardDraft,
    ) {
        val today = timeProvider.localDateNow()
        val dates = calculator.statementDates(card.billingDay, card.dueDay, today)
        var created = false
        if (draft.openingDue > 0.0) {
            // Local noon of the day before the statement date, so it is always billed.
            val billedTime = with(timeConverter) {
                dates.lastStatementDate.minusDays(1).atTime(LocalTime.NOON).toUTC()
            }
            transactionRepository.save(openingExpense(account, draft.openingDue, billedTime))
            created = true
        }
        if (draft.openingUnbilled > 0.0) {
            transactionRepository.save(
                openingExpense(account, draft.openingUnbilled, timeProvider.utcNow())
            )
            created = true
        }
        if (created) {
            dataObserver.post(DataWriteEvent.AllDataChange)
        }
    }

    private fun openingExpense(account: Account, amount: Double, time: Instant): Expense = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = NotBlankTrimmedString.unsafe(OpeningBalanceTitle),
        description = null,
        category = null,
        time = time,
        settled = true,
        metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanId = null,
            loanRecordId = null,
        ),
        tags = emptyList(),
        value = PositiveValue(amount = PositiveDouble.unsafe(amount), asset = account.asset),
        account = account.id,
    )

    private fun Double.isValidAmount(): Boolean = isFinite() && this >= 0.0

    private data class CardIdentity(
        val pan: CardPan?,
        val last4: CardLast4,
        val bin: CardBin?,
        val network: CardNetwork,
        val issuer: String?,
        val tier: String?,
    )
}
