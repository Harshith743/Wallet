package com.ivy.domain.usecase.creditcard

import arrow.core.Either
import arrow.core.Some
import com.ivy.base.time.TimeProvider
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.CardNetwork
import com.ivy.data.model.CardSecrets
import com.ivy.data.model.CardSkinMode
import com.ivy.data.model.CreditCard
import com.ivy.data.model.Expense
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.CardCvv
import com.ivy.data.model.primitive.CardPan
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.testing.account
import com.ivy.data.model.testing.creditCard
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CreditCardRepository
import com.ivy.data.repository.CreditCardSecretsRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.creditcard.BinLookup
import com.ivy.domain.creditcard.BinRecord
import com.ivy.domain.model.CreditCardWithAccount
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.UUID

class SaveCreditCardUseCaseTest {
    private val accountRepository = mockk<AccountRepository>(relaxed = true)
    private val creditCardRepository = mockk<CreditCardRepository>(relaxed = true)
    private val secretsRepository = mockk<CreditCardSecretsRepository>()
    private val transactionRepository = mockk<TransactionRepository>(relaxed = true)
    private val currencyRepository = mockk<CurrencyRepository>()
    private val timeProvider = mockk<TimeProvider>()
    private val dataObserver = mockk<DataObserver>(relaxed = true)
    private val timeConverter = TestTimeConverter()
    private val binLookup = mockk<BinLookup>()

    private val today = LocalDate.of(2026, 9, 10)
    private val now = today.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC)
    private val inr = AssetCode.unsafe("INR")

    private lateinit var useCase: SaveCreditCardUseCase

    @Before
    fun setup() {
        coEvery { accountRepository.findMaxOrderNum() } returns 3.0
        coEvery { binLookup.lookup(any()) } returns null
        coEvery { secretsRepository.save(any(), any()) } returns Either.Right(Unit)
        coEvery { currencyRepository.getBaseCurrency() } returns inr
        every { timeProvider.localDateNow() } returns today
        every { timeProvider.utcNow() } returns now
        useCase = SaveCreditCardUseCase(
            accountRepository = accountRepository,
            creditCardRepository = creditCardRepository,
            secretsRepository = secretsRepository,
            transactionRepository = transactionRepository,
            currencyRepository = currencyRepository,
            calculator = CreditCardStatementCalculator(timeConverter),
            timeProvider = timeProvider,
            timeConverter = timeConverter,
            dataObserver = dataObserver,
            binLookup = binLookup,
        )
    }

    @Test
    fun `new card - creates account and card with the same id, card first`() = runTest {
        val draft = validDraft()

        val result = useCase.save(draft, existing = null).shouldBeRight()

        result.card.id shouldBe result.account.id
        result.account.name.value shouldBe "HDFC Pixel"
        result.account.asset shouldBe inr
        result.account.includeInBalance shouldBe false
        result.account.orderNum shouldBe 4.0
        result.account.icon?.id shouldBe "ic_vue_money_card"
        result.card.last4.value shouldBe "1111"
        result.card.bin?.value shouldBe "411111"
        result.card.network shouldBe CardNetwork.VISA
        result.card.creditLimit.value shouldBe 36_000.0
        result.card.billingDay.value shouldBe 5
        result.card.dueDay.value shouldBe 25
        coVerifyOrder {
            creditCardRepository.save(result.card)
            accountRepository.save(result.account)
        }
        coVerify {
            secretsRepository.save(
                result.card.id,
                CardSecrets(pan = CardPan.unsafe("4111111111111111"), cvv = CardCvv.unsafe("123"))
            )
        }
    }

    @Test
    fun `new card - opening amounts become adjustment expenses`() = runTest {
        val draft = validDraft().copy(openingDue = 1_000.0, openingUnbilled = 250.0)
        val saved = mutableListOf<Expense>()
        coEvery { transactionRepository.save(capture(saved)) } returns Unit

        val result = useCase.save(draft, existing = null).shouldBeRight()

        saved.size shouldBe 2
        // statement day 5 -> S = Sep 5; opening due is dated Sep 4 at noon
        saved[0].value.amount.value shouldBe 1_000.0
        saved[0].time shouldBe LocalDate.of(2026, 9, 4).atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC)
        saved[1].value.amount.value shouldBe 250.0
        saved[1].time shouldBe now
        saved.all { it.account == result.card.id && it.settled && it.category == null } shouldBe true
        coVerify { dataObserver.post(DataWriteEvent.AllDataChange) }
    }

    @Test
    fun `new card - zero opening amounts create no transactions`() = runTest {
        useCase.save(validDraft(), existing = null).shouldBeRight()

        coVerify(exactly = 0) { transactionRepository.save(any()) }
    }

    @Test
    fun `validation errors`() = runTest {
        useCase.save(validDraft().copy(name = "  "), null).shouldBeLeft() shouldBe CreditCardError.BlankName
        useCase.save(validDraft().copy(pan = "4111111111111112"), null)
            .shouldBeLeft() shouldBe CreditCardError.InvalidCardNumber
        useCase.save(validDraft().copy(expiryMonth = 13), null)
            .shouldBeLeft() shouldBe CreditCardError.InvalidExpiry
        useCase.save(validDraft().copy(expiryMonth = 8, expiryYear = 2026), null)
            .shouldBeLeft() shouldBe CreditCardError.ExpiredCard
        useCase.save(validDraft().copy(creditLimit = 0.0), null)
            .shouldBeLeft() shouldBe CreditCardError.InvalidCreditLimit
        useCase.save(validDraft().copy(billingDay = 0), null)
            .shouldBeLeft() shouldBe CreditCardError.InvalidBillingDay
        useCase.save(validDraft().copy(dueDay = 32), null)
            .shouldBeLeft() shouldBe CreditCardError.InvalidDueDay
        useCase.save(validDraft().copy(openingDue = -1.0), null)
            .shouldBeLeft() shouldBe CreditCardError.InvalidOpeningAmount
        useCase.save(validDraft().copy(cvv = "12"), null)
            .shouldBeLeft() shouldBe CreditCardError.InvalidCvv
        useCase.save(validDraft().copy(pan = null), null)
            .shouldBeLeft() shouldBe CreditCardError.CardNotFound
    }

    @Test
    fun `expiry in the current month is still valid`() = runTest {
        useCase.save(validDraft().copy(expiryMonth = 9, expiryYear = 2026), null).shouldBeRight()
    }

    @Test
    fun `the BIN dataset fills the issuer and tier`() = runTest {
        coEvery { binLookup.lookup("4111111111111111") } returns BinRecord(
            bin = "411111", brand = "VISA", cardType = "CREDIT", tier = "Platinum", issuer = "HDFC Bank",
        )

        val result = useCase.save(validDraft(), existing = null).shouldBeRight()

        result.card.issuer?.value shouldBe "HDFC Bank"
        result.card.tier?.value shouldBe "Platinum"
        result.card.skin shouldBe CardSkinMode.AUTO
    }

    @Test
    fun `the chosen design is stored`() = runTest {
        val result = useCase.save(validDraft().copy(skin = CardSkinMode.COLOR), existing = null).shouldBeRight()

        result.card.skin shouldBe CardSkinMode.COLOR
    }

    @Test
    fun `overrides win over detection`() = runTest {
        val draft = validDraft().copy(networkOverride = CardNetwork.RUPAY, issuerOverride = "My Bank")

        val result = useCase.save(draft, existing = null).shouldBeRight()

        result.card.network shouldBe CardNetwork.RUPAY
        result.card.issuer?.value shouldBe "My Bank"
    }

    @Test
    fun `edit without re-entering the number keeps identity and secrets`() = runTest {
        val id = AccountId(UUID.randomUUID())
        val existingCard: CreditCard = Arb.creditCard(id = Some(id)).next()
        val existingAccount: Account = Arb.account(accountId = Some(id), orderNum = Some(7.0)).next()
        val draft = validDraft().copy(pan = null, cvv = null, name = "Renamed", color = ColorInt(42))

        val result = useCase.save(draft, CreditCardWithAccount(existingCard, existingAccount)).shouldBeRight()

        result.card.id shouldBe id
        result.card.last4 shouldBe existingCard.last4
        result.card.bin shouldBe existingCard.bin
        result.card.network shouldBe existingCard.network
        result.card.tier shouldBe existingCard.tier
        result.card.skin shouldBe CardSkinMode.AUTO
        result.account.name.value shouldBe "Renamed"
        result.account.color shouldBe ColorInt(42)
        result.account.orderNum shouldBe 7.0
        result.account.asset shouldBe existingAccount.asset
        coVerify(exactly = 0) { secretsRepository.save(any(), any()) }
        coVerify(exactly = 0) { transactionRepository.save(any()) }
    }

    private fun validDraft() = CreditCardDraft(
        name = "HDFC Pixel",
        color = ColorInt(1),
        pan = "4111 1111 1111 1111",
        cvv = "123",
        cardholderName = "Harshith",
        issuerOverride = null,
        networkOverride = null,
        expiryMonth = 12,
        expiryYear = 2030,
        creditLimit = 36_000.0,
        billingDay = 5,
        dueDay = 25,
        repaymentAccountId = null,
        payeeVpa = null,
        openingDue = 0.0,
        openingUnbilled = 0.0,
        skin = CardSkinMode.AUTO,
    )
}
