package io.github.kitswas.virtualgamepadmobile.ui.screens

import io.github.kitswas.virtualgamepadmobile.data.defaultStickSnap
import io.github.kitswas.virtualgamepadmobile.data.defaultDpadSingle
import io.github.kitswas.virtualgamepadmobile.data.defaultGamepadStyle
import io.github.kitswas.virtualgamepadmobile.data.GamepadStyle
import android.os.Parcelable
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import io.github.kitswas.virtualgamepadmobile.ui.utils.HapticPrefs
import io.github.kitswas.virtualgamepadmobile.ui.utils.HapticUtils
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.font.FontWeight
import io.github.kitswas.virtualgamepadmobile.R
import io.github.kitswas.virtualgamepadmobile.data.ColorScheme
import io.github.kitswas.virtualgamepadmobile.data.PreviewBase
import io.github.kitswas.virtualgamepadmobile.data.PreviewHeightDp
import io.github.kitswas.virtualgamepadmobile.data.PreviewWidthDp
import io.github.kitswas.virtualgamepadmobile.data.SettingsRepository
import io.github.kitswas.virtualgamepadmobile.data.defaultColorScheme
import io.github.kitswas.virtualgamepadmobile.data.defaultFullScreenEnabled
import io.github.kitswas.virtualgamepadmobile.data.defaultHapticFeedbackEnabled
import io.github.kitswas.virtualgamepadmobile.data.defaultPollingDelay
import io.github.kitswas.virtualgamepadmobile.data.defaultSaveConnectionCredentials
import io.github.kitswas.virtualgamepadmobile.ui.composables.ColorSchemePicker
import io.github.kitswas.virtualgamepadmobile.ui.composables.ListItemPicker
import io.github.kitswas.virtualgamepadmobile.ui.composables.SpinBox
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

private val CONTROL_WIDTH = 176.dp

@Composable
private fun SettingRow(
    label: String,
    labelExtra: @Composable () -> Unit = {},
    helper: String? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
    control: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
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
                    Text(text = label, style = MaterialTheme.typography.bodyMedium)
                    labelExtra()
                }
                if (helper != null) {
                    Text(
                        text = helper,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            control()
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun SwitchRow(label: String, helper: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingRow(label = label, helper = helper) { Switch(checked = checked, onCheckedChange = onChange) }
}

@Composable
private fun SliderRow(
    label: String,
    helper: String? = null,
    valueText: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    onFinished: (() -> Unit)? = null,
) {
    SettingRow(label = label, helper = listOfNotNull(helper, valueText).joinToString("  ")) {
        androidx.compose.material3.Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            onValueChangeFinished = { onFinished?.invoke() },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            modifier = Modifier.width(CONTROL_WIDTH)
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 2.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGamepadCustomization: () -> Unit,
    settingsRepository: SettingsRepository
) {
    var settingsChanges by rememberSaveable { mutableStateOf(SettingsChanges()) }
    val applyScope = rememberCoroutineScope()

    Scaffold { paddingValues ->
        val colorScheme by settingsRepository.colorScheme.collectAsState(initial = defaultColorScheme)
        val gamepadStyle by settingsRepository.gamepadStyle.collectAsState(initial = defaultGamepadStyle)
        val pollingDelay by settingsRepository.pollingDelay.collectAsState(initial = defaultPollingDelay)
        val hapticEnabled by settingsRepository.hapticFeedbackEnabled.collectAsState(initial = defaultHapticFeedbackEnabled)
        val stickSnap by settingsRepository.stickSnap.collectAsState(initial = defaultStickSnap)
        val hapticPrefs by settingsRepository.hapticPrefs.collectAsState(initial = HapticPrefs())
        val prefs by settingsRepository.appPrefs.collectAsState(initial = io.github.kitswas.virtualgamepadmobile.data.AppPrefs())
        val hapticView = androidx.compose.ui.platform.LocalView.current
        val dpadSingle by settingsRepository.dpadSingle.collectAsState(initial = defaultDpadSingle)
        val saveCredentials by settingsRepository.saveConnectionCredentials.collectAsState(initial = defaultSaveConnectionCredentials)
        val fullScreenEnabled by settingsRepository.fullScreenEnabled.collectAsState(initial = defaultFullScreenEnabled)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fixed title at the top
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Scrollable settings content
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                fun save(p: io.github.kitswas.virtualgamepadmobile.data.AppPrefs) { applyScope.launch { settingsRepository.setAppPrefs(p) } }
                fun saveHaptics(p: HapticPrefs) { applyScope.launch { settingsRepository.setHapticPrefs(p) } }

                SectionHeader(stringResource(R.string.sec_appearance))
                SettingRow(label = stringResource(R.string.settings_color_scheme)) {
                    ColorSchemePicker(
                        selectedItem = settingsChanges.colorScheme ?: colorScheme,
                        showLabel = false,
                        buttonModifier = Modifier
                            .width(CONTROL_WIDTH)
                            .height(40.dp)
                    ) {
                        settingsChanges = settingsChanges.copy(colorScheme = it)
                        applyScope.launch { settingsRepository.setColorScheme(it) }
                    }
                }
                SettingRow(
                    label = stringResource(R.string.settings_gamepad_style),
                    helper = stringResource(R.string.settings_gamepad_style_desc)
                ) {
                    ListItemPicker(
                        list = GamepadStyle.entries.asIterable(),
                        selectedItem = gamepadStyle,
                        label = "",
                        showLabel = false,
                        buttonModifier = Modifier
                            .width(CONTROL_WIDTH)
                            .height(40.dp),
                        formattedDisplay = { item -> Text(text = stringResource(item.nameRes)) },
                        onItemSelected = {
                            applyScope.launch { settingsRepository.setGamepadStyle(it) }
                        }
                    )
                }
                SliderRow(stringResource(R.string.pref_opacity), null, "${prefs.opacity}%", prefs.opacity, 20..100, { save(prefs.copy(opacity = it)) })
                SwitchRow(stringResource(R.string.pref_glow), stringResource(R.string.pref_glow_desc), prefs.glowEnabled) { save(prefs.copy(glowEnabled = it)) }
                if (prefs.glowEnabled) {
                    SliderRow(stringResource(R.string.pref_glow_strength), null, "${prefs.glowStrength}%", prefs.glowStrength, 10..200, { save(prefs.copy(glowStrength = it)) })
                }

                SectionHeader(stringResource(R.string.sec_layout))
                SettingRow(
                    label = stringResource(R.string.settings_customize_layout),
                    helper = stringResource(R.string.settings_customize_layout_desc),
                    onClick = onNavigateToGamepadCustomization
                ) {
                    Text(
                        text = "\u203A",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SectionHeader(stringResource(R.string.sec_display))
                SwitchRow(stringResource(R.string.settings_full_screen), stringResource(R.string.settings_full_screen_desc), settingsChanges.fullScreenEnabled ?: fullScreenEnabled) {
                    settingsChanges = settingsChanges.copy(fullScreenEnabled = it)
                    applyScope.launch { settingsRepository.setFullScreenEnabled(it) }
                }
                SwitchRow(stringResource(R.string.pref_keep_on), stringResource(R.string.pref_keep_on_desc), prefs.keepScreenOn) { save(prefs.copy(keepScreenOn = it)) }

                SectionHeader(stringResource(R.string.sec_sticks))
                SwitchRow(stringResource(R.string.settings_stick_snap), stringResource(R.string.settings_stick_snap_desc), stickSnap) {
                    applyScope.launch { settingsRepository.setStickSnap(it) }
                }
                SliderRow(stringResource(R.string.pref_dead_zone), stringResource(R.string.pref_dead_zone_desc), "${prefs.deadZone}%", prefs.deadZone, 0..40, { save(prefs.copy(deadZone = it)) })
                SliderRow(stringResource(R.string.pref_sensitivity), stringResource(R.string.pref_sensitivity_desc), "${prefs.sensitivity}%", prefs.sensitivity, 50..150, { save(prefs.copy(sensitivity = it)) })
                SwitchRow(stringResource(R.string.pref_click), stringResource(R.string.pref_click_desc), prefs.clickEnabled) { save(prefs.copy(clickEnabled = it)) }
                if (prefs.clickEnabled) {
                    SliderRow(stringResource(R.string.pref_click_ms), null, "${prefs.clickHoldMs} ms", prefs.clickHoldMs, 200..1500, { save(prefs.copy(clickHoldMs = it)) })
                }
                SwitchRow(stringResource(R.string.pref_invert_left), null, prefs.invertLeftY) { save(prefs.copy(invertLeftY = it)) }
                SwitchRow(stringResource(R.string.pref_invert_right), null, prefs.invertRightY) { save(prefs.copy(invertRightY = it)) }

                SectionHeader(stringResource(R.string.sec_dpad))
                SwitchRow(stringResource(R.string.settings_dpad_single), stringResource(R.string.settings_dpad_single_desc), dpadSingle) {
                    applyScope.launch { settingsRepository.setDpadSingle(it) }
                }
                if (!dpadSingle) {
                    SliderRow(stringResource(R.string.pref_diagonal), stringResource(R.string.pref_diagonal_desc), "${prefs.dpadDiagonal}%", prefs.dpadDiagonal, 20..60, { save(prefs.copy(dpadDiagonal = it)) })
                }

                SectionHeader(stringResource(R.string.sec_haptics))
                SwitchRow(stringResource(R.string.settings_haptic_feedback), stringResource(R.string.settings_haptic_desc), settingsChanges.hapticFeedbackEnabled ?: hapticEnabled) {
                    settingsChanges = settingsChanges.copy(hapticFeedbackEnabled = it)
                    applyScope.launch { settingsRepository.setHapticFeedbackEnabled(it) }
                }
                if (hapticEnabled) {
                    SwitchRow(stringResource(R.string.haptics_custom), stringResource(R.string.haptics_custom_desc), hapticPrefs.custom) { saveHaptics(hapticPrefs.copy(custom = it)) }
                    if (hapticPrefs.custom) {
                        SliderRow(stringResource(R.string.haptics_intensity), null, "${hapticPrefs.intensity}%", hapticPrefs.intensity, 5..100,
                            { saveHaptics(hapticPrefs.copy(intensity = it)) }, { HapticUtils.performButtonPressFeedback(hapticView) })
                        SliderRow(stringResource(R.string.haptics_duration), null, "${hapticPrefs.durationMs} ms", hapticPrefs.durationMs, 5..80,
                            { saveHaptics(hapticPrefs.copy(durationMs = it)) }, { HapticUtils.performButtonPressFeedback(hapticView) })
                    }
                    SwitchRow(stringResource(R.string.haptics_press), null, hapticPrefs.press) { saveHaptics(hapticPrefs.copy(press = it)) }
                    SwitchRow(stringResource(R.string.haptics_release), null, hapticPrefs.release) { saveHaptics(hapticPrefs.copy(release = it)) }
                    SwitchRow(stringResource(R.string.haptics_sticks), null, hapticPrefs.sticks) { saveHaptics(hapticPrefs.copy(sticks = it)) }
                    SwitchRow(stringResource(R.string.haptics_triggers), null, hapticPrefs.triggers) { saveHaptics(hapticPrefs.copy(triggers = it)) }
                    SettingRow(label = stringResource(R.string.haptics_test)) {
                        OutlinedButton(onClick = { HapticUtils.performButtonPressFeedback(hapticView) }) { Text(stringResource(R.string.haptics_test)) }
                    }
                }

                SectionHeader(stringResource(R.string.sec_connection))
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
                        modifier = Modifier.width(CONTROL_WIDTH)
                    )
                }
                SwitchRow(stringResource(R.string.settings_save_connection_credentials), stringResource(R.string.settings_remember_desc), settingsChanges.saveConnectionCredentials ?: saveCredentials) {
                    settingsChanges = settingsChanges.copy(saveConnectionCredentials = it)
                    applyScope.launch { settingsRepository.setSaveConnectionCredentials(it) }
                }
                SwitchRow(stringResource(R.string.pref_reconnect), stringResource(R.string.pref_reconnect_desc), prefs.autoReconnect) { save(prefs.copy(autoReconnect = it)) }
                if (prefs.autoReconnect) {
                    SliderRow(stringResource(R.string.pref_reconnect_s), null, "${prefs.reconnectSeconds} s", prefs.reconnectSeconds, 10..600, { save(prefs.copy(reconnectSeconds = it)) })
                }
                SwitchRow(stringResource(R.string.pref_confirm), stringResource(R.string.pref_confirm_desc), prefs.confirmDisconnect) { save(prefs.copy(confirmDisconnect = it)) }

            }

            // Fixed buttons at the bottom
            Row(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(shape = MaterialTheme.shapes.small, modifier = Modifier.offset(x = (-12).dp), onClick = {
                    settingsChanges = SettingsChanges()
                    runBlocking { settingsRepository.resetAllSettings() }
                    Log.i(logTag, "Settings reset to defaults")
                }) {
                    Text(stringResource(R.string.settings_reset_all), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.weight(1f))
                Button(
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.width(120.dp).height(48.dp),
                    onClick = onNavigateBack
                ) {
                    Text(stringResource(R.string.done))
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
