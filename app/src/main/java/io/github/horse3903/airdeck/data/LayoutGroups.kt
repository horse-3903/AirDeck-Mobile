package io.github.horse3903.airdeck.data

import androidx.compose.ui.geometry.Rect
import io.github.horse3903.airdeck.data.ButtonComponent.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs

/**
 * Controls that move together in the layout editor. A control belongs to at most one group.
 * Groups only exist while editing, they have no effect on play.
 */
@Serializable
data class LayoutGroup(
    val id: String,
    val name: String,
    val members: List<ButtonComponent>,
)

val defaultLayoutGroups = listOf(
    LayoutGroup("left_shoulders", "Left shoulders", listOf(LEFT_TRIGGER, LEFT_SHOULDER)),
    LayoutGroup("right_shoulders", "Right shoulders", listOf(RIGHT_TRIGGER, RIGHT_SHOULDER)),
    LayoutGroup("left_controls", "Left controls", listOf(LEFT_ANALOG_STICK, DPAD, SELECT_BUTTON)),
    LayoutGroup("right_controls", "Right controls", listOf(FACE_BUTTONS, RIGHT_ANALOG_STICK, START_BUTTON)),
    LayoutGroup("top_bar", "Top bar", listOf(BACK_BUTTON, CONNECTION_BADGE, SETTINGS_BUTTON)),
)

fun encodeGroups(groups: List<LayoutGroup>): String = Json.encodeToString(groups)

fun decodeGroups(text: String?): List<LayoutGroup>? =
    text?.let { runCatching { Json.decodeFromString<List<LayoutGroup>>(it) }.getOrNull() }

fun groupOf(groups: List<LayoutGroup>, component: ButtonComponent): LayoutGroup? =
    groups.firstOrNull { component in it.members }

/** Takes [component] out of every group and, if [targetId] is given, puts it in that one. Empty groups disappear. */
fun moveToGroup(groups: List<LayoutGroup>, component: ButtonComponent, targetId: String?): List<LayoutGroup> =
    groups.map { g ->
        val without = g.members - component
        if (g.id == targetId) g.copy(members = without + component) else g.copy(members = without)
    }.filter { it.members.isNotEmpty() }

fun withNewGroup(groups: List<LayoutGroup>, component: ButtonComponent): List<LayoutGroup> {
    var n = 1
    while (groups.any { it.id == "custom_$n" }) n++
    return moveToGroup(groups, component, null) + LayoutGroup("custom_$n", "Group $n", listOf(component))
}

fun dissolveGroup(groups: List<LayoutGroup>, id: String): List<LayoutGroup> = groups.filter { it.id != id }

fun unionOf(rects: Collection<Rect>): Rect? {
    if (rects.isEmpty()) return null
    return Rect(
        rects.minOf { it.left }, rects.minOf { it.top },
        rects.maxOf { it.right }, rects.maxOf { it.bottom }
    )
}

/** Alignment lines the snapping locked onto, in the same pixel space as the rectangles. */
class SnapResult(val dx: Float, val dy: Float, val xLine: Float?, val yLine: Float?)

/**
 * A light magnet: if an edge or the centre of [moving] is within [threshold] of an edge or centre of
 * another rectangle, report how far to nudge so they line up.
 */
fun snapPosition(moving: Rect, others: List<Rect>, threshold: Float): SnapResult {
    var bestDx = 0f
    var bestX: Float? = null
    var bestDy = 0f
    var bestY: Float? = null
    val movingX = listOf(moving.left, moving.center.x, moving.right)
    val movingY = listOf(moving.top, moving.center.y, moving.bottom)
    for (o in others) {
        for (target in listOf(o.left, o.center.x, o.right)) {
            for (m in movingX) {
                val d = target - m
                if (abs(d) <= threshold && (bestX == null || abs(d) < abs(bestDx))) {
                    bestDx = d; bestX = target
                }
            }
        }
        for (target in listOf(o.top, o.center.y, o.bottom)) {
            for (m in movingY) {
                val d = target - m
                if (abs(d) <= threshold && (bestY == null || abs(d) < abs(bestDy))) {
                    bestDy = d; bestY = target
                }
            }
        }
    }
    return SnapResult(bestDx, bestY?.let { bestDy } ?: 0f, bestX, bestY)
}

/**
 * Nudges a new size so the control's width or height matches another control's, when already within
 * [tolerance] (a fraction). [self] is the control's bounds at its [oldScale].
 */
fun snapScale(oldScale: Float, newScale: Float, self: Rect, others: List<Rect>, tolerance: Float = 0.05f): Float {
    if (self.width <= 0f || self.height <= 0f || oldScale <= 0f) return newScale
    var best = newScale
    var bestError = tolerance
    for (o in others) {
        for (candidate in listOf(oldScale * o.width / self.width, oldScale * o.height / self.height)) {
            val error = abs(candidate / newScale - 1f)
            if (error < bestError) {
                bestError = error
                best = candidate
            }
        }
    }
    return best.coerceIn(SCALE_VALUE_RANGE)
}
