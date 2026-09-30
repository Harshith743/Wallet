package com.ivy.data.db.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ivy.base.kotlinxserilzation.KSerializerUUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Card-specific details of a credit card. The [id] is the id of the `accounts` row that
 * backs the card (name, colour, currency, transactions). Amounts are never stored here;
 * they are derived from the account's transactions. Secrets (PAN, CVV) are never stored
 * in the database.
 */
@Suppress("DataClassDefaultValues")
@Keep
@Serializable
@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @SerialName("cardholderName")
    val cardholderName: String? = null,
    @SerialName("issuer")
    val issuer: String? = null,
    @SerialName("network")
    val network: String = "UNKNOWN",
    @SerialName("last4")
    val last4: String,
    @SerialName("bin")
    val bin: String? = null,
    @SerialName("expiryMonth")
    val expiryMonth: Int,
    @SerialName("expiryYear")
    val expiryYear: Int,
    @SerialName("creditLimit")
    val creditLimit: Double,
    @SerialName("billingDay")
    val billingDay: Int,
    @SerialName("dueDay")
    val dueDay: Int,
    @SerialName("repaymentAccountId")
    @Serializable(with = KSerializerUUID::class)
    val repaymentAccountId: UUID? = null,
    @SerialName("payeeVpa")
    val payeeVpa: String? = null,
    @SerialName("tier")
    val tier: String? = null,
    @SerialName("skin")
    @ColumnInfo(defaultValue = "AUTO")
    val skin: String = "AUTO",

    @PrimaryKey
    @SerialName("id")
    @Serializable(with = KSerializerUUID::class)
    val id: UUID,
)
