package io.github.horse3903.airdeck.ui.screens

import io.github.horse3903.airdeck.data.defaultStickSnap
import io.github.horse3903.airdeck.data.defaultDpadSingle
import io.github.horse3903.airdeck.data.defaultGamepadStyle
import io.github.horse3903.airdeck.data.withBehaviour
import io.github.horse3903.airdeck.data.GamepadStyle
import android.os.Parcelable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import io.github.horse3903.airdeck.data.ButtonComponent
import io.github.horse3903.airdeck.data.ButtonConfig
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.activity.compose.BackHandler
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import io.github.horse3903.airdeck.ui.utils.HapticPrefs
import io.github.horse3903.airdeck.ui.utils.HapticUtils
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.gestures.detectTapGestures
import io.github.horse3903.airdeck.data.Curve
import io.github.horse3903.airdeck.data.AppPrefs
import io.github.horse3903.airdeck.data.applyCurve
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.horse3903.airdeck.data.defaultButtonConfigs
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import io.github.horse3903.airdeck.R
import io.github.horse3903.airdeck.data.ColorScheme
import io.github.horse3903.airdeck.data.PreviewBase
import io.github.horse3903.airdeck.data.PreviewHeightDp
import io.github.horse3903.airdeck.data.PreviewWidthDp
import io.github.horse3903.airdeck.data.SettingsRepository
import io.github.horse3903.airdeck.data.defaultColorScheme
import io.github.horse3903.airdeck.data.defaultFullScreenEnabled
import io.github.horse3903.airdeck.data.defaultHapticFeedbackEnabled
import io.github.horse3903.airdeck.data.defaultPollingDelay
import io.github.horse3903.airdeck.data.defaultSaveConnectionCredentials
import io.github.horse3903.airdeck.ui.composables.ColorSchemeChoice
import io.github.horse3903.airdeck.ui.composables.GamepadStyleChoice
import io.github.horse3903.airdeck.ui.composables.SpinBox
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize

private const val logTag = "SettingsScreen"

@Parcelize
private data class SettingsChanges(
    val colorScheme: ColorScheme? = null,
    val pollingDelay: Int? = null,
    val hapticFeedbackEnabled: Boolean? = null,
    val saveConnectionCredentials: Boolean? = null,
    val fullScreenEnabled: Boolean? = null
) : Parcelable

// Every row's control sits in one column of this width, so the controls line up down the screen
private val CONTROL_COLUMN = 248.dp

// The content column is wide enough for the controls without being stretched edge to edge
private val SETTINGS_WIDTH = 720.dp

@Composable
private fun SettingRow(
    label: String,
    labelExtra: @Composable () -> Unit = {},
    helper: String? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
    indent: Dp = 0.dp,
    enabled: Boolean = true,
    controlWidth: Dp = CONTROL_COLUMN,
    control: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = SETTINGS_WIDTH)
            .fillMaxWidth()
            .padding(start = 24.dp + indent, end = 24.dp)
            .alpha(if (enabled) 1f else 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp, horizontal = 0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium))
                    labelExtra()
                }
                if (helper != null) {
                    Text(
                        text = helper,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(controlWidth)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.CenterEnd
            ) { control() }
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        }
    }
}

@Composable
private fun ResetChip(overridden: Boolean, onReset: (() -> Unit)?) {
    if (overridden && onReset != null) {
        TextButton(onClick = onReset, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(28.dp)) {
            Text(stringResource(R.string.pref_reset_row), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    helper: String? = null,
    checked: Boolean,
    overridden: Boolean = false,
    onReset: (() -> Unit)? = null,
    onChange: (Boolean) -> Unit
) {
    SettingRow(label = label, labelExtra = { ResetChip(overridden, onReset) }, helper = helper) {
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.scale(0.85f))
    }
}

/** A row with a few short choices side by side. */
@Composable
private fun <T> ChoiceRow(
    label: String,
    helper: String?,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    overridden: Boolean = false,
    onReset: (() -> Unit)? = null,
    enabled: Boolean = true,
    indent: Dp = 0.dp,
    onSelect: (T) -> Unit
) {
    SettingRow(label = label, labelExtra = { ResetChip(overridden, onReset) }, helper = helper, controlWidth = 300.dp, enabled = enabled, indent = indent) {
        io.github.horse3903.airdeck.ui.composables.SegmentedChoice(
            options = options,
            selected = selected,
            onSelect = onSelect,
            modifier = Modifier.fillMaxWidth()
        ) { option, isSelected ->
            Text(
                optionLabel(option),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/** Chooses where a change on a settings page is saved: for every layout, or only for the layout in use. */
@Composable
private fun ScopeBar(layoutLabel: String, scopeLayout: Boolean, onChange: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = SETTINGS_WIDTH)
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        io.github.horse3903.airdeck.ui.composables.SegmentedChoice(
            options = listOf(false, true),
            selected = scopeLayout,
            onSelect = onChange,
            modifier = Modifier.fillMaxWidth()
        ) { layout, isSelected ->
            Text(
                if (layout) stringResource(R.string.scope_this) else stringResource(R.string.scope_all),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Text(
            if (scopeLayout) stringResource(R.string.scope_layout_hint, layoutLabel) else stringResource(R.string.scope_all_hint),
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderRow(
    label: String,
    helper: String? = null,
    valueText: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    onFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
    indent: Dp = 0.dp,
    overridden: Boolean = false,
    onReset: (() -> Unit)? = null,
) {
    SettingRow(label = label, labelExtra = { ResetChip(overridden, onReset) }, helper = helper, indent = indent, enabled = enabled) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Slider(
                value = value.toFloat(),
                onValueChange = { onChange(it.toInt()) },
                onValueChangeFinished = { onFinished?.invoke() },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                enabled = enabled,
                modifier = Modifier.weight(1f),
                thumb = {
                    Box(
                        Modifier
                            .size(20.dp)
                            .shadow(3.dp, CircleShape)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                },
                track = { state ->
                    val span = state.valueRange.endInclusive - state.valueRange.start
                    val fraction = if (span > 0f) (state.value - state.valueRange.start) / span else 0f
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.onSurface)
                        )
                    }
                }
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(52.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, letterSpacing = 0.8.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .widthIn(max = SETTINGS_WIDTH)
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 4.dp)
    )
}

@Composable
private fun CategoryItem(label: String, selected: Boolean, expanded: Boolean? = null, onToggle: () -> Unit = {}, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = if (expanded == null) 14.dp else 4.dp, top = if (expanded == null) 12.dp else 0.dp, bottom = if (expanded == null) 12.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            maxLines = 1,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        if (expanded != null) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (expanded) "\u2304" else "\u203A",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NavRow(label: String, helper: String? = null, onClick: () -> Unit) {
    SettingRow(label = label, helper = helper, onClick = onClick) {
        Text(
            text = "\u203A",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A curve drawn into a small box, for headers and preset cards. */
@Composable
private fun MiniCurve(f: (Float) -> Float, modifier: Modifier, color: Color) {
    androidx.compose.foundation.Canvas(modifier) {
        val path = androidx.compose.ui.graphics.Path()
        for (i in 0..40) {
            val x = i / 40f
            val y = f(x)
            if (i == 0) path.moveTo(0f, size.height - y * size.height) else path.lineTo(x * size.width, size.height - y * size.height)
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
    }
}

private val SensColor = Color(0xFFCBB78D)
private val UsageColor = Color(0xFF71B69A)
private val NegColor = Color(0xFFCF7777)

/** Moves point [i] to (x, y), keeping the points in order and, unless decreasing curves are allowed, never below the one on its left or above the one on its right. */
private fun placed(c: Curve, i: Int, x: Int, y: Int): Curve {
    val pv = c.points.getOrNull(i - 1)
    val nx = c.points.getOrNull(i + 1)
    val lo = (pv?.first ?: 0) + 1
    val hi = (nx?.first ?: 100) - 1
    val px = x.coerceIn(lo, maxOf(lo, hi))
    var py = y.coerceIn(0, 100)
    if (!c.free) py = py.coerceIn(pv?.second ?: 0, nx?.second ?: 100)
    return c.copy(points = c.points.mapIndexed { n, p -> if (n == i) px to py else p })
}

private fun sensOf(f: (Float) -> Float, v: Float): Float {
    val h = 0.004f
    val a = (v - h).coerceAtLeast(0f)
    val b = (v + h).coerceAtMost(1f)
    return (f(b) - f(a)) / (b - a)
}

@Composable
private fun StepField(label: String, value: Int?, enabled: Boolean, onChange: (Int) -> Unit) {
    Column {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { onChange(-1) }, enabled = enabled, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(32.dp)) { Text("−") }
            Text(if (value == null) "-" else "$value%", fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.width(46.dp))
            OutlinedButton(onClick = { onChange(1) }, enabled = enabled, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(32.dp)) { Text("+") }
        }
    }
}

/**
 * A response curve setting that folds away. Closed it shows the preset in use and a small preview. Open it is a tuning workspace:
 * preset cards, an editing toolbar for Custom, the main graph on the left and on the right the analysis (the values at the chosen point,
 * the sensitivity of the curve and, where it applies, where the stick or trigger has actually been). Tap the graph to add a point
 * (up to 10), drag to move one, double tap to remove it. Touching a preset's graph turns it into an editable Custom curve.
 * Output may only rise unless decreasing curves are allowed.
 */
@Composable
private fun CurveSection(
    title: String,
    helper: String,
    kind: Int,
    custom: Curve,
    overridden: Boolean,
    onReset: () -> Unit,
    onApply: (Int?, Curve?) -> Unit,
    inLabel: String,
    outLabel: String,
    indent: Dp = 0.dp,
    usageLabel: String? = null,
    usageSet: io.github.horse3903.airdeck.data.Usage? = null,
    captions: List<Int> = listOf(R.string.curve_cap_linear, R.string.curve_cap_precise, R.string.curve_cap_quick, R.string.curve_cap_custom),
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    val names = listOf(R.string.curve_linear, R.string.curve_precise, R.string.curve_quick, R.string.curve_custom)
    val line = MaterialTheme.colorScheme.onSurface
    val faint = MaterialTheme.colorScheme.outlineVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val k = kind.coerceIn(0, 3)
    var cur by remember(custom) { mutableStateOf(custom) }
    var sel by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    var probe by remember { androidx.compose.runtime.mutableFloatStateOf(50f) }
    var menu by remember { mutableStateOf(false) }
    var advanced by remember { mutableStateOf(false) }
    var analysisOpen by remember { mutableStateOf(false) }
    val undo = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    val redo = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    var tick by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(expanded, usageSet) {
        while (expanded && usageSet != null) { kotlinx.coroutines.delay(500); tick++ }
    }
    val latestKind = androidx.compose.runtime.rememberUpdatedState(k)
    val apply = androidx.compose.runtime.rememberUpdatedState(onApply)
    val active: (Float) -> Float = { applyCurve(k, cur, it) }
    fun snap() { undo.add(cur.encode()); if (undo.size > 50) undo.removeAt(0); redo.clear() }
    fun commit(c: Curve) { cur = c; apply.value(null, c) }
    fun toCustom() {
        if (latestKind.value != 3) {
            val seed = Curve.fromKind(latestKind.value)
            cur = seed
            apply.value(3, seed)
        }
    }

    Column(modifier = Modifier.widthIn(max = SETTINGS_WIDTH).fillMaxWidth().padding(start = 24.dp + indent, end = 24.dp, top = 6.dp, bottom = 6.dp)) {
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        ResetChip(overridden, onReset)
                    }
                    Text(helper, fontSize = 12.5.sp, color = muted)
                }
                if (!expanded) {
                    Text(stringResource(names[k]), fontSize = 13.sp, color = muted, modifier = Modifier.padding(horizontal = 10.dp))
                    MiniCurve(active, Modifier.size(48.dp, 36.dp), line)
                }
                Text(if (expanded) "⌃" else "›", fontSize = 20.sp, color = muted, modifier = Modifier.padding(start = 10.dp))
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = expanded,
                enter = androidx.compose.animation.expandVertically(tween(180)) + fadeIn(tween(180)),
                exit = androidx.compose.animation.shrinkVertically(tween(150)) + fadeOut(tween(150))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    var maxS = 0f
                    var minS = 0f
                    val sensSamples = FloatArray(129) { i -> if (k == 3) cur.sensitivity(i / 128f) else sensOf(active, i / 128f) }
                    for (v in sensSamples) { maxS = maxOf(maxS, v); minS = minOf(minS, v) }
                    val paint = remember { android.graphics.Paint().apply { isAntiAlias = true } }

                    // presets
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        for (i in 0..3) {
                            val selected = k == i
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.5.dp, if (selected) line else faint, RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (i == 3 && cur.points.isEmpty()) {
                                            val seed = Curve.fromKind(k)
                                            cur = seed
                                            onApply(3, seed)
                                        } else onApply(i, null)
                                    }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MiniCurve({ applyCurve(i, if (i == 3) cur else Curve.Linear, it) }, Modifier.size(30.dp, 22.dp), if (selected) line else muted)
                                Column(modifier = Modifier.padding(start = 6.dp)) {
                                    Text(stringResource(names[i]), fontSize = 12.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal, maxLines = 1)
                                    Text(stringResource(captions[i]), fontSize = 9.sp, color = muted, maxLines = 1)
                                }
                            }
                        }
                    }

                    // editing toolbar, Custom only
                    if (k == 3) Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        io.github.horse3903.airdeck.ui.composables.SegmentedChoice(
                            options = listOf(true, false), selected = cur.smooth,
                            onSelect = { v -> snap(); commit(cur.copy(smooth = v)) },
                            modifier = Modifier.width(180.dp)
                        ) { v, isSel ->
                            Text(stringResource(if (v) R.string.curve_smooth else R.string.curve_linear), fontSize = 13.sp, fontWeight = if (isSel) FontWeight.Medium else FontWeight.Normal, maxLines = 1, softWrap = false)
                        }
                        Text("${cur.points.size} / ${io.github.horse3903.airdeck.data.MAX_CURVE_POINTS}", fontSize = 12.sp, color = muted, modifier = Modifier.weight(1f))
                        TextButton(onClick = { if (undo.isNotEmpty()) { redo.add(cur.encode()); commit(Curve.decode(undo.removeAt(undo.size - 1))) } }, enabled = undo.isNotEmpty(), contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(32.dp)) { Text("↶", fontSize = 16.sp) }
                        TextButton(onClick = { if (redo.isNotEmpty()) { undo.add(cur.encode()); commit(Curve.decode(redo.removeAt(redo.size - 1))) } }, enabled = redo.isNotEmpty(), contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(32.dp)) { Text("↷", fontSize = 16.sp) }
                        OutlinedButton(onClick = { snap(); sel = -1; commit(Curve(cur.smooth)) }, contentPadding = PaddingValues(horizontal = 10.dp), modifier = Modifier.height(32.dp)) { Text(stringResource(R.string.curve_clear), fontSize = 12.sp) }
                        Box {
                            OutlinedButton(onClick = { menu = true }, contentPadding = PaddingValues(horizontal = 10.dp), modifier = Modifier.height(32.dp)) { Text("⋯", fontSize = 14.sp) }
                            androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(stringResource(R.string.curve_copy)) },
                                    onClick = { menu = false; clipboard.setText(androidx.compose.ui.text.AnnotatedString("AIRCURVE1:" + cur.encode())) }
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(stringResource(R.string.curve_paste)) },
                                    onClick = {
                                        menu = false
                                        val t = clipboard.getText()?.text?.trim().orEmpty()
                                        if (t.startsWith("AIRCURVE1:")) { snap(); sel = -1; commit(Curve.decode(t.removePrefix("AIRCURVE1:"))) }
                                    }
                                )
                            }
                        }
                    }

                    val mainBlock: @Composable () -> Unit = {
                        Column(modifier = Modifier.width(310.dp)) {
                            Text("$outLabel (%)", fontSize = 11.sp, color = muted, modifier = Modifier.padding(bottom = 4.dp))
                            Row {
                                Column(modifier = Modifier.height(280.dp).width(30.dp).padding(end = 6.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                                    for (v in listOf(100, 75, 50, 25, 0)) Text("$v", fontSize = 10.sp, color = muted)
                                }
                                androidx.compose.foundation.Canvas(
                                    modifier = Modifier
                                        .size(280.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                        .pointerInput(Unit) {
                                            val hit = 26.dp.toPx()
                                            fun pts() = if (latestKind.value == 3) cur.points else Curve.fromKind(latestKind.value).points
                                            fun nearest(o: androidx.compose.ui.geometry.Offset): Int {
                                                var best = -1
                                                var bestD = hit
                                                pts().forEachIndexed { i, p ->
                                                    val d = (androidx.compose.ui.geometry.Offset(p.first / 100f * size.width, size.height - p.second / 100f * size.height) - o).getDistance()
                                                    if (d < bestD) { bestD = d; best = i }
                                                }
                                                return best
                                            }
                                            detectTapGestures(
                                                onDoubleTap = { o ->
                                                    val i = nearest(o)
                                                    if (i >= 0) {
                                                        snap(); toCustom()
                                                        sel = -1
                                                        commit(Curve.clean(cur.smooth, cur.points.filterIndexed { n, _ -> n != i }, cur.free))
                                                    }
                                                },
                                                onTap = { o ->
                                                    val i = nearest(o)
                                                    probe = (o.x / size.width * 100f).coerceIn(0f, 100f)
                                                    if (i >= 0) { snap(); toCustom(); sel = i; probe = cur.points.getOrNull(i)?.first?.toFloat() ?: probe }
                                                    else if (pts().size < io.github.horse3903.airdeck.data.MAX_CURVE_POINTS) {
                                                        snap(); toCustom()
                                                        val x = (o.x / size.width * 100f).toInt()
                                                        val y = ((1f - o.y / size.height) * 100f).toInt()
                                                        val c = Curve.clean(cur.smooth, cur.points + (x to y), cur.free)
                                                        sel = c.points.indexOfFirst { it.first == x.coerceIn(1, 99) }
                                                        commit(c)
                                                    }
                                                }
                                            )
                                        }
                                        .pointerInput(Unit) {
                                            val hit = 26.dp.toPx()
                                            var which = -1
                                            detectDragGestures(
                                                onDragStart = { o ->
                                                    which = -1
                                                    var bestD = hit
                                                    val list = if (latestKind.value == 3) cur.points else Curve.fromKind(latestKind.value).points
                                                    list.forEachIndexed { i, p ->
                                                        val d = (androidx.compose.ui.geometry.Offset(p.first / 100f * size.width, size.height - p.second / 100f * size.height) - o).getDistance()
                                                        if (d < bestD) { bestD = d; which = i }
                                                    }
                                                    if (which >= 0) { snap(); toCustom(); sel = which }
                                                },
                                                onDragEnd = { if (which >= 0) apply.value(null, cur) },
                                                onDragCancel = { if (which >= 0) apply.value(null, cur) }
                                            ) { change, _ ->
                                                change.consume()
                                                if (which >= 0) {
                                                    val x = (change.position.x / size.width * 100f).toInt()
                                                    val y = ((1f - change.position.y / size.height) * 100f).toInt()
                                                    cur = placed(cur, which, x, y)
                                                    probe = cur.points[which].first.toFloat()
                                                }
                                            }
                                        }
                                ) {
                                    val w = size.width
                                    val h = size.height
                                    for (i in 1..3) {
                                        drawLine(faint.copy(alpha = 0.4f), androidx.compose.ui.geometry.Offset(w * i / 4f, 0f), androidx.compose.ui.geometry.Offset(w * i / 4f, h), strokeWidth = 1f)
                                        drawLine(faint.copy(alpha = 0.4f), androidx.compose.ui.geometry.Offset(0f, h * i / 4f), androidx.compose.ui.geometry.Offset(w, h * i / 4f), strokeWidth = 1f)
                                    }
                                    drawLine(
                                        faint, androidx.compose.ui.geometry.Offset(0f, h), androidx.compose.ui.geometry.Offset(w, 0f), strokeWidth = 2f,
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                                    )
                                    val path = androidx.compose.ui.graphics.Path()
                                    for (i in 0..100) {
                                        val x = i / 100f
                                        val y = active(x)
                                        if (i == 0) path.moveTo(0f, h - y * h) else path.lineTo(x * w, h - y * h)
                                    }
                                    drawPath(path, line, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f))
                                    drawLine(muted.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(probe / 100f * w, 0f), androidx.compose.ui.geometry.Offset(probe / 100f * w, h), strokeWidth = 1.5f)
                                    if (k == 3) cur.points.forEachIndexed { i, pt ->
                                        val c = androidx.compose.ui.geometry.Offset(pt.first / 100f * w, h - pt.second / 100f * h)
                                        if (i == sel) {
                                            drawCircle(line, 22f, c, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
                                            drawCircle(line, 15f, c)
                                        } else drawCircle(line, 11f, c)
                                    }
                                }
                            }
                            Row(modifier = Modifier.padding(start = 30.dp).width(280.dp).padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                for (v in listOf(0, 25, 50, 75, 100)) Text("$v", fontSize = 10.sp, color = muted)
                            }
                            Text("$inLabel (%)", fontSize = 11.sp, color = muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(start = 30.dp).width(280.dp))
                            if (k == 3) Text(stringResource(R.string.curve_hint), fontSize = 11.sp, color = muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(start = 30.dp, top = 2.dp).width(280.dp))
                        }
                    }

                    val analysisBlock: @Composable () -> Unit = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            val f = active
                            val s = if (k == 3) cur.sensitivity(probe / 100f) else sensOf(f, probe / 100f)
                            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(if (sel >= 0 && k == 3) stringResource(R.string.curve_selected).uppercase() else "AT ${probe.toInt()}% INPUT", fontSize = 9.sp, color = muted)
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) { Text(inLabel, fontSize = 10.sp, color = muted, maxLines = 1); Text("${probe.toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                                    Column(modifier = Modifier.weight(1f)) { Text(outLabel, fontSize = 10.sp, color = muted, maxLines = 1); Text("${(f(probe / 100f) * 1000).toInt() / 10f}%", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                                    Column(modifier = Modifier.weight(1f)) { Text(stringResource(R.string.curve_sens_label), fontSize = 10.sp, color = muted, maxLines = 1); Text("${(s * 100).toInt() / 100f}x", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (s < 0f) NegColor else Color.Unspecified) }
                                }
                            }
                            TextButton(onClick = { analysisOpen = !analysisOpen }, contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.height(30.dp)) {
                                Text(stringResource(R.string.curve_analysis) + if (analysisOpen) " \u2303" else " \u2304", fontSize = 12.sp, color = muted)
                            }
                            if (analysisOpen) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column {
                                Text(stringResource(R.string.curve_sensitivity), fontSize = 11.sp, color = muted, modifier = Modifier.padding(bottom = 2.dp))
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(86.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))) {
                                    val w = size.width
                                    val h = size.height
                                    val top = kotlin.math.ceil(maxS * 1.1f).coerceIn(2f, 8f)
                                    val bot = if (minS < 0f) kotlin.math.floor(minS * 1.1f).coerceAtLeast(-4f) else 0f
                                    fun yOf(v: Float) = h - (v.coerceIn(bot, top) - bot) / (top - bot) * h
                                    for (i in 1..3) drawLine(faint.copy(alpha = 0.4f), androidx.compose.ui.geometry.Offset(w * i / 4f, 0f), androidx.compose.ui.geometry.Offset(w * i / 4f, h), strokeWidth = 1f)
                                    drawLine(faint, androidx.compose.ui.geometry.Offset(0f, yOf(0f)), androidx.compose.ui.geometry.Offset(w, yOf(0f)), strokeWidth = 1.5f)
                                    drawLine(
                                        muted, androidx.compose.ui.geometry.Offset(0f, yOf(1f)), androidx.compose.ui.geometry.Offset(w, yOf(1f)), strokeWidth = 1.5f,
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 9f))
                                    )
                                    for (i in 1..128) {
                                        val neg = sensSamples[i] < 0f || sensSamples[i - 1] < 0f
                                        drawLine(
                                            if (neg) NegColor else SensColor,
                                            androidx.compose.ui.geometry.Offset((i - 1) / 128f * w, yOf(sensSamples[i - 1])), androidx.compose.ui.geometry.Offset(i / 128f * w, yOf(sensSamples[i])), strokeWidth = 4f
                                        )
                                    }
                                    drawLine(muted.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(probe / 100f * w, 0f), androidx.compose.ui.geometry.Offset(probe / 100f * w, h), strokeWidth = 1.5f)
                                    paint.color = muted.toArgb()
                                    paint.textSize = 10.sp.toPx()
                                    drawContext.canvas.nativeCanvas.drawText("${top.toInt()}x", 6f, paint.textSize + 2f, paint)
                                    drawContext.canvas.nativeCanvas.drawText("1x", 6f, yOf(1f) - 4f, paint)
                                }
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    for (v in listOf(0, 25, 50, 75, 100)) Text("$v", fontSize = 10.sp, color = muted)
                                }
                            }
                            if (usageSet != null && usageLabel != null) {
                                @Suppress("UNUSED_EXPRESSION") tick
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(usageLabel, fontSize = 11.sp, color = muted)
                                        OutlinedButton(onClick = { usageSet.clear(); tick++ }, contentPadding = PaddingValues(horizontal = 10.dp), modifier = Modifier.height(28.dp)) {
                                            Text(stringResource(R.string.curve_usage_reset), fontSize = 11.sp)
                                        }
                                    }
                                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(72.dp).padding(top = 4.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))) {
                                        val w = size.width
                                        val h = size.height
                                        for (i in 1..3) drawLine(faint.copy(alpha = 0.4f), androidx.compose.ui.geometry.Offset(w * i / 4f, 0f), androidx.compose.ui.geometry.Offset(w * i / 4f, h), strokeWidth = 1f)
                                        val bins = usageSet.bins
                                        var mx = 1
                                        for (b in bins) mx = maxOf(mx, b)
                                        if (usageSet.samples == 0) {
                                            paint.color = muted.toArgb()
                                            paint.textSize = 11.sp.toPx()
                                            drawContext.canvas.nativeCanvas.drawText(ctx.getString(R.string.curve_usage_empty), 10f, h / 2 + 4f, paint)
                                        } else {
                                            val bw = w / bins.size
                                            for (i in bins.indices) {
                                                val bh = bins[i].toFloat() / mx * h
                                                drawRect(UsageColor, androidx.compose.ui.geometry.Offset(i * bw + 1f, h - bh), androidx.compose.ui.geometry.Size(bw - 2f, bh))
                                            }
                                        }
                                        drawLine(muted.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(probe / 100f * w, 0f), androidx.compose.ui.geometry.Offset(probe / 100f * w, h), strokeWidth = 1.5f)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        for (v in listOf(0, 25, 50, 75, 100)) Text("$v", fontSize = 10.sp, color = muted)
                                    }
                                    val total = usageSet.bins.sum()
                                    if (total > 0) {
                                        val b = usageSet.bins
                                        val fine = b.take(12).sum() * 100 / total
                                        val full = b.drop(38).sum() * 100 / total
                                        Text("Fine $fine%   Mid ${100 - fine - full}%   Full $full%", fontSize = 11.sp, color = muted, modifier = Modifier.padding(top = 2.dp))
                                    }
                                }
                            }
                            }
                            if (minS < -0.01f) Text(stringResource(R.string.curve_warn_neg), fontSize = 12.sp, color = NegColor)
                            if (maxS > 6f) Text(stringResource(R.string.curve_warn_steep), fontSize = 12.sp, color = SensColor)
                        }
                    }

                    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        if (maxWidth >= 640.dp) {
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                mainBlock()
                                Box(modifier = Modifier.weight(1f)) { analysisBlock() }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { mainBlock(); analysisBlock() }
                        }
                    }

                    if (k == 3) {
                        val sp = cur.points.getOrNull(sel)
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.curve_selected).uppercase(), fontSize = 11.sp, color = muted, modifier = Modifier.padding(bottom = 8.dp))
                            StepField(stringResource(R.string.curve_input), sp?.first, sp != null) { d -> snap(); commit(placed(cur, sel, cur.points[sel].first + d, cur.points[sel].second)); probe = cur.points[sel].first.toFloat() }
                            StepField(stringResource(R.string.curve_output), sp?.second, sp != null) { d -> snap(); commit(placed(cur, sel, cur.points[sel].first, cur.points[sel].second + d)) }
                            TextButton(
                                onClick = { snap(); val i = sel; sel = -1; commit(Curve.clean(cur.smooth, cur.points.filterIndexed { n, _ -> n != i }, cur.free)) },
                                enabled = sp != null, contentPadding = PaddingValues(horizontal = 8.dp)
                            ) { Text(stringResource(R.string.curve_remove), fontSize = 12.sp) }
                        }
                        TextButton(onClick = { advanced = !advanced }, contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.height(30.dp)) {
                            Text(stringResource(R.string.curve_advanced) + if (advanced) " ⌃" else " ⌄", fontSize = 12.sp, color = muted)
                        }
                        if (advanced) Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = cur.free,
                                onCheckedChange = { v -> snap(); commit(Curve.clean(cur.smooth, cur.points, v)) },
                                modifier = Modifier.scale(0.8f)
                            )
                            Text(stringResource(R.string.curve_allow_decreasing), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGamepadCustomization: () -> Unit,
    settingsRepository: SettingsRepository,
    onNavigateToAbout: () -> Unit = {}
) {
    var settingsChanges by rememberSaveable { mutableStateOf(SettingsChanges()) }
    val applyScope = rememberCoroutineScope()
    var page by rememberSaveable { mutableStateOf("appearance") }
    var scopeLayout by rememberSaveable { mutableStateOf(false) }
    var playExpanded by rememberSaveable { mutableStateOf(true) }

    val snackbarHost = remember { androidx.compose.material3.SnackbarHostState() }
    var confirmReset by remember { mutableStateOf(false) }
    val resetMessage = stringResource(R.string.settings_reset_done)
    val undoLabel = stringResource(R.string.settings_reset_undo)
    Scaffold(snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHost) }) { paddingValues ->
        val colorScheme by settingsRepository.colorScheme.collectAsState(initial = defaultColorScheme)
        val gamepadStyle by settingsRepository.gamepadStyle.collectAsState(initial = defaultGamepadStyle)
        val pollingDelay by settingsRepository.pollingDelay.collectAsState(initial = defaultPollingDelay)
        val hapticEnabled by settingsRepository.hapticFeedbackEnabled.collectAsState(initial = defaultHapticFeedbackEnabled)
        val stickSnap by settingsRepository.stickSnap.collectAsState(initial = defaultStickSnap)
        val hapticPrefs by settingsRepository.hapticPrefs.collectAsState(initial = HapticPrefs())
        val prefs by settingsRepository.appPrefs.collectAsState(initial = io.github.horse3903.airdeck.data.AppPrefs())
        val behaviour by settingsRepository.layoutBehaviour.collectAsState(initial = emptyMap())
        val activeLayout by settingsRepository.activeLayout.collectAsState(initial = null)
        val layoutConfigs by settingsRepository.buttonConfigs.collectAsState(initial = defaultButtonConfigs)
        val layoutLabel = activeLayout ?: stringResource(R.string.layout_custom)
        // With "this layout" chosen the rows show what the layout uses, and a change is saved as its override
        val shown = if (scopeLayout) prefs.withBehaviour(behaviour) else prefs
        val hapticView = androidx.compose.ui.platform.LocalView.current
        val dpadSingle by settingsRepository.dpadSingle.collectAsState(initial = defaultDpadSingle)
        val saveCredentials by settingsRepository.saveConnectionCredentials.collectAsState(initial = defaultSaveConnectionCredentials)
        val fullScreenEnabled by settingsRepository.fullScreenEnabled.collectAsState(initial = defaultFullScreenEnabled)

        if (confirmReset) {
            AlertDialog(
                onDismissRequest = { confirmReset = false },
                title = { Text(stringResource(R.string.settings_reset_title)) },
                text = { Text(stringResource(R.string.settings_reset_body)) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmReset = false
                        applyScope.launch {
                            val before = settingsRepository.snapshotSettings()
                            settingsChanges = SettingsChanges()
                            settingsRepository.resetAllSettings()
                            Log.i(logTag, "Settings reset to defaults")
                            val result = snackbarHost.showSnackbar(resetMessage, actionLabel = undoLabel, duration = androidx.compose.material3.SnackbarDuration.Long)
                            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) settingsRepository.restoreSettings(before)
                        }
                    }) { Text(stringResource(R.string.settings_reset_all)) }
                },
                dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } }
            )
        }
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Left pane: back arrow, title and the categories
            Column(
                modifier = Modifier
                    .width(232.dp)
                    .fillMaxHeight()
                    .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    )
                }
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    CategoryItem(stringResource(R.string.sec_appearance), selected = page == "appearance") { page = "appearance" }
                    CategoryItem(stringResource(R.string.layouts_title), selected = page == "layouts") { page = "layouts" }
                    CategoryItem(
                        stringResource(R.string.sec_controls), selected = false,
                        expanded = playExpanded, onToggle = { playExpanded = !playExpanded }
                    ) { playExpanded = !playExpanded }
                    AnimatedVisibility(visible = playExpanded) {
                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            listOf(
                                "sticks" to R.string.sec_sticks,
                                "dpad" to R.string.sec_dpad,
                                "triggers" to R.string.sec_triggers,
                                "haptics" to R.string.sec_haptics
                            ).forEach { (id, label) -> CategoryItem(stringResource(label), selected = page == id) { page = id } }
                        }
                    }
                    CategoryItem(stringResource(R.string.sec_group_display), selected = page == "display") { page = "display" }
                    CategoryItem(stringResource(R.string.sec_connection), selected = page == "connection") { page = "connection" }
                }
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            )
            // Right pane: the chosen category
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(
                        when (page) {
                            "layouts" -> R.string.layouts_title
                            "sticks" -> R.string.sec_sticks
                            "dpad" -> R.string.sec_dpad
                            "triggers" -> R.string.sec_triggers
                            "haptics" -> R.string.sec_haptics
                            "display" -> R.string.sec_group_display
                            "connection" -> R.string.sec_connection
                            else -> R.string.sec_appearance
                        }
                    ),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .widthIn(max = SETTINGS_WIDTH)
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 6.dp)
                )
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                fun save(p: io.github.horse3903.airdeck.data.AppPrefs) { applyScope.launch { settingsRepository.setAppPrefs(p) } }
                fun saveHaptics(p: HapticPrefs) { applyScope.launch { settingsRepository.setHapticPrefs(p) } }

                fun ov(key: String) = scopeLayout && key in behaviour
                fun clr(key: String) { applyScope.launch { settingsRepository.setLayoutBehaviour(behaviour - key) } }
                fun chg(key: String, value: Int, global: () -> Unit) {
                    if (scopeLayout) applyScope.launch { settingsRepository.setLayoutBehaviour(behaviour + (key to value)) } else global()
                }
                fun curveChange(kindKey: String, prefix: String, kind: Int?, c: Curve?, apply: (AppPrefs, Int?, Curve?) -> AppPrefs) {
                    if (scopeLayout) {
                        var m = behaviour
                        if (kind != null) m = m + (kindKey to kind)
                        if (c != null) m = m - Curve.keys(prefix).toSet() + c.toInts(prefix)
                        applyScope.launch { settingsRepository.setLayoutBehaviour(m) }
                    } else save(apply(prefs, kind, c))
                }
                val hapShown = if (scopeLayout) hapticPrefs.withBehaviour(behaviour) else hapticPrefs
                val hapOn = if (scopeLayout) (behaviour["hap"]?.let { it != 0 } ?: hapticEnabled) else hapticEnabled

                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(120)) using SizeTransform(clip = false)
                    },
                    label = "page"
                ) { current ->
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                when (current) {
                    "layouts" -> LayoutsPage(settingsRepository, onNavigateToGamepadCustomization)
                    "sticks" -> {
                fun overridden(key: String) = scopeLayout && key in behaviour
                fun clearBehaviour(key: String) { applyScope.launch { settingsRepository.setLayoutBehaviour(behaviour - key) } }
                fun change(key: String, value: Int, global: () -> Unit) {
                    if (scopeLayout) applyScope.launch { settingsRepository.setLayoutBehaviour(behaviour + (key to value)) } else global()
                }
                val snapShown = if (scopeLayout) (behaviour["snap"]?.let { it != 0 } ?: stickSnap) else stickSnap
                val curveNames = listOf(R.string.curve_linear, R.string.curve_precise, R.string.curve_quick, R.string.curve_custom)
                ScopeBar(layoutLabel, scopeLayout) { scopeLayout = it }
                // Tilt steering comes first: it takes a stick over, so the rows below leave that stick out
                ChoiceRow(
                    stringResource(R.string.pref_tilt), stringResource(R.string.pref_tilt_desc), listOf(0, 1, 2), shown.tiltTarget,
                    { stringResource(when (it) { 0 -> R.string.pref_off; 1 -> R.string.tilt_left; else -> R.string.tilt_right }) },
                    overridden("tilt"), { clearBehaviour("tilt") }
                ) { v -> change("tilt", v) { save(prefs.copy(tiltTarget = v)) } }
                AnimatedVisibility(visible = shown.tiltTarget != 0) {
 Column {
                    SliderRow(
                        stringResource(R.string.pref_tilt_range), stringResource(R.string.pref_tilt_range_desc), "${shown.tiltRange}\u00B0",
                        shown.tiltRange, 15..60, { change("tiltRange", it) { save(prefs.copy(tiltRange = it)) } },
                        indent = 16.dp, overridden = overridden("tiltRange"), onReset = { clearBehaviour("tiltRange") }
                    )
                    SliderRow(
                        stringResource(R.string.pref_tilt_dead), stringResource(R.string.pref_tilt_dead_desc), "${shown.tiltDead}\u00B0",
                        shown.tiltDead, 0..15, { change("tiltDead", it) { save(prefs.copy(tiltDead = it)) } },
                        indent = 16.dp, overridden = overridden("tiltDead"), onReset = { clearBehaviour("tiltDead") }
                    )
                    SettingRow(
                        label = stringResource(R.string.pref_tilt_invert),
                        labelExtra = { ResetChip(overridden("tiltInvert")) { clearBehaviour("tiltInvert") } },
                        indent = 16.dp
                    ) {
                        Switch(
                            checked = shown.tiltInvert,
                            onCheckedChange = { v -> change("tiltInvert", if (v) 1 else 0) { save(prefs.copy(tiltInvert = v)) } },
                            modifier = Modifier.scale(0.85f)
                        )
                    }
                    CurveSection(
                        stringResource(R.string.pref_tilt_curve), stringResource(R.string.pref_tilt_curve_desc),
                        shown.tiltCurve, shown.tiltCurveCustom,
                        scopeLayout && ("tcurve" in behaviour || "tM" in behaviour),
                        { applyScope.launch { settingsRepository.setLayoutBehaviour(behaviour - "tcurve" - Curve.keys("t").toSet()) } },
                        { kd, c -> curveChange("tcurve", "t", kd, c) { p, k2, c2 -> p.copy(tiltCurve = k2 ?: p.tiltCurve, tiltCurveCustom = c2 ?: p.tiltCurveCustom) } },
                        inLabel = "Tilt input", outLabel = "Steering output",
                        captions = listOf(R.string.curve_cap_even, R.string.curve_cap_tilt_precise, R.string.curve_cap_tilt_quick, R.string.curve_cap_custom),
                        indent = 16.dp
                    )
                } }
                if (shown.tiltTarget != 1) ChoiceRow(
                    stringResource(R.string.pref_left_stick_position), stringResource(R.string.stick_position_desc),
                    io.github.horse3903.airdeck.data.StickMode.entries.toList(),
                    if (shown.stickLeftFloat) io.github.horse3903.airdeck.data.StickMode.FLOATING else io.github.horse3903.airdeck.data.StickMode.FIXED,
                    { stringResource(it.nameRes) }, ov("stickL"), { clr("stickL") }
                ) { m ->
                    val f = m == io.github.horse3903.airdeck.data.StickMode.FLOATING
                    chg("stickL", if (f) 1 else 0) { save(prefs.copy(stickLeftFloat = f)) }
                }
                if (shown.tiltTarget != 2) ChoiceRow(
                    stringResource(R.string.pref_right_stick_position), null,
                    io.github.horse3903.airdeck.data.StickMode.entries.toList(),
                    if (shown.stickRightFloat) io.github.horse3903.airdeck.data.StickMode.FLOATING else io.github.horse3903.airdeck.data.StickMode.FIXED,
                    { stringResource(it.nameRes) }, ov("stickR"), { clr("stickR") }
                ) { m ->
                    val f = m == io.github.horse3903.airdeck.data.StickMode.FLOATING
                    chg("stickR", if (f) 1 else 0) { save(prefs.copy(stickRightFloat = f)) }
                }
                SwitchRow(stringResource(R.string.settings_stick_snap), stringResource(R.string.settings_stick_snap_desc), snapShown, overridden("snap"), { clearBehaviour("snap") }) { v ->
                    change("snap", if (v) 1 else 0) { applyScope.launch { settingsRepository.setStickSnap(v) } }
                }
                SliderRow(
                    stringResource(R.string.pref_dead_zone), stringResource(R.string.pref_dead_zone_desc), "${shown.deadZone}%", shown.deadZone, 0..40,
                    { change("dead", it) { save(prefs.copy(deadZone = it)) } }, overridden = overridden("dead"), onReset = { clearBehaviour("dead") }
                )
                SliderRow(
                    stringResource(R.string.pref_sensitivity), stringResource(R.string.pref_sensitivity_desc), "${shown.sensitivity}%", shown.sensitivity, 50..150,
                    { change("sens", it) { save(prefs.copy(sensitivity = it)) } }, overridden = overridden("sens"), onReset = { clearBehaviour("sens") }
                )
                CurveSection(
                    stringResource(R.string.pref_curve), stringResource(R.string.pref_curve_desc),
                    shown.stickCurve, shown.curveCustom,
                    scopeLayout && ("curve" in behaviour || "cM" in behaviour),
                    { applyScope.launch { settingsRepository.setLayoutBehaviour(behaviour - "curve" - Curve.keys("c").toSet()) } },
                    { kd, c -> curveChange("curve", "c", kd, c) { p, k2, c2 -> p.copy(stickCurve = k2 ?: p.stickCurve, curveCustom = c2 ?: p.curveCustom) } },
                    inLabel = "Stick input", outLabel = "Stick output",
                    captions = listOf(R.string.curve_cap_linear, R.string.curve_cap_stick_precise, R.string.curve_cap_stick_quick, R.string.curve_cap_custom),
                    usageLabel = "Stick usage", usageSet = io.github.horse3903.airdeck.data.StickUsage.stick
                )
                SwitchRow(stringResource(R.string.pref_click), stringResource(R.string.pref_click_desc), shown.clickEnabled, overridden("click"), { clearBehaviour("click") }) { v ->
                    change("click", if (v) 1 else 0) { save(prefs.copy(clickEnabled = v)) }
                }
                AnimatedVisibility(visible = shown.clickEnabled) {
 Column {
                    SliderRow(
                        stringResource(R.string.pref_click_ms), null, "${shown.clickHoldMs} ms", shown.clickHoldMs, 200..1500,
                        { change("clickMs", it) { save(prefs.copy(clickHoldMs = it)) } }, overridden = overridden("clickMs"), onReset = { clearBehaviour("clickMs") }
                    )
                } }
                if (shown.tiltTarget != 1) SwitchRow(stringResource(R.string.pref_invert_left), null, shown.invertLeftY, overridden("invL"), { clearBehaviour("invL") }) { v ->
                    change("invL", if (v) 1 else 0) { save(prefs.copy(invertLeftY = v)) }
                }
                if (shown.tiltTarget != 2) SwitchRow(stringResource(R.string.pref_invert_right), null, shown.invertRightY, overridden("invR"), { clearBehaviour("invR") }) { v ->
                    change("invR", if (v) 1 else 0) { save(prefs.copy(invertRightY = v)) }
                }

                    }
                    "dpad" -> {
                fun overriddenD(key: String) = scopeLayout && key in behaviour
                fun clearD(key: String) { applyScope.launch { settingsRepository.setLayoutBehaviour(behaviour - key) } }
                fun changeD(key: String, value: Int, global: () -> Unit) {
                    if (scopeLayout) applyScope.launch { settingsRepository.setLayoutBehaviour(behaviour + (key to value)) } else global()
                }
                val singleShown = if (scopeLayout) (behaviour["dpadSingle"]?.let { it != 0 } ?: dpadSingle) else dpadSingle
                ScopeBar(layoutLabel, scopeLayout) { scopeLayout = it }
                SwitchRow(stringResource(R.string.settings_dpad_single), stringResource(R.string.settings_dpad_single_desc), singleShown, overriddenD("dpadSingle"), { clearD("dpadSingle") }) { v ->
                    changeD("dpadSingle", if (v) 1 else 0) { applyScope.launch { settingsRepository.setDpadSingle(v) } }
                }
                AnimatedVisibility(visible = !singleShown) {
 Column {
                    SliderRow(
                        stringResource(R.string.pref_diagonal), stringResource(R.string.pref_diagonal_desc), "${shown.dpadDiagonal}%", shown.dpadDiagonal, 20..60,
                        { changeD("diag", it) { save(prefs.copy(dpadDiagonal = it)) } }, overridden = overriddenD("diag"), onReset = { clearD("diag") }
                    )
                } }

                    }
                    "triggers" -> {
                ScopeBar(layoutLabel, scopeLayout) { scopeLayout = it }
                val offered = io.github.horse3903.airdeck.data.TriggerMode.offered
                ChoiceRow(
                    stringResource(R.string.pref_left_trigger_input), stringResource(offered[shown.triggerLeft.coerceIn(0, 2)].descRes),
                    offered, offered[shown.triggerLeft.coerceIn(0, 2)], { stringResource(it.nameRes) }, ov("trigL"), { clr("trigL") }
                ) { m -> chg("trigL", offered.indexOf(m)) { save(prefs.copy(triggerLeft = offered.indexOf(m))) } }
                ChoiceRow(
                    stringResource(R.string.pref_right_trigger_input), stringResource(offered[shown.triggerRight.coerceIn(0, 2)].descRes),
                    offered, offered[shown.triggerRight.coerceIn(0, 2)], { stringResource(it.nameRes) }, ov("trigR"), { clr("trigR") }
                ) { m -> chg("trigR", offered.indexOf(m)) { save(prefs.copy(triggerRight = offered.indexOf(m))) } }
                CurveSection(
                    stringResource(R.string.pref_trigger_curve), stringResource(R.string.pref_trigger_curve_desc),
                    shown.triggerCurve, shown.triggerCurveCustom,
                    scopeLayout && ("rcurve" in behaviour || "rM" in behaviour),
                    { applyScope.launch { settingsRepository.setLayoutBehaviour(behaviour - "rcurve" - Curve.keys("r").toSet()) } },
                    { kd, c -> curveChange("rcurve", "r", kd, c) { p, k2, c2 -> p.copy(triggerCurve = k2 ?: p.triggerCurve, triggerCurveCustom = c2 ?: p.triggerCurveCustom) } },
                    inLabel = "Trigger input", outLabel = "Trigger output",
                    captions = listOf(R.string.curve_cap_even, R.string.curve_cap_trig_precise, R.string.curve_cap_trig_quick, R.string.curve_cap_custom),
                    usageLabel = "Trigger usage", usageSet = io.github.horse3903.airdeck.data.StickUsage.trigger
                )

                    }
                    "haptics" -> {
                ScopeBar(layoutLabel, scopeLayout) { scopeLayout = it }
                SwitchRow(stringResource(R.string.haptics_game), stringResource(R.string.haptics_game_desc), shown.gameRumble, ov("rumble"), { clr("rumble") }) { v ->
                    chg("rumble", if (v) 1 else 0) { save(prefs.copy(gameRumble = v)) }
                }
                SwitchRow(stringResource(R.string.settings_haptic_feedback), stringResource(R.string.settings_haptic_desc), hapOn, ov("hap"), { clr("hap") }) { v ->
                    chg("hap", if (v) 1 else 0) {
                        settingsChanges = settingsChanges.copy(hapticFeedbackEnabled = v)
                        applyScope.launch { settingsRepository.setHapticFeedbackEnabled(v) }
                    }
                }
                AnimatedVisibility(visible = hapOn) {
 Column {
                    SwitchRow(stringResource(R.string.haptics_custom), stringResource(R.string.haptics_custom_desc), hapShown.custom, ov("hapCustom"), { clr("hapCustom") }) { v ->
                        chg("hapCustom", if (v) 1 else 0) { saveHaptics(hapticPrefs.copy(custom = v)) }
                    }
                    AnimatedVisibility(visible = hapShown.custom) {
 Column {
                        SliderRow(stringResource(R.string.haptics_intensity), null, "${hapShown.intensity}%", hapShown.intensity, 5..100,
                            { chg("hapInt", it) { saveHaptics(hapticPrefs.copy(intensity = it)) } }, { HapticUtils.performButtonPressFeedback(hapticView) },
                            overridden = ov("hapInt"), onReset = { clr("hapInt") })
                        SliderRow(stringResource(R.string.haptics_duration), null, "${hapShown.durationMs} ms", hapShown.durationMs, 5..80,
                            { chg("hapMs", it) { saveHaptics(hapticPrefs.copy(durationMs = it)) } }, { HapticUtils.performButtonPressFeedback(hapticView) },
                            overridden = ov("hapMs"), onReset = { clr("hapMs") })
                    } }
                    SwitchRow(stringResource(R.string.haptics_press), null, hapShown.press, ov("hapPress"), { clr("hapPress") }) { v -> chg("hapPress", if (v) 1 else 0) { saveHaptics(hapticPrefs.copy(press = v)) } }
                    SwitchRow(stringResource(R.string.haptics_release), null, hapShown.release, ov("hapRel"), { clr("hapRel") }) { v -> chg("hapRel", if (v) 1 else 0) { saveHaptics(hapticPrefs.copy(release = v)) } }
                    SwitchRow(stringResource(R.string.haptics_sticks), null, hapShown.sticks, ov("hapStick"), { clr("hapStick") }) { v -> chg("hapStick", if (v) 1 else 0) { saveHaptics(hapticPrefs.copy(sticks = v)) } }
                    SwitchRow(stringResource(R.string.haptics_triggers), null, hapShown.triggers, ov("hapTrig"), { clr("hapTrig") }) { v -> chg("hapTrig", if (v) 1 else 0) { saveHaptics(hapticPrefs.copy(triggers = v)) } }
                    SwitchRow(stringResource(R.string.haptics_connection), stringResource(R.string.haptics_connection_desc), shown.connectionHaptics, ov("hapConn"), { clr("hapConn") }) { v ->
                        chg("hapConn", if (v) 1 else 0) { save(prefs.copy(connectionHaptics = v)) }
                    }
                    SettingRow(label = stringResource(R.string.haptics_test)) {
                        OutlinedButton(onClick = { HapticUtils.performButtonPressFeedback(hapticView) }) { Text(stringResource(R.string.haptics_test)) }
                    }
                } }

                    }
                    "connection" -> {
                SettingRow(
                    label = stringResource(R.string.settings_polling_interval),
                    helper = stringResource(R.string.settings_polling_interval_desc),
                ) {
                    SpinBox(
                        value = settingsChanges.pollingDelay ?: pollingDelay,
                        onValueChange = {
                            settingsChanges = settingsChanges.copy(pollingDelay = it)
                            applyScope.launch { settingsRepository.setPollingDelay(it) }
                        },
                        label = "",
                        minValue = 20,
                        maxValue = 200,
                        step = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SwitchRow(stringResource(R.string.settings_save_connection_credentials), stringResource(R.string.settings_remember_desc), settingsChanges.saveConnectionCredentials ?: saveCredentials) {
                    settingsChanges = settingsChanges.copy(saveConnectionCredentials = it)
                    applyScope.launch { settingsRepository.setSaveConnectionCredentials(it) }
                }
                SwitchRow(stringResource(R.string.pref_reconnect), stringResource(R.string.pref_reconnect_desc), prefs.autoReconnect) { save(prefs.copy(autoReconnect = it)) }
                AnimatedVisibility(visible = prefs.autoReconnect) {
 Column {
                    SliderRow(stringResource(R.string.pref_reconnect_s), null, "${prefs.reconnectSeconds} s", prefs.reconnectSeconds, 10..600, { save(prefs.copy(reconnectSeconds = it)) })
                } }
                SwitchRow(stringResource(R.string.pref_confirm), stringResource(R.string.pref_confirm_desc), prefs.confirmDisconnect) { save(prefs.copy(confirmDisconnect = it)) }
                SectionHeader(stringResource(R.string.settings_reset_section))
                SettingRow(label = stringResource(R.string.settings_reset_all), helper = stringResource(R.string.settings_reset_desc)) {
                    OutlinedButton(onClick = { confirmReset = true }) { Text(stringResource(R.string.settings_reset_all)) }
                }
                    }
                    "display" -> {
                SwitchRow(stringResource(R.string.settings_full_screen), stringResource(R.string.settings_full_screen_desc), settingsChanges.fullScreenEnabled ?: fullScreenEnabled) {
                    settingsChanges = settingsChanges.copy(fullScreenEnabled = it)
                    applyScope.launch { settingsRepository.setFullScreenEnabled(it) }
                }
                SwitchRow(stringResource(R.string.pref_keep_on), stringResource(R.string.pref_keep_on_desc), prefs.keepScreenOn) { save(prefs.copy(keepScreenOn = it)) }
                SliderRow(
                    stringResource(R.string.pref_dim), stringResource(R.string.pref_dim_desc),
                    if (prefs.dimAfterSeconds == 0) stringResource(R.string.pref_off) else "${prefs.dimAfterSeconds} s",
                    prefs.dimAfterSeconds, 0..300, { save(prefs.copy(dimAfterSeconds = (it / 15) * 15)) }
                )
                    }
                    else -> {
                SettingRow(label = stringResource(R.string.settings_color_scheme), controlWidth = 300.dp) {
                    ColorSchemeChoice(
                        modifier = Modifier.fillMaxWidth(),
                        selected = settingsChanges.colorScheme ?: colorScheme,
                        onSelect = {
                            settingsChanges = settingsChanges.copy(colorScheme = it)
                            applyScope.launch { settingsRepository.setColorScheme(it) }
                        },
                        labelFor = { stringResource(it.nameRes) }
                    )
                }
                SettingRow(
                    label = stringResource(R.string.settings_gamepad_style),
                    helper = stringResource(R.string.settings_gamepad_style_desc),
                    controlWidth = 300.dp
                ) {
                    GamepadStyleChoice(
                        modifier = Modifier.fillMaxWidth(),
                        selected = gamepadStyle,
                        onSelect = { applyScope.launch { settingsRepository.setGamepadStyle(it) } },
                        labelFor = { stringResource(it.nameRes) }
                    )
                }
                SliderRow(stringResource(R.string.pref_opacity), null, "${prefs.opacity}%", prefs.opacity, 20..100, { save(prefs.copy(opacity = it)) })
                SwitchRow(stringResource(R.string.pref_glow), stringResource(R.string.pref_glow_desc), prefs.glowEnabled) { save(prefs.copy(glowEnabled = it)) }
                SliderRow(
                    stringResource(R.string.pref_glow_strength), null, "${prefs.glowStrength}%", prefs.glowStrength, 10..200,
                    { save(prefs.copy(glowStrength = it)) }, enabled = prefs.glowEnabled, indent = 16.dp
                )
                SwitchRow(stringResource(R.string.pref_ring), stringResource(R.string.pref_ring_desc), prefs.stickRing) { save(prefs.copy(stickRing = it)) }
                SliderRow(stringResource(R.string.pref_size), stringResource(R.string.pref_size_desc), "${prefs.controlSize}%", prefs.controlSize, 70..130, { save(prefs.copy(controlSize = (it / 5) * 5)) })

                    }
                }
                }
                }

            }
            }
        }
    }
}

@Preview(
    widthDp = PreviewWidthDp,
    heightDp = PreviewHeightDp,
)
@Composable
fun SettingsScreenPreview() {
    PreviewBase {
        SettingsScreen(
            onNavigateBack = {},
            onNavigateToGamepadCustomization = {},
            settingsRepository = SettingsRepository(LocalContext.current)
        )
    }
}
