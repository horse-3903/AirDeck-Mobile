package io.github.horse3903.airdeck.ui.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.getValue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.horse3903.airdeck.R
import io.github.horse3903.airdeck.data.ColorScheme
import io.github.horse3903.airdeck.data.GamepadStyle

/**
 * A row of mutually exclusive options in one rounded container, the selected one filled.
 * [segment] draws the inside of one option.
 */
@Composable
fun <T> SegmentedChoice(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    segment: @Composable (T, Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                val fill by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                    tween(200), label = "fill"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(fill)
                        .clickable { onSelect(option) }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    segment(option, isSelected)
                }
            }
        }
    }
}

@Composable
private fun SegmentLabel(text: String, selected: Boolean) {
    val color by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        tween(200), label = "label"
    )
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = color
    )
}

/** Light, Dark and System as icon-over-label segments. */
@Composable
fun ColorSchemeChoice(
    selected: ColorScheme,
    onSelect: (ColorScheme) -> Unit,
    labelFor: @Composable (ColorScheme) -> String,
    modifier: Modifier = Modifier,
) {
    SegmentedChoice(
        options = listOf(ColorScheme.LIGHT, ColorScheme.DARK, ColorScheme.SYSTEM),
        selected = selected,
        onSelect = onSelect,
        modifier = modifier
    ) { scheme, isSelected ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(
                painter = painterResource(
                    when (scheme) {
                        ColorScheme.LIGHT -> R.drawable.ic_light_mode
                        ColorScheme.DARK -> R.drawable.ic_dark_mode
                        ColorScheme.SYSTEM -> R.drawable.ic_desktop
                    }
                ),
                contentDescription = null,
                tint = animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    tween(200), label = "tint"
                ).value,
                modifier = Modifier.size(20.dp)
            )
            SegmentLabel(labelFor(scheme), isSelected)
        }
    }
}

/** Xbox and PlayStation, each showing its own face-button symbols above the name. */
@Composable
fun GamepadStyleChoice(
    selected: GamepadStyle,
    onSelect: (GamepadStyle) -> Unit,
    labelFor: @Composable (GamepadStyle) -> String,
    modifier: Modifier = Modifier,
) {
    SegmentedChoice(
        options = listOf(GamepadStyle.XBOX, GamepadStyle.PLAYSTATION),
        selected = selected,
        onSelect = onSelect,
        modifier = modifier
    ) { style, isSelected ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            when (style) {
                GamepadStyle.XBOX -> XboxSymbols()
                GamepadStyle.PLAYSTATION -> PlayStationSymbols()
            }
            SegmentLabel(labelFor(style), isSelected)
        }
    }
}

@Composable
internal fun XboxSymbols() {
    Box(Modifier.size(width = 40.dp, height = 28.dp)) {
        @Composable
        fun letter(text: String, color: Color, align: Alignment) {
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.align(align)
            )
        }
        letter("Y", Color(0xFFF5C518), Alignment.TopCenter)
        letter("X", Color(0xFF3E8EF7), Alignment.CenterStart)
        letter("B", Color(0xFFE5484D), Alignment.CenterEnd)
        letter("A", Color(0xFF30A46C), Alignment.BottomCenter)
    }
}

@Composable
internal fun PlayStationSymbols() {
    Canvas(Modifier.size(width = 40.dp, height = 28.dp)) {
        val stroke = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val r = 3.6.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val dx = 12.dp.toPx()
        val dy = 8.dp.toPx()
        // triangle on top
        val tri = Path().apply {
            moveTo(cx, cy - dy - r)
            lineTo(cx + r * 1.1f, cy - dy + r * 0.8f)
            lineTo(cx - r * 1.1f, cy - dy + r * 0.8f)
            close()
        }
        drawPath(tri, Color(0xFF30A46C), style = stroke)
        // square on the left
        drawRect(Color(0xFFC850A8), Offset(cx - dx - r, cy - r), Size(r * 2, r * 2), style = stroke)
        // circle on the right
        drawCircle(Color(0xFFE5394D), r, Offset(cx + dx, cy), style = stroke)
        // cross at the bottom
        val cross = Color(0xFF2F6FE0)
        val by = cy + dy
        drawLine(cross, Offset(cx - r, by - r), Offset(cx + r, by + r), stroke.width, StrokeCap.Round)
        drawLine(cross, Offset(cx + r, by - r), Offset(cx - r, by + r), stroke.width, StrokeCap.Round)
    }
}
