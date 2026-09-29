package com.ivy.data

import com.ivy.data.db.entity.CreditCardEntity
import com.ivy.data.model.testing.cardBin
import com.ivy.data.model.testing.cardLast4
import com.ivy.data.model.testing.cardNetwork
import com.ivy.data.model.testing.maybe
import com.ivy.data.model.testing.notBlankTrimmedString
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.uuid

fun Arb.Companion.validCreditCardEntity(): Arb<CreditCardEntity> = arbitrary {
    CreditCardEntity(
        cardholderName = Arb.maybe(Arb.notBlankTrimmedString()).bind()?.value,
        issuer = Arb.maybe(Arb.notBlankTrimmedString()).bind()?.value,
        network = Arb.cardNetwork().bind().name,
        last4 = Arb.cardLast4().bind().value,
        bin = Arb.maybe(Arb.cardBin()).bind()?.value,
        expiryMonth = Arb.int(min = 1, max = 12).bind(),
        expiryYear = Arb.int(min = 2000, max = 2099).bind(),
        creditLimit = Arb.double(min = 1.0, max = 1_000_000.0).bind(),
        billingDay = Arb.int(min = 1, max = 31).bind(),
        dueDay = Arb.int(min = 1, max = 31).bind(),
        repaymentAccountId = Arb.maybe(Arb.uuid()).bind(),
        payeeVpa = Arb.maybe(Arb.notBlankTrimmedString()).bind()?.value,
        id = Arb.uuid().bind(),
    )
}

fun Arb.Companion.invalidCreditCardEntity(): Arb<CreditCardEntity> = arbitrary {
    val valid = validCreditCardEntity().bind()
    when (Arb.int(min = 0, max = 4).bind()) {
        0 -> valid.copy(last4 = Arb.of("", "123", "12345", "abcd").bind())
        1 -> valid.copy(expiryMonth = Arb.of(0, 13, -1).bind())
        2 -> valid.copy(creditLimit = Arb.of(0.0, -1.0).bind())
        3 -> valid.copy(billingDay = Arb.of(0, 32).bind())
        else -> valid.copy(dueDay = Arb.of(0, 32).bind())
    }
}
