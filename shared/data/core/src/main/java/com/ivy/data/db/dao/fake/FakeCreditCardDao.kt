package com.ivy.data.db.dao.fake

import com.ivy.data.db.dao.read.CreditCardDao
import com.ivy.data.db.dao.write.WriteCreditCardDao
import com.ivy.data.db.entity.CreditCardEntity
import org.jetbrains.annotations.VisibleForTesting
import java.util.UUID

@VisibleForTesting
class FakeCreditCardDao : CreditCardDao, WriteCreditCardDao {
    private val cards = mutableMapOf<UUID, CreditCardEntity>()

    override suspend fun findAll(): List<CreditCardEntity> {
        return cards.values.toList()
    }

    override suspend fun findById(id: UUID): CreditCardEntity? {
        return cards[id]
    }

    override suspend fun save(value: CreditCardEntity) {
        cards[value.id] = value
    }

    override suspend fun saveMany(values: List<CreditCardEntity>) {
        values.forEach { save(it) }
    }

    override suspend fun deleteById(id: UUID) {
        cards.remove(id)
    }

    override suspend fun deleteAll() {
        cards.clear()
    }
}
