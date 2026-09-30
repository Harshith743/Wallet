package com.ivy.creditcards.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewModelScope
import com.ivy.base.threading.DispatchersProvider
import com.ivy.creditcards.model.AccountChipUi
import com.ivy.data.model.AccountId
import com.ivy.data.model.CardNetwork
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.repository.AccountRepository
import com.ivy.design.IVY_COLOR_PICKER_COLORS_FREE
import com.ivy.domain.creditcard.BinLookup
import com.ivy.domain.creditcard.CardNetworkDetector
import com.ivy.domain.creditcard.defaultIssuerFor
import com.ivy.domain.creditcard.isValidLuhn
import com.ivy.domain.model.CreditCardWithAccount
import com.ivy.domain.usecase.creditcard.CreditCardDraft
import com.ivy.domain.usecase.creditcard.CreditCardError
import com.ivy.domain.usecase.creditcard.CreditCardsOverviewUseCase
import com.ivy.domain.usecase.creditcard.DeleteCreditCardUseCase
import com.ivy.domain.usecase.creditcard.SaveCreditCardUseCase
import com.ivy.legacy.utils.amountToDoubleOrNull
import com.ivy.legacy.utils.format
import com.ivy.navigation.Navigation
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.ui.theme.toComposeColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val MaxCardNumberDigits = 19
private const val ExpiryDigits = 4
private const val ExpiryMonthDigits = 2
private const val MaxCvvDigits = 4
private const val MaxDayDigits = 2
private const val CenturyBase = 2000
private const val YearInCentury = 100
private const val FirstMonth = 1
private const val LastMonth = 12
private const val FirstDay = 1
private const val LastDay = 31

@Stable
@HiltViewModel
class EditCreditCardViewModel @Inject constructor(
    private val saveCreditCardUseCase: SaveCreditCardUseCase,
    private val overviewUseCase: CreditCardsOverviewUseCase,
    private val deleteCreditCardUseCase: DeleteCreditCardUseCase,
    private val accountRepository: AccountRepository,
    private val binLookup: BinLookup,
    private val nav: Navigation,
    private val dispatchers: DispatchersProvider,
) : ComposeViewModel<EditCreditCardUiState, EditCreditCardUiEvent>() {

    private var existing: CreditCardWithAccount? = null
    private var currency = ""
    private var previousDetectedIssuer: String? = null

    private var isEdit by mutableStateOf(false)
    private var last4 by mutableStateOf<String?>(null)
    private var numberEntryVisible by mutableStateOf(true)
    private var cardNumber by mutableStateOf("")
    private var detectedNetwork by mutableStateOf(CardNetwork.UNKNOWN)
    private var networkOverride by mutableStateOf<CardNetwork?>(null)
    private var detectedIssuer by mutableStateOf<String?>(null)
    private var detectedTier by mutableStateOf<String?>(null)
    private var issuer by mutableStateOf("")
    private var cardholderName by mutableStateOf("")
    private var cardName by mutableStateOf("")
    private var expiry by mutableStateOf("")
    private var cvv by mutableStateOf("")
    private var creditLimit by mutableStateOf("")
    private var statementDay by mutableStateOf("")
    private var dueDay by mutableStateOf("")
    private var openingDue by mutableStateOf("")
    private var openingUnbilled by mutableStateOf("")
    private var color by mutableStateOf(IVY_COLOR_PICKER_COLORS_FREE.first())
    private var accounts by mutableStateOf<ImmutableList<AccountChipUi>>(persistentListOf())
    private var repaymentAccountId by mutableStateOf<AccountId?>(null)
    private var payeeVpa by mutableStateOf("")
    private var errors by mutableStateOf<ImmutableMap<CardField, FieldError>>(persistentMapOf())
    private var deleteDialogVisible by mutableStateOf(false)
    private var saving by mutableStateOf(false)
    private var loading by mutableStateOf(true)

    @Composable
    override fun uiState(): EditCreditCardUiState = EditCreditCardUiState(
        isEdit = isEdit,
        last4 = last4,
        numberEntryVisible = numberEntryVisible,
        cardNumber = cardNumber,
        detectedNetwork = detectedNetwork,
        networkOverride = networkOverride,
        detectedIssuer = detectedIssuer,
        detectedTier = detectedTier,
        issuer = issuer,
        cardholderName = cardholderName,
        cardName = cardName,
        expiry = expiry,
        cvv = cvv,
        creditLimit = creditLimit,
        statementDay = statementDay,
        dueDay = dueDay,
        openingDue = openingDue,
        openingUnbilled = openingUnbilled,
        color = color,
        palette = IVY_COLOR_PICKER_COLORS_FREE.toImmutableList(),
        accounts = accounts,
        repaymentAccountId = repaymentAccountId,
        payeeVpa = payeeVpa,
        errors = errors,
        deleteDialogVisible = deleteDialogVisible,
        saving = saving,
        loading = loading,
    )

    override fun onEvent(event: EditCreditCardUiEvent) {
        viewModelScope.launch(dispatchers.default) {
            when (event) {
                is EditCreditCardUiEvent.Load -> load(event.cardId)
                is EditCreditCardUiEvent.FieldChange -> fieldChange(event.field, event.value)
                is EditCreditCardUiEvent.NetworkOverride -> networkOverride = event.network
                is EditCreditCardUiEvent.ColorSelect -> color = event.color
                is EditCreditCardUiEvent.RepaymentAccountSelect -> repaymentAccountId = event.id
                EditCreditCardUiEvent.ReenterNumber -> numberEntryVisible = true
                EditCreditCardUiEvent.Save -> save()
                EditCreditCardUiEvent.DeleteClick -> deleteDialogVisible = true
                EditCreditCardUiEvent.DeleteConfirm -> delete()
                EditCreditCardUiEvent.DeleteDismiss -> deleteDialogVisible = false
            }
        }
    }

    private suspend fun load(cardId: UUID?) {
        val cardIds = overviewUseCase.creditCardIds()
        accounts = accountRepository.findAll()
            .filter { it.id !in cardIds }
            .map { AccountChipUi(id = it.id, name = it.name.value, color = it.color.value.toComposeColor()) }
            .toImmutableList()
        val loaded = cardId?.let { overviewUseCase.statement(AccountId(it)) }
        if (loaded != null) {
            existing = CreditCardWithAccount(card = loaded.card, account = loaded.account)
            currency = loaded.account.asset.code
            isEdit = true
            last4 = loaded.card.last4.value
            numberEntryVisible = false
            networkOverride = loaded.card.network
            issuer = loaded.card.issuer?.value.orEmpty()
            cardholderName = loaded.card.cardholderName?.value.orEmpty()
            cardName = loaded.account.name.value
            expiry = "%02d%02d".format(loaded.card.expiry.monthValue, loaded.card.expiry.year % YearInCentury)
            creditLimit = loaded.card.creditLimit.value.format(currency)
            statementDay = loaded.card.billingDay.value.toString()
            dueDay = loaded.card.dueDay.value.toString()
            color = loaded.account.color.value.toComposeColor()
            repaymentAccountId = loaded.card.repaymentAccountId
            payeeVpa = loaded.card.payeeVpa?.value.orEmpty()
        }
        loading = false
    }

    private suspend fun fieldChange(field: CardField, value: String) {
        errors = errors.toMutableMap().apply { remove(field) }.toImmutableMap()
        when (field) {
            CardField.NUMBER -> {
                cardNumber = value.filter { it.isDigit() }.take(MaxCardNumberDigits)
                detectedNetwork = CardNetworkDetector.detect(cardNumber)
                val record = binLookup.lookup(cardNumber)
                detectedIssuer = record?.issuer ?: defaultIssuerFor(detectedNetwork)
                detectedTier = record?.tier
                if (issuer.isBlank() || issuer == previousDetectedIssuer) {
                    issuer = detectedIssuer.orEmpty()
                }
                previousDetectedIssuer = detectedIssuer
            }

            CardField.HOLDER -> cardholderName = value
            CardField.NAME -> cardName = value
            CardField.ISSUER -> issuer = value
            CardField.EXPIRY -> expiry = value.filter { it.isDigit() }.take(ExpiryDigits)
            CardField.CVV -> cvv = value.filter { it.isDigit() }.take(MaxCvvDigits)
            CardField.LIMIT -> creditLimit = value
            CardField.STATEMENT_DAY -> statementDay = value.filter { it.isDigit() }.take(MaxDayDigits)
            CardField.DUE_DAY -> dueDay = value.filter { it.isDigit() }.take(MaxDayDigits)
            CardField.OPENING_DUE -> openingDue = value
            CardField.OPENING_UNBILLED -> openingUnbilled = value
            CardField.PAYEE_VPA -> payeeVpa = value
        }
    }

    private suspend fun save() {
        val validationErrors = validate()
        if (validationErrors.isNotEmpty()) {
            errors = validationErrors.toImmutableMap()
            return
        }
        saving = true
        val draft = CreditCardDraft(
            name = cardName,
            color = ColorInt(color.toArgb()),
            pan = cardNumber.takeIf { numberEntryVisible && it.isNotBlank() },
            cvv = cvv.takeIf { numberEntryVisible && it.isNotBlank() },
            cardholderName = cardholderName.takeIf { it.isNotBlank() },
            issuerOverride = issuer.takeIf { it.isNotBlank() },
            networkOverride = networkOverride,
            expiryMonth = expiry.take(ExpiryMonthDigits).toIntOrNull() ?: 0,
            expiryYear = CenturyBase + (expiry.drop(ExpiryMonthDigits).toIntOrNull() ?: 0),
            creditLimit = creditLimit.amountToDoubleOrNull() ?: 0.0,
            billingDay = statementDay.toIntOrNull() ?: 0,
            dueDay = dueDay.toIntOrNull() ?: 0,
            repaymentAccountId = repaymentAccountId,
            payeeVpa = payeeVpa.takeIf { it.isNotBlank() },
            openingDue = openingDue.amountToDoubleOrNull() ?: 0.0,
            openingUnbilled = openingUnbilled.amountToDoubleOrNull() ?: 0.0,
        )
        saveCreditCardUseCase.save(draft, existing).fold(
            ifLeft = { error ->
                saving = false
                errors = persistentMapOf(error.toField()).toImmutableMap()
            },
            ifRight = { nav.back() },
        )
    }

    private fun validate(): Map<CardField, FieldError> = buildMap {
        if (cardName.isBlank()) put(CardField.NAME, FieldError.REQUIRED)
        if (numberEntryVisible) {
            if (cardNumber.isBlank()) {
                put(CardField.NUMBER, FieldError.REQUIRED)
            } else if (!isValidLuhn(cardNumber)) {
                put(CardField.NUMBER, FieldError.INVALID_NUMBER)
            }
        }
        val month = expiry.take(ExpiryMonthDigits).toIntOrNull()
        if (expiry.length != ExpiryDigits || month == null || month !in FirstMonth..LastMonth) {
            put(CardField.EXPIRY, FieldError.INVALID_EXPIRY)
        }
        if ((creditLimit.amountToDoubleOrNull() ?: 0.0) <= 0.0) put(CardField.LIMIT, FieldError.INVALID_AMOUNT)
        if (!statementDay.isValidDay()) put(CardField.STATEMENT_DAY, FieldError.DAY_RANGE)
        if (!dueDay.isValidDay()) put(CardField.DUE_DAY, FieldError.DAY_RANGE)
        if (openingDue.isNotBlank() && openingDue.amountToDoubleOrNull() == null) {
            put(CardField.OPENING_DUE, FieldError.INVALID_AMOUNT)
        }
        if (openingUnbilled.isNotBlank() && openingUnbilled.amountToDoubleOrNull() == null) {
            put(CardField.OPENING_UNBILLED, FieldError.INVALID_AMOUNT)
        }
    }

    private fun String.isValidDay(): Boolean = toIntOrNull()?.let { it in FirstDay..LastDay } == true

    private fun CreditCardError.toField(): Pair<CardField, FieldError> = when (this) {
        CreditCardError.BlankName -> CardField.NAME to FieldError.REQUIRED
        CreditCardError.InvalidCardNumber, CreditCardError.CardNotFound -> CardField.NUMBER to FieldError.INVALID_NUMBER
        CreditCardError.InvalidCvv -> CardField.CVV to FieldError.INVALID_CVV
        CreditCardError.InvalidExpiry -> CardField.EXPIRY to FieldError.INVALID_EXPIRY
        CreditCardError.ExpiredCard -> CardField.EXPIRY to FieldError.EXPIRED
        CreditCardError.InvalidCreditLimit -> CardField.LIMIT to FieldError.INVALID_AMOUNT
        CreditCardError.InvalidBillingDay -> CardField.STATEMENT_DAY to FieldError.DAY_RANGE
        CreditCardError.InvalidDueDay -> CardField.DUE_DAY to FieldError.DAY_RANGE
        CreditCardError.InvalidOpeningAmount -> CardField.OPENING_DUE to FieldError.INVALID_AMOUNT
        is CreditCardError.SecretsSaveFailed -> CardField.NUMBER to FieldError.SECRETS
    }

    private suspend fun delete() {
        val id = existing?.card?.id ?: return
        deleteDialogVisible = false
        deleteCreditCardUseCase.delete(id)
        nav.back()
    }
}
