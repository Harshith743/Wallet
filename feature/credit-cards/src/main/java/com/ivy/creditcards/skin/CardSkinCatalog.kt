package com.ivy.creditcards.skin

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.ivy.data.model.CardNetwork
import com.ivy.wallet.ui.theme.findContrastTextColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * How a card face is painted: gradient stops (one colour = solid), the text colour that
 * reads on them, an optional accent (issuer brand colour) and an optional wordmark that
 * replaces the issuer text.
 */
@Immutable
data class CardSkinUi(
    val colors: ImmutableList<Color>,
    val textColor: Color,
    val accent: Color?,
    val wordmark: String?,
)

private data class Theme(
    val keys: List<List<String>>,
    val colors: List<Color>,
    val accent: Color?,
    val wordmark: String?,
)

private val White = Color.White
private val Graphite = listOf(Color(0xFF141414), Color(0xFF34343A))

private val HdfcNavy = Color(0xFF0B2A6F)
private val HdfcBlue = Color(0xFF1E4DB7)
private val HdfcRed = Color(0xFFE31837)
private val IciciMaroon = Color(0xFF7A1F1F)
private val IciciRed = Color(0xFFB02A30)
private val IciciOrange = Color(0xFFF58220)
private val SbiNavy = Color(0xFF1B4F8C)
private val SbiBlue = Color(0xFF2A7DD1)
private val SbiSky = Color(0xFF7CC4F5)
private val AxisPlum = Color(0xFF5C1E5C)
private val AxisBurgundy = Color(0xFF97144D)
private val AxisGold = Color(0xFFF2A900)
private val KotakDark = Color(0xFFB71C1C)
private val KotakRed = Color(0xFFED1C24)
private val KotakBlue = Color(0xFF003874)
private val SlicePurple = Color(0xFF6A00FF)
private val SliceViolet = Color(0xFFB13CFF)
private val OneCardBlack = Color(0xFF0F0F12)
private val OneCardSteel = Color(0xFF2A2A31)
private val OneCardGold = Color(0xFFD4AF37)
private val IdfcMaroon = Color(0xFF8B0E2F)
private val IdfcRed = Color(0xFFC1123C)
private val IdfcYellow = Color(0xFFF7B500)
private val IndusIndWine = Color(0xFF6B1F2A)
private val IndusIndRed = Color(0xFF9E2B39)
private val IndusIndGold = Color(0xFFD9A400)
private val YesNavy = Color(0xFF0B3C8C)
private val YesBlue = Color(0xFF1358C7)
private val YesRed = Color(0xFFE41E26)
private val RblNavy = Color(0xFF1A2B5F)
private val RblBlue = Color(0xFF2E4AA0)
private val RblOrange = Color(0xFFE9531D)
private val AuOrange = Color(0xFFE8621A)
private val AuAmber = Color(0xFFF28C28)
private val HsbcBlack = Color(0xFF1B1B1B)
private val HsbcGrey = Color(0xFF3A3A3A)
private val HsbcRed = Color(0xFFDB0011)
private val AmexBlue = Color(0xFF006FCF)
private val AmexSky = Color(0xFF00A3E0)
private val ScGreenDark = Color(0xFF0F4C3A)
private val ScGreen = Color(0xFF1C7F5E)
private val ScLime = Color(0xFF38D200)
private val CitiNavy = Color(0xFF003B70)
private val CitiBlue = Color(0xFF056DAE)
private val CitiRed = Color(0xFFD9261C)
private val FederalNavy = Color(0xFF0F3C74)
private val FederalBlue = Color(0xFF1F63B8)
private val FederalOrange = Color(0xFFF9A61A)
private val VisaNavy = Color(0xFF1A1F71)
private val VisaBlue = Color(0xFF2E3A9E)
private val MastercardCharcoal = Color(0xFF1F1F1F)
private val MastercardGrey = Color(0xFF3C3C3C)
private val MastercardOrange = Color(0xFFF79E1B)
private val RupayTeal = Color(0xFF097969)
private val RupayOrange = Color(0xFFE8702A)
private val DinersSlate = Color(0xFF37474F)
private val DinersGrey = Color(0xFF607D8B)
private val DiscoverDark = Color(0xFFE4642D)
private val DiscoverOrange = Color(0xFFF58220)

private fun keys(vararg phrases: String): List<List<String>> = phrases.map { it.split(' ') }

private val IssuerThemes: List<Theme> = listOf(
    Theme(keys("hdfc"), listOf(HdfcNavy, HdfcBlue), HdfcRed, "HDFC BANK"),
    Theme(keys("icici"), listOf(IciciMaroon, IciciRed), IciciOrange, "ICICI BANK"),
    Theme(keys("sbi", "state bank"), listOf(SbiNavy, SbiBlue), SbiSky, "SBI CARD"),
    Theme(keys("axis"), listOf(AxisPlum, AxisBurgundy), AxisGold, "AXIS BANK"),
    Theme(keys("kotak"), listOf(KotakDark, KotakRed), KotakBlue, "KOTAK"),
    Theme(keys("slice"), listOf(SlicePurple, SliceViolet), null, "slice"),
    Theme(keys("onecard", "one card"), listOf(OneCardBlack, OneCardSteel), OneCardGold, "OneCard"),
    Theme(keys("idfc"), listOf(IdfcMaroon, IdfcRed), IdfcYellow, "IDFC FIRST"),
    Theme(keys("indusind"), listOf(IndusIndWine, IndusIndRed), IndusIndGold, "INDUSIND"),
    Theme(keys("yes"), listOf(YesNavy, YesBlue), YesRed, "YES BANK"),
    Theme(keys("rbl", "ratnakar"), listOf(RblNavy, RblBlue), RblOrange, "RBL BANK"),
    Theme(keys("au", "au small finance"), listOf(AuOrange, AuAmber), null, "AU BANK"),
    Theme(keys("hsbc"), listOf(HsbcBlack, HsbcGrey), HsbcRed, "HSBC"),
    Theme(keys("amex", "american express"), listOf(AmexBlue, AmexSky), null, "AMERICAN EXPRESS"),
    Theme(keys("standard chartered", "stanchart"), listOf(ScGreenDark, ScGreen), ScLime, "STANDARD CHARTERED"),
    Theme(keys("citi", "citibank"), listOf(CitiNavy, CitiBlue), CitiRed, "citi"),
    Theme(keys("federal"), listOf(FederalNavy, FederalBlue), FederalOrange, "FEDERAL BANK"),
)

private val NetworkThemes: Map<CardNetwork, Theme> = mapOf(
    CardNetwork.VISA to Theme(emptyList(), listOf(VisaNavy, VisaBlue), null, null),
    CardNetwork.MASTERCARD to Theme(emptyList(), listOf(MastercardCharcoal, MastercardGrey), MastercardOrange, null),
    CardNetwork.RUPAY to Theme(emptyList(), listOf(RupayTeal, RupayOrange), null, null),
    CardNetwork.AMEX to Theme(emptyList(), listOf(AmexBlue, AmexSky), null, "AMERICAN EXPRESS"),
    CardNetwork.DINERS to Theme(emptyList(), listOf(DinersSlate, DinersGrey), null, null),
    CardNetwork.DISCOVER to Theme(emptyList(), listOf(DiscoverDark, DiscoverOrange), null, null),
)

/** Tier words that turn the face graphite ("premium") while keeping the issuer accent. */
private val PremiumTokens: List<List<String>> = keys(
    "black", "metal", "infinia", "reserve", "centurion", "infinite", "signature", "world elite",
    "magnus", "aurum", "emeralde", "prive", "diners black", "regalia gold",
)

/**
 * Picks a face theme from whole-word tokens of the issuer, card name and tier; falls back
 * to the network, then to the account colour. Matching is by whole tokens, so "au" never
 * matches "Mauritius" and "citi" never matches "Citizens".
 */
object CardSkinCatalog {
    fun resolve(
        issuer: String?,
        cardName: String,
        network: CardNetwork,
        tier: String?,
        fallback: Color,
    ): CardSkinUi {
        val tokens = tokenize("${issuer.orEmpty()} $cardName")
        val premium = PremiumTokens.any { tokens.containsSequence(it) } ||
            tier?.let { tokenize(it) }?.let { t -> PremiumTokens.any { t.containsSequence(it) } } == true
        val theme = IssuerThemes.firstOrNull { theme -> theme.keys.any { tokens.containsSequence(it) } }
            ?: NetworkThemes[network]
            ?: return solid(fallback)
        val colors = if (premium) Graphite else theme.colors
        return CardSkinUi(
            colors = colors.toImmutableList(),
            textColor = White,
            accent = theme.accent,
            wordmark = theme.wordmark,
        )
    }

    /** A plain single-colour face with contrast text. */
    fun solid(color: Color): CardSkinUi = CardSkinUi(
        colors = persistentListOf(color),
        textColor = findContrastTextColor(color),
        accent = null,
        wordmark = null,
    )

    private fun tokenize(text: String): List<String> =
        text.lowercase().split(NonLetter).filter { it.isNotEmpty() }

    private fun List<String>.containsSequence(sequence: List<String>): Boolean {
        if (sequence.isEmpty() || sequence.size > size) return false
        return indices.any { start ->
            start + sequence.size <= size && subList(start, start + sequence.size) == sequence
        }
    }

    private val NonLetter = Regex("[^a-z]+")
}
