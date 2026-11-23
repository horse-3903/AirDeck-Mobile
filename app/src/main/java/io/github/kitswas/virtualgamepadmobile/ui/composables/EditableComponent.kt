package io.github.kitswas.virtualgamepadmobile.ui.composables

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.kitswas.virtualgamepadmobile.data.ButtonComponent
import io.github.kitswas.virtualgamepadmobile.data.ButtonConfig

/**
 * Places a gamepad component at its configured offset.
 * With [onTap] set (editor mode) the component is drawn with an outline, hidden components are
 * dimmed instead of removed, and every touch on it opens its settings instead of acting as input.
 */
@Composable
fun EditableComponent(
    component: ButtonComponent,
    config: ButtonConfig,
    baseDp: Int,
    onTap: ((ButtonComponent) -> Unit)?,
    onDrag: ((ButtonComponent, Dp, Dp) -> Unit)? = null,
    selected: Boolean = false,
    content: @Composable (Modifier) -> Unit,
) {
    val offsetModifier = Modifier.offset(
        x = (config.offsetX * baseDp).dp,
        y = (config.offsetY * baseDp).dp
    )
    if (onTap == null) {
        content(offsetModifier)
        return
    }
    val density = LocalDensity.current
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDrag by rememberUpdatedState(onDrag)
    Box(
        modifier = offsetModifier
            .alpha(if (config.visible) 1f else 0.3f)
            .border(
                if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                RoundedCornerShape(10.dp)
            )
    ) {
        content(Modifier)
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(component) {
                    detectTapGestures(onTap = { currentOnTap?.invoke(component) })
                }
                .pointerInput(component) {
                    detectDragGestures(
                        onDragStart = { currentOnTap?.invoke(component) },
                        onDrag = { change, amount ->
                            change.consume()
                            with(density) {
                                currentOnDrag?.invoke(component, amount.x.toDp(), amount.y.toDp())
                            }
                        }
                    )
                }
        )
    }
}
