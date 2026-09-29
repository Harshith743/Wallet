package com.ivy.domain.creditcard

import com.ivy.data.model.CardNetwork

data class IssuerBin(
    val prefix: String,
    val issuer: String,
)

/**
 * Best-effort, offline mapping from card number prefixes to Indian issuer names.
 *
 * BIN ownership is not public data and changes over time, so this table is a small,
 * approximate seed. It is plain data: add the first six digits of your own cards here
 * to get them recognised. The user can always override the detected issuer in the form.
 */
object IndianIssuerBinTable {
    val entries: List<IssuerBin> = listOf(
        // Seed values. Unverified: treat as hints only.
        IssuerBin(prefix = "437551", issuer = "HDFC Bank"),
        IssuerBin(prefix = "524111", issuer = "HDFC Bank"),
        IssuerBin(prefix = "552260", issuer = "HDFC Bank"),
        IssuerBin(prefix = "461786", issuer = "HDFC Bank"),
        IssuerBin(prefix = "437623", issuer = "ICICI Bank"),
        IssuerBin(prefix = "470512", issuer = "ICICI Bank"),
        IssuerBin(prefix = "434666", issuer = "SBI Card"),
        IssuerBin(prefix = "512932", issuer = "SBI Card"),
        IssuerBin(prefix = "451417", issuer = "Axis Bank"),
        IssuerBin(prefix = "518646", issuer = "Axis Bank"),
        IssuerBin(prefix = "406655", issuer = "Kotak Mahindra Bank"),
        IssuerBin(prefix = "476183", issuer = "Federal Bank"),
        IssuerBin(prefix = "526912", issuer = "IDFC First Bank"),
        IssuerBin(prefix = "471530", issuer = "IndusInd Bank"),
        IssuerBin(prefix = "426536", issuer = "Yes Bank"),
        IssuerBin(prefix = "473060", issuer = "RBL Bank"),
        IssuerBin(prefix = "652850", issuer = "Slice"),
    )

    private val networkIssuers: Map<CardNetwork, String> = mapOf(
        CardNetwork.AMEX to "American Express",
        CardNetwork.DINERS to "Diners Club",
    )

    /**
     * Longest matching prefix wins. Falls back to the network's own issuer for
     * networks that issue their own cards (Amex, Diners), otherwise null.
     */
    fun lookup(cardNumber: String, network: CardNetwork = CardNetwork.UNKNOWN): String? {
        val digits = normalizeCardNumber(cardNumber)
        val byPrefix = entries
            .filter { digits.startsWith(it.prefix) }
            .maxByOrNull { it.prefix.length }
            ?.issuer
        return byPrefix ?: networkIssuers[network]
    }
}
