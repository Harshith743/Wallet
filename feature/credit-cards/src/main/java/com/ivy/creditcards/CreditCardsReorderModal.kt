package com.ivy.creditcards

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.creditcards.model.CreditCardUi
import com.ivy.creditcards.ui.ReorderableCard
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.wallet.ui.theme.components.ReorderModalSingleType
import kotlinx.collections.immutable.ImmutableList
import java.util.UUID

/** The legacy drag-to-reorder modal, listing the cards in their current order. */
@Composable
internal fun BoxScope.CreditCardsReorderModal(
    visible: Boolean,
    cards: ImmutableList<CreditCardUi>,
    onEvent: (CreditCardsUiEvent) -> Unit,
) {
    val modalId = remember { UUID.randomUUID() }
    val items = remember(cards) {
        cards.mapIndexed { index, card ->
            ReorderableCard(
                id = card.id,
                name = card.name,
                last4 = card.last4,
                color = card.color,
                orderNum = index.toDouble(),
            )
        }
    }
    ReorderModalSingleType(
        visible = visible,
        id = modalId,
        initialItems = items,
        dismiss = { onEvent(CreditCardsUiEvent.ReorderModalVisible(visible = false)) },
        onReordered = { reordered -> onEvent(CreditCardsUiEvent.Reorder(reordered.map { it.id })) },
    ) { _, item ->
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp)
                .padding(vertical = 8.dp),
            text = "${item.name} •• ${item.last4}",
            style = UI.typo.b1.style(
                color = item.color,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
