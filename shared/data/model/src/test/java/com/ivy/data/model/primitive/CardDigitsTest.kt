package com.ivy.data.model.primitive

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.Test

class CardDigitsTest {
    @Test
    fun `pan accepts 12 to 19 digits`() {
        CardPan.from("123456789012").isRight() shouldBe true
        CardPan.from("1234567890123456789").isRight() shouldBe true
        CardPan.from("12345678901").isLeft() shouldBe true
        CardPan.from("12345678901234567890").isLeft() shouldBe true
    }

    @Test
    fun `pan rejects non digits`() {
        CardPan.from("4111 1111 1111 1111").isLeft() shouldBe true
        CardPan.from("4111-1111-1111-1111").isLeft() shouldBe true
        CardPan.from("").isLeft() shouldBe true
    }

    @Test
    fun `pan and cvv are masked when printed`() {
        val pan = CardPan.unsafe("4111111111111111")
        val cvv = CardCvv.unsafe("123")

        pan.toString() shouldNotContain "4111"
        cvv.toString() shouldNotContain "123"
        pan.value shouldBe "4111111111111111"
        cvv.value shouldBe "123"
    }

    @Test
    fun `cvv accepts 3 or 4 digits`() {
        CardCvv.from("123").isRight() shouldBe true
        CardCvv.from("1234").isRight() shouldBe true
        CardCvv.from("12").isLeft() shouldBe true
        CardCvv.from("12345").isLeft() shouldBe true
        CardCvv.from("12a").isLeft() shouldBe true
    }

    @Test
    fun `last4 accepts exactly 4 digits`() {
        CardLast4.from("6304").getOrNull()?.value shouldBe "6304"
        CardLast4.from("630").isLeft() shouldBe true
        CardLast4.from("63041").isLeft() shouldBe true
        CardLast4.from("63O4").isLeft() shouldBe true
    }

    @Test
    fun `bin accepts exactly 6 digits`() {
        CardBin.from("437551").getOrNull()?.value shouldBe "437551"
        CardBin.from("43755").isLeft() shouldBe true
        CardBin.from("4375511").isLeft() shouldBe true
    }
}
