package com.ivy.creditcards.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.creditcards.CreditCardsContent
import com.ivy.creditcards.CreditCardsNavigation
import com.ivy.creditcards.details.CreditCardDetailsUi
import com.ivy.creditcards.edit.EditCreditCardUi
import com.ivy.creditcards.payments.CreditCardPaymentsUi
import com.ivy.legacy.IvyWalletPreview
import com.ivy.navigation.IvyPreview
import kotlinx.collections.immutable.persistentListOf
import com.ivy.base.legacy.Theme as LegacyTheme

private val noNavigation = CreditCardsNavigation(
    onAddCard = {},
    onViewDetails = {},
    onEditCard = {},
    onRecentSpends = {},
    onPaymentHistory = {},
)

/** For screenshot testing: the cards segment as shown inside the Accounts tab. */
@Composable
fun CreditCardsContentUiTest(
    dark: Boolean,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    empty: Boolean = false,
    revealed: Boolean = false,
) {
    val cards = if (empty) persistentListOf() else CreditCardsPreviewData.cards
    IvyWalletPreview(theme = if (dark) LegacyTheme.DARK else LegacyTheme.LIGHT) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            CreditCardsContent(
                state = CreditCardsPreviewData.cardsState(
                    cards = cards,
                    expanded = expanded,
                    revealedCardId = if (revealed) cards.firstOrNull()?.id else null,
                ),
                onEvent = {},
                navigation = noNavigation,
            )
        }
    }
}

/** For screenshot testing. */
@Composable
fun EditCreditCardUiTest(
    dark: Boolean,
    isEdit: Boolean,
    withErrors: Boolean = false,
    colourDesign: Boolean = false,
    photoMissing: Boolean = false,
) {
    IvyPreview(dark = dark) {
        EditCreditCardUi(
            state = CreditCardsPreviewData.editState(
                isEdit = isEdit,
                withErrors = withErrors,
                colourDesign = colourDesign,
                photoMissing = photoMissing,
            ),
            onEvent = {},
        )
    }
}

/** For screenshot testing. */
@Composable
fun CreditCardPaymentsUiTest(dark: Boolean, empty: Boolean) {
    IvyPreview(dark = dark) {
        CreditCardPaymentsUi(state = CreditCardsPreviewData.paymentsState(empty = empty))
    }
}

/** For screenshot testing. */
@Composable
fun CreditCardDetailsUiTest(dark: Boolean, revealed: Boolean) {
    IvyPreview(dark = dark) {
        CreditCardDetailsUi(
            state = CreditCardsPreviewData.detailsState(revealed = revealed),
            onEvent = {},
        )
    }
}

@Preview
@Composable
private fun PreviewCardsCollapsed() {
    CreditCardsContentUiTest(dark = false, expanded = false)
}

@Preview
@Composable
private fun PreviewCardsExpandedDark() {
    CreditCardsContentUiTest(dark = true, expanded = true)
}

@Preview
@Composable
private fun PreviewCardsRevealed() {
    CreditCardsContentUiTest(dark = false, expanded = false, revealed = true)
}

@Preview
@Composable
private fun PreviewEdit() {
    EditCreditCardUiTest(dark = false, isEdit = false)
}

@Preview
@Composable
private fun PreviewDetails() {
    CreditCardDetailsUiTest(dark = false, revealed = true)
}
