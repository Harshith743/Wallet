package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("MagicNumber", "ClassNaming")
class Migration131to132_CreditCardSkins : Migration(131, 132) {
    companion object {
        private const val CREDIT_CARDS_TABLE = "credit_cards"
    }

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `$CREDIT_CARDS_TABLE` ADD COLUMN `tier` TEXT")
        db.execSQL("ALTER TABLE `$CREDIT_CARDS_TABLE` ADD COLUMN `skin` TEXT NOT NULL DEFAULT 'AUTO'")
    }
}
