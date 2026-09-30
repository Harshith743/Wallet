package com.ivy.creditcards.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.creditcards.model.CreditCardUi
import com.ivy.data.model.AccountId
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.IvyOutlinedButton
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.pureBlur
import kotlinx.collections.immutable.ImmutableList

object CarouselDefaults {
    val ThumbnailWidth: Dp = 72.dp
    val ThumbnailHeight: Dp = 46.dp
    val StripHeight: Dp = 66.dp

    /**
     * Space to leave under the strip, above the navigation bar inset: the main "+"
     * button spans 30–86 dp above that inset (see feature/main MainBottomBar.kt), so
     * 94 dp keeps the strip clear of it by 8 dp.
     */
    val BottomClearance: Dp = 94.dp
}

/**
 * The persistent strip of mini cards under the stack: "ALL (N)", one tappable thumbnail
 * per card (the active one is outlined) and a sticky "Add card" button.
 */
@Composable
fun CreditCardsCarousel(
    cards: ImmutableList<CreditCardUi>,
    activeCardId: AccountId?,
    onSelectCard: (AccountId) -> Unit,
    onAddCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(activeCardId, cards) {
        val index = cards.indexOfFirst { it.id == activeCardId }
        val visible = listState.layoutInfo.visibleItemsInfo.any { info ->
            info.index == index &&
                info.offset >= listState.layoutInfo.viewportStartOffset &&
                info.offset + info.size <= listState.layoutInfo.viewportEndOffset
        }
        if (index >= 0 && !visible) listState.animateScrollToItem(index)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(pureBlur())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.all_cards_count, cards.size).uppercase(),
            style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Bold).copy(letterSpacing = 1.sp),
            maxLines = 1,
        )
        LazyRow(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(count = cards.size, key = { cards[it].id.value }) { index ->
                val card = cards[index]
                CardThumbnail(
                    card = card,
                    selected = card.id == activeCardId,
                    onClick = { onSelectCard(card.id) },
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        IvyOutlinedButton(
            text = stringResource(R.string.add_card),
            iconStart = R.drawable.ic_add,
            solidBackground = true,
            padding = 8.dp,
            onClick = onAddCard,
        )
    }
}

@Composable
fun CardThumbnail(
    card: CreditCardUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val contrast = findContrastTextColor(card.color)
    Box(
        modifier = modifier
            .size(width = CarouselDefaults.ThumbnailWidth, height = CarouselDefaults.ThumbnailHeight)
            .clip(shape)
            .background(card.color)
            .border(2.dp, if (selected) UI.colors.pureInverse else Color.Transparent, shape)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = card.issuer.uppercase(),
                style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = contrast),
                maxLines = 1,
            )
            Text(
                text = "•• ${card.last4}",
                style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = contrast),
                maxLines = 1,
            )
        }
    }
}
