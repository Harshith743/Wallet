package com.ivy.creditcards.model

import com.ivy.creditcards.skin.CardSkinResolver
import com.ivy.domain.model.CreditCardWithStatement
import com.ivy.wallet.ui.theme.toComposeColor
import javax.inject.Inject

/**
 * Turns a card with its computed statement into the pre-formatted UI model.
 * Shared by the cards segment and the details screen.
 */
class CreditCardUiMapper @Inject constructor(
    private val statementLabelMapper: StatementLabelMapper,
    private val skinResolver: CardSkinResolver,
) {
    fun map(item: CreditCardWithStatement, currency: String): CreditCardUi = CreditCardUi(
        id = item.card.id,
        name = item.account.name.value,
        issuer = item.card.issuer?.value ?: item.account.name.value,
        network = item.card.network,
        last4 = item.card.last4.value,
        cardholderName = item.card.cardholderName?.value.orEmpty(),
        color = item.account.color.value.toComposeColor(),
        dueAmount = item.statement.due.value,
        dueText = formatWithSymbol(item.statement.due.value, currency),
        availableText = formatWithSymbol(item.statement.availableLimit, currency),
        limitText = formatWithSymbol(item.card.creditLimit.value, currency),
        statement = statementLabelMapper.map(item.statement.status),
        repaymentAccountId = item.card.repaymentAccountId,
        payeeVpa = item.card.payeeVpa?.value,
        tier = item.card.tier?.value,
        skin = skinResolver.resolve(
            mode = item.card.skin,
            cardId = item.card.id,
            issuer = item.card.issuer?.value,
            cardName = item.account.name.value,
            network = item.card.network,
            tier = item.card.tier?.value,
            color = item.account.color.value.toComposeColor(),
        ),
    )
}
