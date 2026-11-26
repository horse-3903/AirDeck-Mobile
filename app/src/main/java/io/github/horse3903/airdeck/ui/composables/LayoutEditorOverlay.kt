package io.github.horse3903.airdeck.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.horse3903.airdeck.data.ButtonComponent
import io.github.horse3903.airdeck.data.LayoutGroup
import io.github.horse3903.airdeck.data.unionOf

/** What a touch landed on in the layout editor. */
sealed class EditorHit {
    data class Control(val component: ButtonComponent) : EditorHit()
    data class Group(val id: String) : EditorHit()
}

/** Padded bounds of a group: the "general area" that selects it when tapped. */
fun groupArea(group: LayoutGroup, bounds: Map<ButtonComponent, Rect>, padding: Float): Rect? =
    unionOf(group.members.mapNotNull { bounds[it] })?.inflate(padding)

/**
 * Sits over the editable pad and does all touch handling for the editor: a touch on a control picks it,
 * a touch in the empty area of a group picks the group. It also draws the group outlines and the
 * alignment guides.
 */
@Composable
fun LayoutEditorOverlay(
    bounds: Map<ButtonComponent, Rect>,
    groups: List<LayoutGroup>,
    selectedGroupId: String?,
    guideX: Float?,
    guideY: Float?,
    onTap: (EditorHit?) -> Unit,
    onDragStart: (EditorHit?) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val pad = with(density) { 12.dp.toPx() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val currentBounds by rememberUpdatedState(bounds)
    val currentGroups by rememberUpdatedState(groups)
    val currentOrigin by rememberUpdatedState(origin)
    val tap by rememberUpdatedState(onTap)
    val dragStart by rememberUpdatedState(onDragStart)
    val drag by rememberUpdatedState(onDrag)
    val dragEnd by rememberUpdatedState(onDragEnd)

    fun hit(local: Offset): EditorHit? {
        val p = local + currentOrigin
        currentBounds.entries
            .filter { it.value.contains(p) }
            .minByOrNull { it.value.width * it.value.height }
            ?.let { return EditorHit.Control(it.key) }
        return currentGroups
            .mapNotNull { g -> groupArea(g, currentBounds, pad)?.let { g to it } }
            .filter { it.second.contains(p) }
            .minByOrNull { it.second.width * it.second.height }
            ?.let { EditorHit.Group(it.first.id) }
    }

    val primary = MaterialTheme.colorScheme.primary
    val faint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(Unit) { detectTapGestures { tap(hit(it)) } }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragStart(hit(it)) },
                    onDragEnd = { dragEnd() },
                    onDragCancel = { dragEnd() },
                    onDrag = { change, amount -> change.consume(); drag(amount) }
                )
            }
    ) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()))
        for (g in groups) {
            val area = groupArea(g, bounds, pad) ?: continue
            val selected = g.id == selectedGroupId
            val r = area.translate(-origin)
            drawRoundRect(
                color = if (selected) primary else faint,
                topLeft = r.topLeft,
                size = Size(r.width, r.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                style = Stroke(
                    width = if (selected) 2.dp.toPx() else 1.dp.toPx(),
                    pathEffect = if (selected) null else dash
                )
            )
        }
        guideX?.let {
            val x = it - origin.x
            drawLine(primary.copy(alpha = 0.8f), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
        }
        guideY?.let {
            val y = it - origin.y
            drawLine(primary.copy(alpha = 0.8f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
    }
}
