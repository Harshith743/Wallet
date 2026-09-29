package com.ivy.data.db.dao.write

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ivy.data.db.entity.CreditCardEntity
import java.util.UUID

@Dao
interface WriteCreditCardDao {
    @Upsert
    suspend fun save(value: CreditCardEntity)

    @Upsert
    suspend fun saveMany(values: List<CreditCardEntity>)

    @Query("DELETE FROM credit_cards WHERE id = :id")
    suspend fun deleteById(id: UUID)

    @Query("DELETE FROM credit_cards")
    suspend fun deleteAll()
}
