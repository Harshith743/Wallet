package com.ivy.data.repository

import arrow.core.Some
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.DeleteOperation
import com.ivy.data.db.dao.fake.FakeCreditCardDao
import com.ivy.data.model.AccountId
import com.ivy.data.model.testing.creditCard
import com.ivy.data.repository.mapper.CreditCardMapper
import com.ivy.data.repository.fake.fakeRepositoryMemoFactory
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.UUID

class CreditCardRepositoryTest {
    private val dao = FakeCreditCardDao()
    private val mapper = CreditCardMapper()

    private lateinit var repository: CreditCardRepository

    @Before
    fun setup() {
        repository = CreditCardRepository(
            mapper = mapper,
            creditCardDao = dao,
            writeCreditCardDao = dao,
            memoFactory = fakeRepositoryMemoFactory(),
        )
    }

    @Test
    fun `find by id - missing`() = runTest {
        repository.findById(AccountId(UUID.randomUUID())) shouldBe null
    }

    @Test
    fun `save then find by id and find all`() = runTest {
        val card = Arb.creditCard().next()

        repository.save(card)

        repository.findById(card.id) shouldBe card
        repository.findAll() shouldBe listOf(card)
        repository.findAllIds() shouldBe setOf(card.id)
    }

    @Test
    fun `save many, delete by id, delete all`() = runTest {
        val a = Arb.creditCard().next()
        val b = Arb.creditCard().next()

        repository.saveMany(listOf(a, b))
        repository.findAll() shouldContainExactlyInAnyOrder listOf(a, b)

        repository.deleteById(a.id)
        repository.findAll() shouldBe listOf(b)
        dao.findById(a.id.value) shouldBe null

        repository.deleteAll()
        repository.findAll() shouldBe emptyList()
        dao.findAll() shouldBe emptyList()
    }

    @Test
    fun `save posts a credit card change event`() = runTest {
        val observer = mockk<DataObserver>(relaxed = true)
        val repo = CreditCardRepository(
            mapper = mapper,
            creditCardDao = dao,
            writeCreditCardDao = dao,
            memoFactory = RepositoryMemoFactory(
                dataObserver = observer,
                dispatchers = com.ivy.base.TestDispatchersProvider,
            ),
        )
        val card = Arb.creditCard(id = Some(AccountId(UUID.randomUUID()))).next()

        repo.save(card)
        repo.deleteById(card.id)

        coVerify { observer.post(DataWriteEvent.SaveCreditCards(listOf(card))) }
        coVerify {
            observer.post(DataWriteEvent.DeleteCreditCards(DeleteOperation.Just(listOf(card.id))))
        }
    }
}
