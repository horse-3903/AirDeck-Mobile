package io.github.horse3903.airdeck.ui.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.horse3903.airdeck.data.ButtonComponent
import io.github.horse3903.airdeck.data.ButtonConfig

/**
 * Places a gamepad component at its configured offset.
 * In [editing] mode hidden components are dimmed instead of removed, the selected one gets an outline,
 * and its bounds are reported so the editor can hit-test, group and snap. Touches are handled by the
 * editor's overlay, not here.
 */
@Composable
fun EditableComponent(
    component: ButtonComponent,
    config: ButtonConfig,
    baseDp: Int,
    editing: Boolean,
    onBounds: ((ButtonComponent, Rect) -> Unit)?,
    selected: Boolean = false,
    content: @Composable (Modifier) -> Unit,
) {
    val offsetModifier = Modifier.offset(
        x = (config.offsetX * baseDp).dp,
        y = (config.offsetY * baseDp).dp
    )
    if (!editing) {
        content(offsetModifier)
        return
    }
    val currentOnBounds by rememberUpdatedState(onBounds)
    Box(
        modifier = offsetModifier
            .alpha(if (config.visible) 1f else 0.3f)
            .onGloballyPositioned { currentOnBounds?.invoke(component, it.boundsInRoot()) }
            .then(
                if (selected) Modifier.border(
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    RoundedCornerShape(10.dp)
                ) else Modifier
            )
    ) {
        content(Modifier)
    }
}
