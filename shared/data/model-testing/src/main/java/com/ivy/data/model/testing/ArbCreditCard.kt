package com.ivy.data.model.testing

import arrow.core.None
import arrow.core.Option
import arrow.core.getOrElse
import com.ivy.data.model.AccountId
import com.ivy.data.model.CardNetwork
import com.ivy.data.model.CardSkinMode
import com.ivy.data.model.CreditCard
import com.ivy.data.model.primitive.CardBin
import com.ivy.data.model.primitive.CardLast4
import com.ivy.data.model.primitive.DayOfMonth
import com.ivy.data.model.primitive.PositiveDouble
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import java.time.YearMonth

private const val MinExpiryYear = 2000
private const val MaxExpiryYear = 2099
private const val MaxCreditLimit = 100_000_000.0

fun Arb.Companion.creditCard(
    id: Option<AccountId> = None,
    billingDay: Option<DayOfMonth> = None,
    dueDay: Option<DayOfMonth> = None,
    creditLimit: Option<PositiveDouble> = None,
    repaymentAccountId: Option<AccountId?> = None,
): Arb<CreditCard> = arbitrary {
    CreditCard(
        id = id.getOrElse { Arb.accountId().bind() },
        cardholderName = Arb.maybe(Arb.notBlankTrimmedString()).bind(),
        issuer = Arb.maybe(Arb.notBlankTrimmedString()).bind(),
        network = Arb.cardNetwork().bind(),
        last4 = Arb.cardLast4().bind(),
        bin = Arb.maybe(Arb.cardBin()).bind(),
        expiry = Arb.yearMonth().bind(),
        creditLimit = creditLimit.getOrElse {
            Arb.positiveDoubleExact(max = MaxCreditLimit).bind()
        },
        billingDay = billingDay.getOrElse { Arb.dayOfMonth().bind() },
        dueDay = dueDay.getOrElse { Arb.dayOfMonth().bind() },
        repaymentAccountId = repaymentAccountId.getOrElse {
            Arb.maybe(Arb.accountId()).bind()
        },
        payeeVpa = Arb.maybe(Arb.notBlankTrimmedString()).bind(),
        tier = Arb.maybe(Arb.notBlankTrimmedString()).bind(),
        skin = Arb.enum<CardSkinMode>().bind(),
    )
}

fun Arb.Companion.cardNetwork(): Arb<CardNetwork> = Arb.enum<CardNetwork>()

fun Arb.Companion.dayOfMonth(): Arb<DayOfMonth> =
    Arb.int(min = DayOfMonth.MIN, max = DayOfMonth.MAX).map(DayOfMonth::unsafe)

fun Arb.Companion.yearMonth(): Arb<YearMonth> = arbitrary {
    YearMonth.of(
        Arb.int(min = MinExpiryYear, max = MaxExpiryYear).bind(),
        Arb.int(min = 1, max = 12).bind(),
    )
}

fun Arb.Companion.cardLast4(): Arb<CardLast4> = Arb.digits(CardLast4.LENGTH).map(CardLast4::unsafe)

fun Arb.Companion.cardBin(): Arb<CardBin> = Arb.digits(CardBin.LENGTH).map(CardBin::unsafe)

fun Arb.Companion.digits(length: Int): Arb<String> = arbitrary {
    buildString {
        repeat(length) { append(Arb.int(min = 0, max = 9).bind()) }
    }
}
