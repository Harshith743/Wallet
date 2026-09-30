package com.ivy.domain.creditcard

import android.content.Context
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.model.CardNetwork
import com.ivy.data.model.primitive.CardBin
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the bundled dataset knows about a 6-digit BIN. Every field except [bin] may be
 * absent. [tier] is the card category when it says something about the product
 * ("Platinum", "Signature"), never generic values such as "Classic" or "Standard".
 */
data class BinRecord(
    val bin: String,
    val brand: String?,
    val cardType: String?,
    val tier: String?,
    val issuer: String?,
)

/** Offline lookup of the issuing bank and product tier by card number. */
interface BinLookup {
    suspend fun lookup(cardNumber: String): BinRecord?
}

private const val CommentPrefix = '#'
private const val HeaderBin = "bin"
private const val ColumnBin = 0
private const val ColumnBrand = 1
private const val ColumnType = 2
private const val ColumnCategory = 3
private const val ColumnIssuer = 4

private val UninformativeCategories = setOf(
    "CLASSIC", "STANDARD", "ELECTRON", "PREPAID", "ATM", "DEBIT", "CREDIT", "BUSINESS", "CORPORATE", "CORPORATE T&E",
)

/**
 * Parses the bundled `bin,brand,type,category,issuer` CSV. Fields never contain commas
 * or quotes (the generator strips them). Comment lines (`#`), the header and blank
 * lines are skipped; a row with a malformed BIN is ignored, missing columns are null.
 */
fun parseBinCsv(lines: Sequence<String>): Map<String, BinRecord> = buildMap {
    for (line in lines) {
        val record = parseBinLine(line) ?: continue
        put(record.bin, record)
    }
}

private fun parseBinLine(line: String): BinRecord? {
    val trimmed = line.trim()
    val skip = trimmed.isEmpty() || trimmed.startsWith(CommentPrefix) || trimmed.startsWith(HeaderBin)
    val columns = if (skip) emptyList() else trimmed.split(',').map { it.trim() }
    val bin = columns.getOrNull(ColumnBin)?.takeIf { it.length == CardBin.LENGTH && it.all(Char::isDigit) }
    val category = columns.getOrNull(ColumnCategory)?.takeIf { it.isNotEmpty() }
    return bin?.let {
        BinRecord(
            bin = it,
            brand = columns.getOrNull(ColumnBrand)?.takeIf { it.isNotEmpty() },
            cardType = columns.getOrNull(ColumnType)?.takeIf { it.isNotEmpty() },
            tier = category?.takeIf { it.uppercase() !in UninformativeCategories },
            issuer = columns.getOrNull(ColumnIssuer)?.takeIf { it.isNotEmpty() },
        )
    }
}

/** Finds the record for the first six digits of [cardNumber] (spaces allowed). */
fun Map<String, BinRecord>.lookupCard(cardNumber: String): BinRecord? {
    val digits = normalizeCardNumber(cardNumber)
    if (digits.length < CardBin.LENGTH) return null
    return this[digits.take(CardBin.LENGTH)]
}

/** Networks that issue their own cards: the network is the issuer when the BIN is unknown. */
fun defaultIssuerFor(network: CardNetwork): String? = when (network) {
    CardNetwork.AMEX -> "American Express"
    CardNetwork.DINERS -> "Diners Club"
    else -> null
}

/**
 * The bundled Indian BIN dataset (`assets/bin_ranges_in.csv`, from binlist-data, CC BY 4.0),
 * parsed once on first use. No network access.
 */
@Singleton
class BundledBinDataset @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatchersProvider,
) : BinLookup {
    private val mutex = Mutex()

    @Volatile
    private var records: Map<String, BinRecord>? = null

    override suspend fun lookup(cardNumber: String): BinRecord? = records().lookupCard(cardNumber)

    private suspend fun records(): Map<String, BinRecord> = records ?: mutex.withLock {
        records ?: withContext(dispatchers.io) {
            context.assets.open(AssetFileName).bufferedReader().useLines(::parseBinCsv)
        }.also { records = it }
    }

    companion object {
        const val AssetFileName = "bin_ranges_in.csv"
    }
}
