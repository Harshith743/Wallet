package com.ivy.accounts

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.creditcards.session.AccountsSegment
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Red
import com.ivy.wallet.ui.theme.White
import com.ivy.wallet.ui.theme.components.CircleButtonFilled
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.components.ReorderButton
import com.ivy.wallet.ui.theme.pureBlur

private val ToolbarHorizontalPadding = 16.dp
private val PillHeight = 40.dp
private val PillPadding = 4.dp
private val SideButtonSpace = 56.dp
private val BadgeSize = 16.dp
private val CaptionLetterSpacing = 2.sp
private const val ChevronOpenRotation = 180f

/**
 * CRED-style sticky toolbar: [reorder] [ ACCOUNTS | CREDIT CARDS (badge) ] [settings].
 * The reorder button is only shown when the current segment supports reordering.
 */
@Composable
fun AccountsHeaderToolbar(
    segment: AccountsSegment,
    showReorder: Boolean,
    dueCardsCount: Int,
    onSegmentSelect: (AccountsSegment) -> Unit,
    onReorderClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(pureBlur())
            .padding(horizontal = ToolbarHorizontalPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(SideButtonSpace), contentAlignment = Alignment.CenterStart) {
            if (showReorder) {
                ReorderButton(onClick = onReorderClick)
            }
        }
        SegmentPillToggle(
            modifier = Modifier.weight(1f),
            segment = segment,
            dueCardsCount = dueCardsCount,
            onSegmentSelect = onSegmentSelect,
        )
        Box(modifier = Modifier.width(SideButtonSpace), contentAlignment = Alignment.CenterEnd) {
            CircleButtonFilled(
                modifier = Modifier.testTag("settings_button"),
                icon = R.drawable.ic_settings,
                contentDescription = "settings",
                onClick = onSettingsClick,
            )
        }
    }
}

@Composable
private fun SegmentPillToggle(
    segment: AccountsSegment,
    dueCardsCount: Int,
    onSegmentSelect: (AccountsSegment) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .height(PillHeight)
            .clip(UI.shapes.rFull)
            .background(UI.colors.medium)
            .padding(PillPadding)
    ) {
        val segmentWidth = (maxWidth - PillPadding * 2) / 2
        val thumbOffset by animateDpAsState(
            targetValue = if (segment == AccountsSegment.CREDIT_CARDS) segmentWidth else 0.dp,
            label = "segment_thumb",
        )
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(UI.shapes.rFull)
                .background(UI.colors.pure)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            SegmentLabel(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.segment_accounts),
                selected = segment == AccountsSegment.ACCOUNTS,
                onClick = { onSegmentSelect(AccountsSegment.ACCOUNTS) },
            )
            SegmentLabel(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.segment_credit_cards),
                selected = segment == AccountsSegment.CREDIT_CARDS,
                onClick = { onSegmentSelect(AccountsSegment.CREDIT_CARDS) },
                badgeCount = dueCardsCount,
            )
        }
    }
}

@Composable
private fun SegmentLabel(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(UI.shapes.rFull)
            .clickableNoIndication(rememberInteractionSource(), onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text.uppercase(),
            style = UI.typo.c.style(
                color = if (selected) UI.colors.pureInverse else UI.colors.gray,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            ).copy(letterSpacing = 1.sp),
            maxLines = 1,
        )
        if (badgeCount > 0) {
            Spacer(Modifier.width(4.dp))
            CountBadge(count = badgeCount)
        }
    }
}

@Composable
private fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(BadgeSize)
            .clip(CircleShape)
            .background(Red)
            .testTag("due_badge"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            style = UI.typo.c.style(color = White, fontWeight = FontWeight.Bold).copy(fontSize = 9.sp),
            maxLines = 1,
        )
    }
}

/**
 * Centred caption ("TOTAL BALANCE" / "STATEMENT DUE FOR 3 CARDS") with the big amount
 * (currency symbol + number), an optional secondary line, and, when [onDrawerToggle] is
 * given, a chevron that opens the breakdown drawer.
 */
@Composable
fun AccountsHeaderSummary(
    caption: String,
    currencySymbol: String,
    amountText: String,
    modifier: Modifier = Modifier,
    secondaryLine: String? = null,
    drawerExpanded: Boolean = false,
    onDrawerToggle: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = caption.uppercase(),
            style = UI.typo.c.style(
                color = UI.colors.gray,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ).copy(letterSpacing = CaptionLetterSpacing),
        )
        Spacer(Modifier.height(4.dp))
        AmountRow(
            currencySymbol = currencySymbol,
            amountText = amountText,
            drawerExpanded = drawerExpanded,
            onDrawerToggle = onDrawerToggle,
        )
        if (secondaryLine != null && !drawerExpanded) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = secondaryLine,
                style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
            )
        }
    }
}

@Composable
private fun AmountRow(
    currencySymbol: String,
    amountText: String,
    drawerExpanded: Boolean,
    onDrawerToggle: (() -> Unit)?,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (drawerExpanded) ChevronOpenRotation else 0f,
        label = "breakdown_chevron",
    )
    Row(
        modifier = Modifier
            .clip(UI.shapes.rFull)
            .then(
                if (onDrawerToggle != null) {
                    Modifier.clickableNoIndication(rememberInteractionSource(), onDrawerToggle)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            modifier = Modifier
                .padding(bottom = 6.dp)
                .testTag("header_currency_symbol"),
            text = currencySymbol,
            style = UI.typo.nB1.style(fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.width(2.dp))
        Text(
            modifier = Modifier.testTag("header_amount"),
            text = amountText,
            style = UI.typo.nH2.style(fontWeight = FontWeight.ExtraBold),
        )
        if (onDrawerToggle != null) {
            Spacer(Modifier.width(4.dp))
            IvyIcon(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .rotate(chevronRotation),
                icon = R.drawable.ic_expand_more,
                tint = UI.colors.gray,
                contentDescription = stringResource(
                    if (drawerExpanded) R.string.hide_breakdown else R.string.show_breakdown
                ),
            )
        }
    }
}
