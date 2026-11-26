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
                if (layout) layoutLabel else stringResource(R.string.scope_all),
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

/** A small graph to shape a response curve: drag the three dots up or down. The faint line is no shaping at all. */
@Composable
private fun CurveEditor(points: Int, overridden: Boolean, onReset: () -> Unit, onChange: (Int) -> Unit) {
    val pts = io.github.horse3903.airdeck.data.unpackCurve(points)
    val line = MaterialTheme.colorScheme.primary
    val faint = MaterialTheme.colorScheme.outlineVariant
    val dot = MaterialTheme.colorScheme.onSurface
    SettingRow(label = stringResource(R.string.pref_curve_custom), labelExtra = { ResetChip(overridden, onReset) }, helper = stringResource(R.string.pref_curve_custom_desc), indent = 16.dp) {
        var current by remember(points) { mutableStateOf(pts) }
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .pointerInput(Unit) {
                    var which = -1
                    detectDragGestures(
                        onDragStart = { o ->
                            val idx = (o.x / size.width * 4f).let { Math.round(it) }.coerceIn(1, 3)
                            which = idx - 1
                        },
                        onDragEnd = { onChange(io.github.horse3903.airdeck.data.packCurve(current[0], current[1], current[2])) },
                        onDragCancel = { onChange(io.github.horse3903.airdeck.data.packCurve(current[0], current[1], current[2])) }
                    ) { change, _ ->
                        change.consume()
                        if (which >= 0) {
                            val v = ((1f - change.position.y / size.height) * 100f).toInt().coerceIn(0, 100)
                            current = current.copyOf().also { it[which] = v }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            drawLine(faint, androidx.compose.ui.geometry.Offset(0f, h), androidx.compose.ui.geometry.Offset(w, 0f), strokeWidth = 2f)
            val xs = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
            val ys = floatArrayOf(0f, current[0] / 100f, current[1] / 100f, current[2] / 100f, 1f)
            for (i in 0 until 4) {
                drawLine(line, androidx.compose.ui.geometry.Offset(xs[i] * w, h - ys[i] * h), androidx.compose.ui.geometry.Offset(xs[i + 1] * w, h - ys[i + 1] * h), strokeWidth = 5f)
            }
            for (i in 1..3) drawCircle(dot, 11f, androidx.compose.ui.geometry.Offset(xs[i] * w, h - ys[i] * h))
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
                    CategoryItem(
                        stringResource(R.string.layouts_title), selected = page == "layouts",
                        expanded = playExpanded, onToggle = { playExpanded = !playExpanded }
                    ) { page = "layouts" }
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
                    ChoiceRow(
                        stringResource(R.string.pref_tilt_curve), stringResource(R.string.pref_tilt_curve_desc), listOf(0, 1, 2, 3), shown.tiltCurve,
                        { stringResource(curveNames[it]) }, overridden("tcurve"), { clearBehaviour("tcurve") }, indent = 16.dp
                    ) { v -> change("tcurve", v) { save(prefs.copy(tiltCurve = v)) } }
                    AnimatedVisibility(visible = shown.tiltCurve == 3) {
                        CurveEditor(
                            shown.tiltCurvePoints, overridden("tcurveP"), { clearBehaviour("tcurveP") },
                            { v -> change("tcurveP", v) { save(prefs.copy(tiltCurvePoints = v)) } }
                        )
                    }
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
                ChoiceRow(
                    stringResource(R.string.pref_curve), stringResource(R.string.pref_curve_desc), listOf(0, 1, 2, 3), shown.stickCurve,
                    { stringResource(curveNames[it]) }, overridden("curve"), { clearBehaviour("curve") }
                ) { v -> change("curve", v) { save(prefs.copy(stickCurve = v)) } }
                AnimatedVisibility(visible = shown.stickCurve == 3) {
                    CurveEditor(
                        shown.curvePoints, overridden("curveP"), { clearBehaviour("curveP") },
                        { v -> change("curveP", v) { save(prefs.copy(curvePoints = v)) } }
                    )
                }
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
