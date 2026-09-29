package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("MagicNumber", "ClassNaming")
class Migration130to131_CreditCards : Migration(130, 131) {
    companion object {
        private const val CREDIT_CARDS_TABLE = "credit_cards"
    }

    @Suppress("MaximumLineLength", "MaxLineLength")
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `$CREDIT_CARDS_TABLE` (`cardholderName` TEXT, `issuer` TEXT, `network` TEXT NOT NULL, `last4` TEXT NOT NULL, `bin` TEXT, `expiryMonth` INTEGER NOT NULL, `expiryYear` INTEGER NOT NULL, `creditLimit` REAL NOT NULL, `billingDay` INTEGER NOT NULL, `dueDay` INTEGER NOT NULL, `repaymentAccountId` TEXT, `payeeVpa` TEXT, `id` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
    }
}
