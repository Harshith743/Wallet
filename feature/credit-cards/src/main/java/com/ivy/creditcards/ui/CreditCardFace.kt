package com.ivy.creditcards.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.creditcards.model.CreditCardUi
import com.ivy.creditcards.skin.brush
import com.ivy.data.model.CardNetwork
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Red

object CreditCardFaceDefaults {
    const val AspectRatio = 1.586f
    const val SecondaryAlpha = 0.8f
    val CornerRadius: Dp = 20.dp
    val Padding: Dp = 20.dp
    val ChipWidth: Dp = 38.dp
    val ChipHeight: Dp = 28.dp
    val AccentBarWidth: Dp = 28.dp
    val AccentBarHeight: Dp = 3.dp
}

/**
 * The card face: issuer wordmark, tier and accent, due amount and status, chip, network
 * and last 4 digits, cardholder name, remaining limit, and an optional "Pay now" button.
 * Painted with the card's skin (bank gradient, plain colour).
 * Theme-agnostic (explicit styles) so it renders the same in the legacy tab and M3 screens.
 */
@Composable
fun CreditCardFace(
    card: CreditCardUi,
    modifier: Modifier = Modifier,
    showPayNow: Boolean = false,
    onPayNow: () -> Unit = {},
) {
    val contrast = card.skin.textColor
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CreditCardFaceDefaults.AspectRatio)
            .clip(RoundedCornerShape(CreditCardFaceDefaults.CornerRadius))
            .background(card.skin.brush())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(CreditCardFaceDefaults.Padding)
        ) {
            FaceHeader(card = card, contrast = contrast)
            Spacer(Modifier.height(16.dp))
            FaceIdentityRow(card = card, contrast = contrast)
            Spacer(Modifier.weight(1f))
            FaceFooter(card = card, contrast = contrast, showPayNow = showPayNow, onPayNow = onPayNow)
        }
    }
}

@Composable
private fun FaceHeader(
    card: CreditCardUi,
    contrast: Color,
) {
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = card.skin.wordmark ?: card.issuer.uppercase(),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = contrast,
                    letterSpacing = 1.sp,
                ),
                maxLines = 1,
            )
            card.tier?.let { tier ->
                Text(
                    text = tier.uppercase(),
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contrast.copy(alpha = CreditCardFaceDefaults.SecondaryAlpha),
                        letterSpacing = 2.sp,
                    ),
                    maxLines = 1,
                )
            }
            card.skin.accent?.let { accent ->
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(
                            width = CreditCardFaceDefaults.AccentBarWidth,
                            height = CreditCardFaceDefaults.AccentBarHeight,
                        )
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent)
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = card.dueText,
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = contrast),
                maxLines = 1,
            )
            StatusText(card = card, contrast = contrast)
        }
    }
}

@Composable
private fun StatusText(
    card: CreditCardUi,
    contrast: Color,
) {
    val text = card.statement.text().uppercase()
    if (card.statement.isOverdue()) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(CreditCardFaceDefaults.CornerRadius))
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(text = text, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Red))
        }
    } else {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = contrast.copy(alpha = CreditCardFaceDefaults.SecondaryAlpha),
                letterSpacing = 1.sp,
            ),
        )
    }
}

@Composable
private fun FaceIdentityRow(
    card: CreditCardUi,
    contrast: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CardChip()
        Spacer(Modifier.width(12.dp))
        NetworkWordmark(network = card.network, color = contrast)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "•• ${card.last4}",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = contrast,
                letterSpacing = 2.sp,
            ),
        )
    }
}

@Composable
private fun FaceFooter(
    card: CreditCardUi,
    contrast: Color,
    showPayNow: Boolean,
    onPayNow: () -> Unit,
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = card.cardholderName.uppercase(),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contrast,
                    letterSpacing = 2.sp,
                ),
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.available_of_limit, card.availableText, card.limitText),
                style = TextStyle(
                    fontSize = 11.sp,
                    color = contrast.copy(alpha = CreditCardFaceDefaults.SecondaryAlpha),
                ),
                maxLines = 1,
            )
        }
        if (showPayNow) {
            Spacer(Modifier.width(12.dp))
            PayNowButton(onClick = onPayNow)
        }
    }
}

@Composable
private fun PayNowButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            text = stringResource(R.string.pay_now),
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black),
        )
    }
}

@Composable
private fun CardChip(
    modifier: Modifier = Modifier,
) {
    val gold = Brush.linearGradient(listOf(ChipGoldLight, ChipGoldDark))
    Canvas(
        modifier = modifier
            .size(width = CreditCardFaceDefaults.ChipWidth, height = CreditCardFaceDefaults.ChipHeight)
            .clip(RoundedCornerShape(6.dp))
            .background(gold)
    ) {
        val line = Color.Black.copy(alpha = ChipLineAlpha)
        val third = size.height / ChipRows
        val middle = size.width / 2f
        drawLine(line, Offset(0f, third), Offset(size.width, third), strokeWidth = ChipLineWidth)
        drawLine(line, Offset(0f, third * 2), Offset(size.width, third * 2), strokeWidth = ChipLineWidth)
        drawLine(line, Offset(middle, 0f), Offset(middle, size.height), strokeWidth = ChipLineWidth)
    }
}

private const val ChipLineAlpha = 0.25f
private const val ChipRows = 3f
private const val ChipLineWidth = 2f
private val ChipGoldLight = Color(0xFFEAD9A6)
private val ChipGoldDark = Color(0xFFC6A65C)

@Composable
private fun NetworkWordmark(
    network: CardNetwork,
    color: Color,
) {
    val (label, italic) = when (network) {
        CardNetwork.VISA -> "VISA" to true
        CardNetwork.MASTERCARD -> "mastercard" to false
        CardNetwork.RUPAY -> "RuPay" to true
        CardNetwork.AMEX -> "AMEX" to false
        CardNetwork.DINERS -> "Diners" to false
        CardNetwork.DISCOVER -> "DISCOVER" to false
        CardNetwork.UNKNOWN -> "" to false
    }
    Text(
        text = label,
        style = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            color = color,
        ),
    )
}
