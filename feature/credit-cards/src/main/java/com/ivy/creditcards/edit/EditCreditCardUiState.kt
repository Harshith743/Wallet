@file:Suppress("DataClassTypedIDs") // the rule does not recognise the app's typed ids

package com.ivy.creditcards.edit

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.ivy.creditcards.model.AccountChipUi
import com.ivy.creditcards.skin.CardSkinUi
import com.ivy.data.model.AccountId
import com.ivy.data.model.CardNetwork
import com.ivy.data.model.CardSkinMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import java.util.UUID

enum class CardField {
    NUMBER,
    HOLDER,
    NAME,
    ISSUER,
    EXPIRY,
    CVV,
    LIMIT,
    STATEMENT_DAY,
    DUE_DAY,
    OPENING_DUE,
    OPENING_UNBILLED,
    PAYEE_VPA,
    DESIGN,
}

enum class FieldError {
    REQUIRED,
    INVALID_NUMBER,
    INVALID_EXPIRY,
    EXPIRED,
    INVALID_CVV,
    INVALID_AMOUNT,
    DAY_RANGE,
    SECRETS,
    PHOTO,
}

@Immutable
data class EditCreditCardUiState(
    val isEdit: Boolean,
    val last4: String?,
    val numberEntryVisible: Boolean,
    val cardNumber: String,
    val detectedNetwork: CardNetwork,
    val networkOverride: CardNetwork?,
    val detectedIssuer: String?,
    val detectedTier: String?,
    val issuer: String,
    val cardholderName: String,
    val cardName: String,
    val expiry: String,
    val cvv: String,
    val creditLimit: String,
    val statementDay: String,
    val dueDay: String,
    val openingDue: String,
    val openingUnbilled: String,
    val color: Color,
    val palette: ImmutableList<Color>,
    /** AUTO (bank theme), COLOR (the swatch) or IMAGE (photo); the preview shows what AUTO would paint. */
    val skinMode: CardSkinMode,
    val skinPreview: CardSkinUi,
    /** The staged or stored photo to show in the editor, if any. */
    val photoPath: String?,
    /** Photo mode is selected but no photo exists on this device. */
    val photoMissing: Boolean,
    val accounts: ImmutableList<AccountChipUi>,
    val repaymentAccountId: AccountId?,
    val payeeVpa: String,
    val errors: ImmutableMap<CardField, FieldError>,
    val deleteDialogVisible: Boolean,
    val saving: Boolean,
    val loading: Boolean,
) {
    val network: CardNetwork
        get() = networkOverride ?: detectedNetwork
}

sealed interface EditCreditCardUiEvent {
    data class Load(val cardId: UUID?) : EditCreditCardUiEvent
    data class FieldChange(val field: CardField, val value: String) : EditCreditCardUiEvent
    data class NetworkOverride(val network: CardNetwork?) : EditCreditCardUiEvent
    data class ColorSelect(val color: Color) : EditCreditCardUiEvent
    data class SkinModeSelect(val mode: CardSkinMode) : EditCreditCardUiEvent
    data class PhotoPicked(val uri: Uri) : EditCreditCardUiEvent
    data object PhotoRemove : EditCreditCardUiEvent
    data class RepaymentAccountSelect(val id: AccountId?) : EditCreditCardUiEvent
    data object ReenterNumber : EditCreditCardUiEvent
    data object Save : EditCreditCardUiEvent
    data object DeleteClick : EditCreditCardUiEvent
    data object DeleteConfirm : EditCreditCardUiEvent
    data object DeleteDismiss : EditCreditCardUiEvent
}
