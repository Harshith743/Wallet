package com.ivy.data.repository

import com.ivy.data.DataWriteEvent
import com.ivy.data.db.dao.read.CreditCardDao
import com.ivy.data.db.dao.write.WriteCreditCardDao
import com.ivy.data.model.AccountId
import com.ivy.data.model.CreditCard
import com.ivy.data.repository.mapper.CreditCardMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Card rows only. The backing account row is managed by [AccountRepository];
 * secrets by [CreditCardSecretsRepository].
 */
@Singleton
class CreditCardRepository @Inject constructor(
    private val mapper: CreditCardMapper,
    private val creditCardDao: CreditCardDao,
    private val writeCreditCardDao: WriteCreditCardDao,
    memoFactory: RepositoryMemoFactory,
) {
    private val memo = memoFactory.createMemo(
        getDataWriteSaveEvent = DataWriteEvent::SaveCreditCards,
        getDateWriteDeleteEvent = DataWriteEvent::DeleteCreditCards
    )

    suspend fun findById(id: AccountId): CreditCard? = memo.findById(
        id = id,
        findByIdOperation = {
            creditCardDao.findById(id.value)?.let {
                with(mapper) { it.toDomain() }.getOrNull()
            }
        }
    )

    suspend fun findAll(): List<CreditCard> = memo.findAll(
        findAllOperation = {
            creditCardDao.findAll().mapNotNull {
                with(mapper) { it.toDomain() }.getOrNull()
            }
        },
        sortMemo = { toList() }
    )

    /**
     * Ids of all accounts that are credit cards. Cheap after the first call (memoized).
     */
    suspend fun findAllIds(): Set<AccountId> = findAll().map { it.id }.toSet()

    suspend fun save(value: CreditCard): Unit = memo.save(value) {
        writeCreditCardDao.save(
            with(mapper) { it.toEntity() }
        )
    }

    suspend fun saveMany(values: List<CreditCard>): Unit = memo.saveMany(values) {
        writeCreditCardDao.saveMany(
            it.map { with(mapper) { it.toEntity() } }
        )
    }

    suspend fun deleteById(id: AccountId): Unit = memo.deleteById(id) {
        writeCreditCardDao.deleteById(id.value)
    }

    suspend fun deleteAll(): Unit = memo.deleteAll(
        deleteAllOperation = writeCreditCardDao::deleteAll
    )
}
