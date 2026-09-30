package com.ivy.data.repository.mapper

import com.ivy.data.invalidCreditCardEntity
import com.ivy.data.model.CardNetwork
import com.ivy.data.model.CardSkinMode
import com.ivy.data.model.testing.creditCard
import com.ivy.data.validCreditCardEntity
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CreditCardMapperPropertyTest {

    private val mapper = CreditCardMapper()

    @Test
    fun `property - domain-entity isomorphism`() = runTest {
        checkAll(Arb.creditCard()) { cardOrig ->
            with(mapper) {
                val entityOne = cardOrig.toEntity()
                val cardTwo = entityOne.toDomain().getOrNull()

                cardTwo.shouldNotBeNull() shouldBe cardOrig

                val entityTwo = cardTwo.toEntity()
                entityTwo shouldBe entityOne
            }
        }
    }

    @Test
    fun `maps valid entities - always succeeds`() = runTest {
        checkAll(Arb.validCreditCardEntity()) { entity ->
            with(mapper) { entity.toDomain() }.shouldBeRight()
        }
    }

    @Test
    fun `maps invalid entities - always fails`() = runTest {
        checkAll(Arb.invalidCreditCardEntity()) { entity ->
            with(mapper) { entity.toDomain() }.shouldBeLeft()
        }
    }

    @Test
    fun `unknown network string maps to UNKNOWN`() {
        val entity = Arb.validCreditCardEntity().next().copy(network = "SOMETHING_NEW")

        val card = with(mapper) { entity.toDomain() }.getOrNull()

        card.shouldNotBeNull().network shouldBe CardNetwork.UNKNOWN
    }

    @Test
    fun `unknown skin string maps to AUTO`() {
        val entity = Arb.validCreditCardEntity().next().copy(skin = "HOLOGRAM")

        val card = with(mapper) { entity.toDomain() }.getOrNull()

        card.shouldNotBeNull().skin shouldBe CardSkinMode.AUTO
    }
}
