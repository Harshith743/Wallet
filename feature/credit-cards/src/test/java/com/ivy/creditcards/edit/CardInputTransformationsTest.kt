package com.ivy.creditcards.edit

import androidx.compose.ui.text.AnnotatedString
import com.ivy.data.model.CardNetwork
import io.kotest.matchers.shouldBe
import org.junit.Test

class CardInputTransformationsTest {
    @Test
    fun `card number groups of four`() {
        val result = CardNumberVisualTransformation(CardNetwork.VISA).filter(AnnotatedString("4111111111111111"))

        result.text.text shouldBe "4111 1111 1111 1111"
        result.offsetMapping.originalToTransformed(0) shouldBe 0
        result.offsetMapping.originalToTransformed(4) shouldBe 5
        result.offsetMapping.originalToTransformed(16) shouldBe 19
        result.offsetMapping.transformedToOriginal(19) shouldBe 16
        result.offsetMapping.transformedToOriginal(5) shouldBe 4
    }

    @Test
    fun `amex groups 4-6-5`() {
        val result = CardNumberVisualTransformation(CardNetwork.AMEX).filter(AnnotatedString("378282246310005"))

        result.text.text shouldBe "3782 822463 10005"
    }

    @Test
    fun `partial numbers`() {
        CardNumberVisualTransformation(CardNetwork.UNKNOWN).filter(AnnotatedString("41")).text.text shouldBe "41"
        CardNumberVisualTransformation(CardNetwork.UNKNOWN).filter(AnnotatedString("41111")).text.text shouldBe "4111 1"
    }

    @Test
    fun `expiry adds the slash`() {
        val result = ExpiryVisualTransformation().filter(AnnotatedString("1230"))

        result.text.text shouldBe "12/30"
        result.offsetMapping.originalToTransformed(4) shouldBe 5
        result.offsetMapping.transformedToOriginal(5) shouldBe 4
        ExpiryVisualTransformation().filter(AnnotatedString("12")).text.text shouldBe "12"
    }
}
