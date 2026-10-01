package com.ivy.ui.haze

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

/**
 * Frosted glass (Haze) as used by the bottom bar and the tab headers: the content scrolling
 * behind a panel is blurred under a theme-coloured tint, with a hairline on one edge.
 */
object FrostedGlassDefaults {
    val BlurRadius: Dp = 24.dp
    const val TintAlpha = 0.55f
    const val Noise = 0.05f
    const val HairlineAlpha = 0.4f
    val HairlineWidth: Dp = 1.dp

    /** Opacity of the plain scrim that stands in for the blur in previews and screenshot tests. */
    const val InspectionScrimAlpha = 0.95f
}

enum class HairlineEdge {
    Top,
    Bottom,
}

/** The hairline drawn on one edge of a frosted panel. */
@Immutable
data class FrostedHairline(
    val edge: HairlineEdge,
    val color: Color,
    val widthPx: Float,
)

/** The style every frosted panel uses: [tint] is the theme's "pure" colour (black on dark, white on light). */
@Composable
fun frostedGlassStyle(background: Color, tint: Color): HazeStyle = HazeDefaults.style(
    backgroundColor = background,
    tint = tint.copy(alpha = FrostedGlassDefaults.TintAlpha),
    blurRadius = FrostedGlassDefaults.BlurRadius,
    noiseFactor = FrostedGlassDefaults.Noise,
)

/**
 * Paints this node as a frosted panel over the content recorded by [state]'s `haze` source
 * (which must be drawn before this node), with [hairline] on one edge. When [inspectionScrim]
 * is given (previews, screenshot tests) a plain scrim replaces the blur.
 */
fun Modifier.frostedPanel(
    state: HazeState,
    hairline: FrostedHairline,
    inspectionScrim: Color?,
): Modifier {
    val backdrop = if (inspectionScrim != null) background(inspectionScrim) else hazeChild(state = state)
    return backdrop.drawBehind {
        val y = if (hairline.edge == HairlineEdge.Top) 0f else size.height
        drawLine(
            color = hairline.color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = hairline.widthPx,
        )
    }
}

private enum class Slot {
    Header,
    Content,
}

/**
 * Lays [content] out under a [header] that is drawn on top of it: the header is measured
 * first and its height is handed to [content] as the top padding to reserve, all in one
 * pass. The header is placed last so a frosted header paints over the scrolling content.
 */
@Composable
fun FrostedHeaderScaffold(
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (headerHeight: Dp) -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val headerPlaceables = subcompose(Slot.Header, header).map {
            it.measure(constraints.copy(minHeight = 0))
        }
        val headerHeight = headerPlaceables.maxOfOrNull { it.height } ?: 0
        val contentPlaceables = subcompose(Slot.Content) { content(headerHeight.toDp()) }.map {
            it.measure(constraints)
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            contentPlaceables.forEach { it.place(0, 0) }
            headerPlaceables.forEach { it.place(0, 0) }
        }
    }
}
