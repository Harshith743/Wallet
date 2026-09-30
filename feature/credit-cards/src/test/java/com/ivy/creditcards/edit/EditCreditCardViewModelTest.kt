package com.ivy.creditcards.edit

import arrow.core.Either
import com.ivy.base.TestDispatchersProvider
import com.ivy.data.model.CardNetwork
import com.ivy.data.model.CardSkinMode
import com.ivy.data.repository.AccountRepository
import com.ivy.data.skin.fake.FakeCardSkinImageStore
import com.ivy.domain.creditcard.BinLookup
import com.ivy.domain.creditcard.BinRecord
import com.ivy.domain.usecase.creditcard.CreditCardDraft
import com.ivy.domain.usecase.creditcard.CreditCardError
import com.ivy.domain.usecase.creditcard.CreditCardsOverviewUseCase
import com.ivy.domain.usecase.creditcard.DeleteCreditCardUseCase
import com.ivy.domain.usecase.creditcard.SaveCreditCardUseCase
import com.ivy.navigation.Navigation
import com.ivy.ui.testing.ComposeViewModelTest
import com.ivy.ui.testing.runTest
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class EditCreditCardViewModelTest : ComposeViewModelTest() {
    private val saveUseCase = mockk<SaveCreditCardUseCase>()
    private val overviewUseCase = mockk<CreditCardsOverviewUseCase>()
    private val deleteUseCase = mockk<DeleteCreditCardUseCase>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>()
    private val binLookup = mockk<BinLookup>()
    private val imageStore = FakeCardSkinImageStore()
    private val nav = mockk<Navigation>(relaxed = true)

    private lateinit var viewModel: EditCreditCardViewModel

    @Before
    fun setup() {
        coEvery { overviewUseCase.creditCardIds() } returns emptySet()
        coEvery { accountRepository.findAll() } returns emptyList()
        coEvery { binLookup.lookup(any()) } returns null
        every { nav.back() } returns true
        viewModel = EditCreditCardViewModel(
            saveCreditCardUseCase = saveUseCase,
            overviewUseCase = overviewUseCase,
            deleteCreditCardUseCase = deleteUseCase,
            accountRepository = accountRepository,
            binLookup = binLookup,
            imageStore = imageStore,
            nav = nav,
            dispatchers = TestDispatchersProvider,
        )
    }

    @Test
    fun `typing a number keeps digits only and detects the network`() {
        viewModel.runTest(
            events = listOf(
                EditCreditCardUiEvent.Load(null),
                EditCreditCardUiEvent.FieldChange(CardField.NUMBER, "5555 5555 5555 4444x"),
            )
        ) {
            cardNumber shouldBe "5555555555554444"
            detectedNetwork shouldBe CardNetwork.MASTERCARD
            network shouldBe CardNetwork.MASTERCARD
            isEdit shouldBe false
            loading shouldBe false
        }
    }

    @Test
    fun `a known BIN fills the issuer and tier, a typed issuer is kept`() {
        coEvery { binLookup.lookup("461786") } returns BinRecord(
            bin = "461786", brand = "VISA", cardType = "CREDIT", tier = "Platinum", issuer = "HDFC Bank",
        )
        viewModel.runTest(
            events = listOf(
                EditCreditCardUiEvent.Load(null),
                EditCreditCardUiEvent.FieldChange(CardField.NUMBER, "461786"),
            )
        ) {
            detectedIssuer shouldBe "HDFC Bank"
            detectedTier shouldBe "Platinum"
            issuer shouldBe "HDFC Bank"
        }
        viewModel.runTest(
            events = listOf(
                EditCreditCardUiEvent.FieldChange(CardField.ISSUER, "My Bank"),
                EditCreditCardUiEvent.FieldChange(CardField.NUMBER, "4617861"),
            )
        ) {
            issuer shouldBe "My Bank"
        }
    }

    @Test
    fun `an unknown Amex number defaults the issuer to the network`() {
        viewModel.runTest(
            events = listOf(
                EditCreditCardUiEvent.Load(null),
                EditCreditCardUiEvent.FieldChange(CardField.NUMBER, "378282246310005"),
            )
        ) {
            detectedIssuer shouldBe "American Express"
            detectedTier shouldBe null
        }
    }

    @Test
    fun `design defaults to the bank theme and the preview follows the typed issuer`() {
        viewModel.runTest(
            events = listOf(
                EditCreditCardUiEvent.Load(null),
                EditCreditCardUiEvent.FieldChange(CardField.ISSUER, "HDFC Bank"),
            )
        ) {
            skinMode shouldBe CardSkinMode.AUTO
            skinPreview.wordmark shouldBe "HDFC BANK"
        }
        viewModel.runTest(events = listOf(EditCreditCardUiEvent.SkinModeSelect(CardSkinMode.COLOR))) {
            skinMode shouldBe CardSkinMode.COLOR
        }
    }

    @Test
    fun `picking a photo stages it and selects the photo design, removing it goes back to auto`() {
        val uri = mockk<android.net.Uri>()
        viewModel.runTest(events = listOf(EditCreditCardUiEvent.Load(null), EditCreditCardUiEvent.PhotoPicked(uri))) {
            skinMode shouldBe CardSkinMode.IMAGE
            photoPath shouldBe "staging-1.jpg"
            photoMissing shouldBe false
        }
        viewModel.runTest(events = listOf(EditCreditCardUiEvent.PhotoRemove)) {
            skinMode shouldBe CardSkinMode.AUTO
            photoPath shouldBe null
        }
        imageStore.hasStaged() shouldBe false
    }

    @Test
    fun `a failed photo import reports a design error and keeps the mode`() {
        imageStore.stageFails = true
        viewModel.runTest(
            events = listOf(EditCreditCardUiEvent.Load(null), EditCreditCardUiEvent.PhotoPicked(mockk()))
        ) {
            skinMode shouldBe CardSkinMode.AUTO
            errors[CardField.DESIGN] shouldBe FieldError.PHOTO
        }
    }

    @Test
    fun `save with an empty form reports the required fields`() {
        viewModel.runTest(events = listOf(EditCreditCardUiEvent.Load(null), EditCreditCardUiEvent.Save)) {
            errors[CardField.NAME] shouldBe FieldError.REQUIRED
            errors[CardField.NUMBER] shouldBe FieldError.REQUIRED
            errors[CardField.EXPIRY] shouldBe FieldError.INVALID_EXPIRY
            errors[CardField.LIMIT] shouldBe FieldError.INVALID_AMOUNT
            errors[CardField.STATEMENT_DAY] shouldBe FieldError.DAY_RANGE
            errors[CardField.DUE_DAY] shouldBe FieldError.DAY_RANGE
        }
        coVerify(exactly = 0) { saveUseCase.save(any(), any()) }
    }

    @Test
    fun `save with a valid form calls the use case and goes back`() {
        val draft = slot<CreditCardDraft>()
        coEvery { saveUseCase.save(capture(draft), null) } returns Either.Right(mockk())

        viewModel.runTest(events = loadAndFill() + EditCreditCardUiEvent.Save) {
            errors.isEmpty() shouldBe true
        }

        draft.captured.pan shouldBe "4111111111111111"
        draft.captured.cvv shouldBe "123"
        draft.captured.expiryMonth shouldBe 12
        draft.captured.expiryYear shouldBe 2030
        draft.captured.creditLimit shouldBe 36000.0
        draft.captured.billingDay shouldBe 5
        draft.captured.dueDay shouldBe 25
        draft.captured.name shouldBe "HDFC Pixel"
        verify { nav.back() }
    }

    @Test
    fun `use case errors are mapped onto fields`() {
        coEvery { saveUseCase.save(any(), null) } returns Either.Left(CreditCardError.ExpiredCard)

        viewModel.runTest(events = loadAndFill() + EditCreditCardUiEvent.Save) {
            errors[CardField.EXPIRY] shouldBe FieldError.EXPIRED
            saving shouldBe false
        }
    }

    private fun loadAndFill(): List<EditCreditCardUiEvent> =
        listOf(EditCreditCardUiEvent.Load(null)) + validFormEvents()

    private fun validFormEvents() = listOf(
        EditCreditCardUiEvent.FieldChange(CardField.NAME, "HDFC Pixel"),
        EditCreditCardUiEvent.FieldChange(CardField.NUMBER, "4111111111111111"),
        EditCreditCardUiEvent.FieldChange(CardField.CVV, "123"),
        EditCreditCardUiEvent.FieldChange(CardField.EXPIRY, "1230"),
        EditCreditCardUiEvent.FieldChange(CardField.LIMIT, "36000"),
        EditCreditCardUiEvent.FieldChange(CardField.STATEMENT_DAY, "5"),
        EditCreditCardUiEvent.FieldChange(CardField.DUE_DAY, "25"),
    )
}
