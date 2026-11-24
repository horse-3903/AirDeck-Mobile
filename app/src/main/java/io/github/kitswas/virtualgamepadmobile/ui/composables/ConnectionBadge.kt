package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class LinkStatus { CONNECTED, CONNECTING, DISCONNECTED }

private val StatusGreen = Color(0xFF30A46C)
private val StatusYellow = Color(0xFFE0A418)
private val StatusRed = Color(0xFFE5484D)

fun linkColor(status: LinkStatus): Color = when (status) {
    LinkStatus.CONNECTED -> StatusGreen
    LinkStatus.CONNECTING -> StatusYellow
    LinkStatus.DISCONNECTED -> StatusRed
}

private fun DrawScope.linkRect(color: Color, x: Float, w: Float, h: Float, filled: Boolean, stroked: Boolean) {
    val top = Offset(x, -h / 2f)
    val size = Size(w, h)
    val radius = CornerRadius(h / 2f, h / 2f)
    drawRoundRect(color.copy(alpha = if (filled) 1f else 0.2f), top, size, radius)
    if (stroked) {
        drawRoundRect(color, top, size, radius, style = Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun DrawScope.shape(color: Color, d: String, alpha: Float, stroked: Boolean) {
    val path = PathParser().parsePathString(d).toPath()
    drawPath(path, color.copy(alpha = alpha))
    if (stroked) drawPath(path, color, style = Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Chain, broken chain and hourglass, drawn on a 24 unit grid so they scale with the badge. */
@Composable
fun LinkStatusIcon(status: LinkStatus, modifier: Modifier = Modifier) {
    val color = linkColor(status)
    Canvas(modifier = modifier) {
        scale(size.width / 24f, pivot = Offset.Zero) {
            when (status) {
                LinkStatus.CONNECTED -> translate(12f, 12f) {
                    rotate(-45f, Offset.Zero) {
                        linkRect(color, -10.6f, 8.4f, 6.4f, filled = false, stroked = true)
                        linkRect(color, 2.2f, 8.4f, 6.4f, filled = false, stroked = true)
                        drawRoundRect(color, Offset(-4.8f, -1.3f), Size(9.6f, 2.6f), CornerRadius(1.3f, 1.3f))
                    }
                }

                LinkStatus.DISCONNECTED -> translate(12f, 12f) {
                    rotate(-45f, Offset.Zero) {
                        linkRect(color, -11.4f, 8.4f, 6.4f, filled = false, stroked = true)
                        linkRect(color, 3.0f, 8.4f, 6.4f, filled = false, stroked = true)
                        drawRoundRect(color, Offset(-5.6f, -1.3f), Size(4.0f, 2.6f), CornerRadius(1.1f, 1.1f))
                        drawRoundRect(color, Offset(1.6f, -1.3f), Size(4.0f, 2.6f), CornerRadius(1.1f, 1.1f))
                    }
                }

                LinkStatus.CONNECTING -> {
                    shape(color, "M7.2 4.2C7.2 8.2 10.6 10.2 10.6 12C10.6 13.8 7.2 15.8 7.2 19.8H16.8C16.8 15.8 13.4 13.8 13.4 12C13.4 10.2 16.8 8.2 16.8 4.2Z", 0.2f, true)
                    shape(color, "M6.5 2.2H17.5A1 1 0 0 1 17.5 4.2H6.5A1 1 0 0 1 6.5 2.2Z", 0.45f, true)
                    shape(color, "M6.5 19.8H17.5A1 1 0 0 1 17.5 21.8H6.5A1 1 0 0 1 6.5 19.8Z", 0.45f, true)
                    shape(color, "M8.7 6.5Q12 7.8 15.3 6.5C14.7 8.8 13 10.1 12.3 11.4H11.7C11 10.1 9.3 8.8 8.7 6.5Z", 0.85f, false)
                    shape(color, "M8.2 19.1C9.5 18.6 11.1 16.1 12 16.1C12.9 16.1 14.5 18.6 15.8 19.1Z", 0.85f, false)
                }
            }
        }
    }
}

/**
 * Compact connection status: neutral surface, a border in the status colour, the status icon and the
 * controller slot in its own colour. Tapping it asks to disconnect.
 */
@Composable
fun ConnectionBadge(
    status: LinkStatus,
    slot: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 32.dp,
) {
    val shape = RoundedCornerShape(height * 0.2f)
    Row(
        modifier = modifier
            .width(height * 2f)
            .height(height)
            .clip(shape)
            .background(PsStyle.Body)
            .border(1.5.dp, linkColor(status), shape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        LinkStatusIcon(status, Modifier.size(height * 0.5f))
        Text(
            text = if (slot != null && status == LinkStatus.CONNECTED) "P$slot" else "\u2014",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (slot != null && status == LinkStatus.CONNECTED) slotColor(slot) else Color.Gray
        )
    }
}

/** A soft glow along the screen edges in the controller's slot colour; it never takes touches. */
@Composable
fun SlotGlow(slot: Int?, modifier: Modifier = Modifier) {
    val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val prefs = LocalAppPrefs.current
    val strength = prefs.glowStrength / 100f
    val alpha by animateFloatAsState(
        targetValue = if (slot == null || !prefs.glowEnabled) 0f else (if (light) 0.16f else 0.14f) * strength,
        animationSpec = tween(500),
        label = "slotGlow"
    )
    val color = slotColor(slot ?: 1)
    Canvas(modifier = modifier.fillMaxSize()) {
        // Fades quickly like the browser's inset shadow: strongest at the very edge, gone by 12 percent of the height
        val d = size.height * 0.12f
        val stops = arrayOf(
            0f to color.copy(alpha = alpha),
            0.35f to color.copy(alpha = alpha * 0.4f),
            1f to Color.Transparent
        )
        val reversed = arrayOf(
            0f to Color.Transparent,
            0.65f to color.copy(alpha = alpha * 0.4f),
            1f to color.copy(alpha = alpha)
        )
        drawRect(Brush.verticalGradient(colorStops = stops, startY = 0f, endY = d), Offset.Zero, Size(size.width, d))
        drawRect(Brush.verticalGradient(colorStops = reversed, startY = size.height - d, endY = size.height), Offset(0f, size.height - d), Size(size.width, d))
        drawRect(Brush.horizontalGradient(colorStops = stops, startX = 0f, endX = d), Offset.Zero, Size(d, size.height))
        drawRect(Brush.horizontalGradient(colorStops = reversed, startX = size.width - d, endX = size.width), Offset(size.width - d, 0f), Size(d, size.height))
    }
}

/** Small round settings button that sits level with the Back button and the connection badge. */
@Composable
fun GearButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 32.dp,
) {
    val shape = RoundedCornerShape(height * 0.2f)
    val glyph = PsStyle.Glyph
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .width(height * 1.14f)
            .height(height)
            .clip(shape)
            .background(PsStyle.Body)
            .border(1.5.dp, PsStyle.Border, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(height * 0.5f)) {
            scale(size.width / 24f, pivot = Offset.Zero) {
                val stroke = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawCircle(glyph, radius = 3.2f, center = Offset(12f, 12f), style = stroke)
                drawPath(PathParser().parsePathString(GEAR_PATH).toPath(), glyph, style = stroke)
            }
        }
    }
}

private const val GEAR_PATH =
    "M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.9.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.9l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.9.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.9-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.9V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"
