package com.ivy.domain.creditcard

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.data.model.CardNetwork
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class CardNetworkDetectorTest {

    enum class Case(val number: String, val expected: CardNetwork) {
        Visa("4111111111111111", CardNetwork.VISA),
        VisaShortPrefix("4", CardNetwork.VISA),
        Mastercard51("5105105105105100", CardNetwork.MASTERCARD),
        Mastercard55("5555555555554444", CardNetwork.MASTERCARD),
        Mastercard2221("2221000000000009", CardNetwork.MASTERCARD),
        Mastercard2720("2720990000000000", CardNetwork.MASTERCARD),
        NotMastercard2721("2721000000000000", CardNetwork.UNKNOWN),
        Amex34("340000000000009", CardNetwork.AMEX),
        Amex37("378282246310005", CardNetwork.AMEX),
        Diners300("30000000000004", CardNetwork.DINERS),
        Diners305("30500000000000", CardNetwork.DINERS),
        Diners36("36000000000008", CardNetwork.DINERS),
        Diners38("38000000000006", CardNetwork.DINERS),
        Discover6011("6011111111111117", CardNetwork.DISCOVER),
        Discover644("6440000000000000", CardNetwork.DISCOVER),
        Discover649("6490000000000000", CardNetwork.DISCOVER),
        RuPay60("6070000000000000", CardNetwork.RUPAY),
        RuPay65Generic("6500000000000000", CardNetwork.RUPAY),
        RuPay6521("6521000000000000", CardNetwork.RUPAY),
        RuPay6599("6599000000000000", CardNetwork.RUPAY),
        RuPay81("8100000000000000", CardNetwork.RUPAY),
        RuPay82("8200000000000000", CardNetwork.RUPAY),
        RuPay508("5080000000000000", CardNetwork.RUPAY),
        Spaces("4111 1111 1111 1111", CardNetwork.VISA),
        Empty("", CardNetwork.UNKNOWN),
        Unknown("9999999999999999", CardNetwork.UNKNOWN),
        Letters("abcd", CardNetwork.UNKNOWN),
    }

    @Test
    fun `detects network`(@TestParameter case: Case) {
        CardNetworkDetector.detect(case.number) shouldBe case.expected
    }
}
