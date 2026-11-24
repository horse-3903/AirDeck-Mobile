package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kitswas.virtualgamepadmobile.R

val SlotColors = listOf(
    Color(0xFFE5484D), Color(0xFF3E8EF7), Color(0xFFF5C518),
    Color(0xFF30A46C), Color(0xFFF97316), Color(0xFFA855F7)
)

fun slotColor(slot: Int): Color = SlotColors[(slot - 1).coerceIn(0, SlotColors.size - 1)]

/**
 * Four dots like an Xbox controller ring, with the one for this phone's slot lit.
 */
@Composable
fun ControllerBadge(slot: Int, modifier: Modifier = Modifier) {
    val color = slotColor(slot)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (index in 1..6) {
            Row(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(slotColor(index).copy(alpha = if (index == slot) 1f else 0.28f))
            ) {}
        }
        Text(
            text = stringResource(R.string.notification_controller, slot),
            fontSize = 11.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            color = color
        )
    }
}
