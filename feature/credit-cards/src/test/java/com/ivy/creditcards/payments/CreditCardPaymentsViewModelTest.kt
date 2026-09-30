package com.ivy.creditcards.payments

import arrow.core.Some
import com.ivy.base.TestDispatchersProvider
import com.ivy.creditcards.model.PaymentUi
import com.ivy.creditcards.model.PaymentUiMapper
import com.ivy.data.DataObserver
import com.ivy.data.model.AccountId
import com.ivy.data.model.TransactionId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.testing.account
import com.ivy.data.repository.AccountRepository
import com.ivy.domain.usecase.creditcard.CreditCardPaymentsUseCase
import com.ivy.domain.usecase.creditcard.CreditCardRepayment
import com.ivy.ui.testing.ComposeViewModelTest
import com.ivy.ui.testing.runTest
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import java.util.UUID

class CreditCardPaymentsViewModelTest : ComposeViewModelTest() {
    private val paymentsUseCase = mockk<CreditCardPaymentsUseCase>()
    private val accountRepository = mockk<AccountRepository>()
    private val paymentUiMapper = mockk<PaymentUiMapper>()

    private val cardId = AccountId(UUID.randomUUID())
    private val inr = AssetCode.unsafe("INR")

    private val account = Arb.account(accountId = Some(cardId), asset = Some(inr)).next()

    private lateinit var viewModel: CreditCardPaymentsViewModel

    @Before
    fun setup() {
        coEvery { accountRepository.findById(cardId) } returns account
        viewModel = CreditCardPaymentsViewModel(
            paymentsUseCase = paymentsUseCase,
            accountRepository = accountRepository,
            paymentUiMapper = paymentUiMapper,
            dataObserver = DataObserver(),
            dispatchers = TestDispatchersProvider,
        )
    }

    @Test
    fun `load maps the history with the card's currency`() {
        val repayment = mockk<CreditCardRepayment>()
        val ui = PaymentUi(
            id = TransactionId(UUID.randomUUID()),
            dateText = "Tue, Sep 8 2026",
            amountText = "₹500.00",
            fromAccountName = "Bank",
            note = null,
        )
        coEvery { paymentsUseCase.history(cardId) } returns listOf(repayment)
        every { paymentUiMapper.map(repayment, "INR") } returns ui

        viewModel.runTest(events = listOf(CreditCardPaymentsUiEvent.Load(cardId.value))) {
            payments shouldBe listOf(ui)
            cardName shouldBe account.name.value
            loading shouldBe false
        }
    }

    @Test
    fun `empty history`() {
        coEvery { paymentsUseCase.history(cardId) } returns emptyList()

        viewModel.runTest(events = listOf(CreditCardPaymentsUiEvent.Load(cardId.value))) {
            payments.isEmpty() shouldBe true
            loading shouldBe false
        }
    }
}
