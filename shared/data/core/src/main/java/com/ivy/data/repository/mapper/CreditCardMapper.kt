package com.ivy.data.repository.mapper

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.ivy.data.db.entity.CreditCardEntity
import com.ivy.data.model.AccountId
import com.ivy.data.model.CardNetwork
import com.ivy.data.model.CardSkinMode
import com.ivy.data.model.CreditCard
import com.ivy.data.model.primitive.CardBin
import com.ivy.data.model.primitive.CardLast4
import com.ivy.data.model.primitive.DayOfMonth
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import java.time.YearMonth
import javax.inject.Inject

private const val FirstMonth = 1
private const val LastMonth = 12

class CreditCardMapper @Inject constructor() {

    fun CreditCardEntity.toDomain(): Either<String, CreditCard> = either {
        ensure(expiryMonth in FirstMonth..LastMonth) { "Invalid expiry month: $expiryMonth" }

        CreditCard(
            id = AccountId(id),
            cardholderName = cardholderName?.let(NotBlankTrimmedString::from)?.getOrNull(),
            issuer = issuer?.let(NotBlankTrimmedString::from)?.getOrNull(),
            network = CardNetwork.entries.firstOrNull { it.name == network } ?: CardNetwork.UNKNOWN,
            last4 = CardLast4.from(last4).bind(),
            bin = bin?.let(CardBin::from)?.getOrNull(),
            expiry = YearMonth.of(expiryYear, expiryMonth),
            creditLimit = PositiveDouble.from(creditLimit).bind(),
            billingDay = DayOfMonth.from(billingDay).bind(),
            dueDay = DayOfMonth.from(dueDay).bind(),
            repaymentAccountId = repaymentAccountId?.let(::AccountId),
            payeeVpa = payeeVpa?.let(NotBlankTrimmedString::from)?.getOrNull(),
            tier = tier?.let(NotBlankTrimmedString::from)?.getOrNull(),
            skin = CardSkinMode.entries.firstOrNull { it.name == skin } ?: CardSkinMode.AUTO,
        )
    }

    fun CreditCard.toEntity(): CreditCardEntity = CreditCardEntity(
        cardholderName = cardholderName?.value,
        issuer = issuer?.value,
        network = network.name,
        last4 = last4.value,
        bin = bin?.value,
        expiryMonth = expiry.monthValue,
        expiryYear = expiry.year,
        creditLimit = creditLimit.value,
        billingDay = billingDay.value,
        dueDay = dueDay.value,
        repaymentAccountId = repaymentAccountId?.value,
        payeeVpa = payeeVpa?.value,
        tier = tier?.value,
        skin = skin.name,
        id = id.value,
    )
}
