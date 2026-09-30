package com.ivy.creditcards

import arrow.core.Either
import arrow.core.Some
import com.ivy.base.TestDispatchersProvider
import com.ivy.creditcards.model.CreditCardUiMapper
import com.ivy.creditcards.model.StatementLabelMapper
import com.ivy.creditcards.session.AccountsSegmentSession
import com.ivy.data.DataObserver
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.DayOfMonth
import com.ivy.data.model.primitive.NonNegativeDouble
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.model.testing.account
import com.ivy.data.model.testing.creditCard
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.domain.model.CreditCardStatement
import com.ivy.domain.model.CreditCardWithStatement
import com.ivy.domain.model.StatementDates
import com.ivy.domain.model.StatementStatus
import com.ivy.domain.usecase.creditcard.CreditCardsOverview
import com.ivy.domain.usecase.creditcard.CreditCardsOverviewUseCase
import com.ivy.domain.usecase.creditcard.DeleteCreditCardUseCase
import com.ivy.domain.usecase.creditcard.RecordCreditCardPaymentUseCase
import com.ivy.ui.testing.ComposeViewModelTest
import com.ivy.ui.testing.runTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class CreditCardsViewModelTest : ComposeViewModelTest() {
    private val overviewUseCase = mockk<CreditCardsOverviewUseCase>()
    private val recordPaymentUseCase = mockk<RecordCreditCardPaymentUseCase>()
    private val deleteCreditCardUseCase = mockk<DeleteCreditCardUseCase>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>()
    private val currencyRepository = mockk<CurrencyRepository>()
    private val session = AccountsSegmentSession()

    private val inr = AssetCode.unsafe("INR")
    private val firstId = AccountId(UUID.randomUUID())
    private val secondId = AccountId(UUID.randomUUID())
    private val bankId = AccountId(UUID.randomUUID())
    private val bank: Account = Arb.account(accountId = Some(bankId), asset = Some(inr)).next()

    private lateinit var viewModel: CreditCardsViewModel

    @Before
    fun setup() {
        coEvery { currencyRepository.getBaseCurrency() } returns inr
        coEvery { overviewUseCase.overview() } returns CreditCardsOverview(
            cards = listOf(card(firstId, due = 1_000.0, repaymentAccount = bankId), card(secondId, due = 0.0)),
            totalDue = 1_000.0,
            cardsWithDueCount = 1,
        )
        coEvery { accountRepository.findAll() } returns listOf(
            bank,
            Arb.account(accountId = Some(firstId), asset = Some(inr)).next(),
        )
        viewModel = CreditCardsViewModel(
            overviewUseCase = overviewUseCase,
            recordPaymentUseCase = recordPaymentUseCase,
            deleteCreditCardUseCase = deleteCreditCardUseCase,
            accountRepository = accountRepository,
            currencyRepository = currencyRepository,
            uiMapper = CreditCardUiMapper(StatementLabelMapper()),
            session = session,
            dataObserver = DataObserver(),
            dispatchers = TestDispatchersProvider,
        )
    }

    @Test
    fun `loads cards, first card active, totals`() {
        viewModel.runTest {
            cards.size shouldBe 2
            activeCardId shouldBe firstId
            dueCardsCount shouldBe 1
            loading shouldBe false
            cards[0].dueAmount shouldBe 1_000.0
        }
    }

    @Test
    fun `selecting a card makes it active, collapses and remembers it in the session`() {
        viewModel.runTest(
            events = listOf(CreditCardsUiEvent.ToggleExpanded, CreditCardsUiEvent.SelectCard(secondId))
        ) {
            activeCardId shouldBe secondId
            expanded shouldBe false
            session.activeCreditCardId shouldBe secondId
        }
    }

    @Test
    fun `expanding via swipe up closes any reveal`() {
        viewModel.runTest(
            events = listOf(CreditCardsUiEvent.Reveal(firstId), CreditCardsUiEvent.ExpandStack)
        ) {
            expanded shouldBe true
            revealedCardId shouldBe null
        }
    }

    @Test
    fun `reveal is exclusive and a stale close is ignored`() {
        viewModel.runTest(
            events = listOf(
                CreditCardsUiEvent.Reveal(firstId),
                CreditCardsUiEvent.Reveal(secondId),
                CreditCardsUiEvent.CloseReveal(firstId),
            )
        ) {
            revealedCardId shouldBe secondId
        }
        viewModel.runTest(events = listOf(CreditCardsUiEvent.CloseReveal(null))) {
            revealedCardId shouldBe null
        }
    }

    @Test
    fun `selecting a card and mark as paid close the reveal`() {
        viewModel.runTest(
            events = listOf(CreditCardsUiEvent.Reveal(firstId), CreditCardsUiEvent.SelectCard(secondId))
        ) {
            revealedCardId shouldBe null
        }
        viewModel.runTest(
            events = listOf(CreditCardsUiEvent.Reveal(firstId), CreditCardsUiEvent.MarkAsPaidClick(firstId))
        ) {
            revealedCardId shouldBe null
            paySheet shouldNotBe null
        }
    }

    @Test
    fun `pay now opens the sheet prefilled with the due and the repayment account, cards excluded`() {
        viewModel.runTest(events = listOf(CreditCardsUiEvent.PayNowClick(firstId))) {
            val sheet = paySheet!!
            sheet.launchUpi shouldBe true
            sheet.selectedAccountId shouldBe bankId
            sheet.accounts.map { it.id } shouldBe listOf(bankId)
            sheet.amountText shouldBe "1,000.00"
        }
    }

    @Test
    fun `confirm without an account or with a bad amount shows errors`() {
        viewModel.runTest(
            events = listOf(
                CreditCardsUiEvent.MarkAsPaidClick(secondId),
                CreditCardsUiEvent.PaySheetAmountChange("abc"),
                CreditCardsUiEvent.PaySheetConfirm,
            )
        ) {
            paySheet!!.amountError shouldBe true
            paySheet!!.accountError shouldBe true
        }
        coVerify(exactly = 0) { recordPaymentUseCase.record(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `confirm records the payment, closes the sheet and requests the UPI launch`() {
        coEvery { recordPaymentUseCase.record(any(), any(), any(), any(), any()) } returns Either.Right(mockk())

        viewModel.runTest(
            events = listOf(
                CreditCardsUiEvent.PayNowClick(firstId),
                CreditCardsUiEvent.PaySheetAmountChange("500"),
                CreditCardsUiEvent.PaySheetNoteChange("part payment"),
                CreditCardsUiEvent.PaySheetConfirm,
            )
        ) {
            paySheet shouldBe null
            pendingUpi?.amount shouldBe 500.0
            pendingUpi?.note shouldBe "part payment"
        }
        coVerify {
            recordPaymentUseCase.record(firstId, PositiveDouble.unsafe(500.0), bankId, null, "part payment")
        }
    }

    @Test
    fun `delete confirm calls the use case`() {
        viewModel.runTest(
            events = listOf(CreditCardsUiEvent.DeleteClick(firstId), CreditCardsUiEvent.DeleteConfirm)
        ) {
            deleteConfirmCardId shouldBe null
        }
        coVerify { deleteCreditCardUseCase.delete(firstId) }
    }

    private fun card(id: AccountId, due: Double, repaymentAccount: AccountId? = null): CreditCardWithStatement {
        val card = Arb.creditCard(
            id = Some(id),
            billingDay = Some(DayOfMonth.unsafe(5)),
            dueDay = Some(DayOfMonth.unsafe(25)),
            creditLimit = Some(PositiveDouble.unsafe(36_000.0)),
            repaymentAccountId = Some(repaymentAccount),
        ).next()
        val dates = StatementDates(
            lastStatementDate = LocalDate.of(2026, 9, 5),
            dueDate = LocalDate.of(2026, 9, 25),
            nextStatementDate = LocalDate.of(2026, 10, 5),
        )
        return CreditCardWithStatement(
            card = card,
            account = Arb.account(accountId = Some(id), asset = Some(inr)).next(),
            statement = CreditCardStatement(
                due = NonNegativeDouble.unsafe(due),
                unbilled = NonNegativeDouble.unsafe(0.0),
                outstanding = due,
                availableLimit = 36_000.0 - due,
                dates = dates,
                lastPaidOn = null,
                status = if (due > 0) {
                    StatementStatus.Due(NonNegativeDouble.unsafe(due), dates.dueDate, 10)
                } else {
                    StatementStatus.StatementAwaited(dates.nextStatementDate)
                },
            ),
        )
    }
}
