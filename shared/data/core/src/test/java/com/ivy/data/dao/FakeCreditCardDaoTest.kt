package com.ivy.data.dao

import com.ivy.data.db.dao.fake.FakeCreditCardDao
import com.ivy.data.db.entity.CreditCardEntity
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.UUID

class FakeCreditCardDaoTest {
    private lateinit var dao: FakeCreditCardDao

    @Before
    fun setup() {
        dao = FakeCreditCardDao()
    }

    @Test
    fun `save then find by id`() = runTest {
        val card = card(UUID.randomUUID())

        dao.save(card)

        dao.findById(card.id) shouldBe card
        dao.findById(UUID.randomUUID()) shouldBe null
    }

    @Test
    fun `upsert replaces the existing row`() = runTest {
        val id = UUID.randomUUID()
        dao.save(card(id, last4 = "1111"))

        dao.save(card(id, last4 = "2222"))

        dao.findAll().size shouldBe 1
        dao.findById(id)?.last4 shouldBe "2222"
    }

    @Test
    fun `save many, delete by id and delete all`() = runTest {
        val a = card(UUID.randomUUID())
        val b = card(UUID.randomUUID())
        dao.saveMany(listOf(a, b))
        dao.findAll() shouldContainExactlyInAnyOrder listOf(a, b)

        dao.deleteById(a.id)
        dao.findAll() shouldBe listOf(b)

        dao.deleteAll()
        dao.findAll() shouldBe emptyList()
    }

    private fun card(id: UUID, last4: String = "6304") = CreditCardEntity(
        cardholderName = "Harshith",
        issuer = "HDFC Bank",
        network = "VISA",
        last4 = last4,
        bin = "437551",
        expiryMonth = 12,
        expiryYear = 2030,
        creditLimit = 36_000.0,
        billingDay = 5,
        dueDay = 25,
        repaymentAccountId = null,
        payeeVpa = null,
        id = id,
    )
}
