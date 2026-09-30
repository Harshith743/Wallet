package com.ivy.creditcards.ui

import androidx.compose.ui.graphics.Color
import com.ivy.data.model.AccountId
import com.ivy.wallet.domain.data.Reorderable

/**
 * A card row for the legacy reorder modal. A plain class (not a data class) because the
 * modal contract requires [withNewOrderNum] and [getItemOrderNum].
 */
class ReorderableCard(
    val id: AccountId,
    val name: String,
    val last4: String,
    val color: Color,
    private val orderNum: Double,
) : Reorderable {
    override fun getItemOrderNum(): Double = orderNum

    override fun withNewOrderNum(newOrderNum: Double): Reorderable = ReorderableCard(
        id = id,
        name = name,
        last4 = last4,
        color = color,
        orderNum = newOrderNum,
    )
}
