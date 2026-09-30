package com.ivy.creditcards.skin

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor

/** The paint for a face: a solid colour for one stop, a diagonal gradient otherwise. */
fun CardSkinUi.brush(): Brush = if (colors.size == 1) SolidColor(colors[0]) else Brush.linearGradient(colors)
