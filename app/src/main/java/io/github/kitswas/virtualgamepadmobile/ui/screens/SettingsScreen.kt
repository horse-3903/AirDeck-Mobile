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

                SectionHeader(stringResource(R.string.settings_section_appearance))
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

                SectionHeader(stringResource(R.string.settings_section_controller))
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

                SettingRow(
                    label = stringResource(R.string.settings_stick_snap),
                    helper = stringResource(R.string.settings_stick_snap_desc)
                ) {
                    Switch(
                        checked = stickSnap,
                        onCheckedChange = { applyScope.launch { settingsRepository.setStickSnap(it) } }
                    )
                }

                SettingRow(
                    label = stringResource(R.string.settings_dpad_single),
                    helper = stringResource(R.string.settings_dpad_single_desc)
                ) {
                    Switch(
                        checked = dpadSingle,
                        onCheckedChange = { applyScope.launch { settingsRepository.setDpadSingle(it) } }
                    )
                }

                SettingRow(
                    label = stringResource(R.string.settings_haptic_feedback),
                    helper = stringResource(R.string.settings_haptic_desc)
                ) {
                    Switch(
                        checked = settingsChanges.hapticFeedbackEnabled ?: hapticEnabled,
                        onCheckedChange = {
                            settingsChanges = settingsChanges.copy(hapticFeedbackEnabled = it)
                            applyScope.launch { settingsRepository.setHapticFeedbackEnabled(it) }
                        }
                    )
                }

                if (hapticEnabled) {
                    SettingRow(
                        label = stringResource(R.string.haptics_custom),
                        helper = stringResource(R.string.haptics_custom_desc)
                    ) {
                        Switch(
                            checked = hapticPrefs.custom,
                            onCheckedChange = { applyScope.launch { settingsRepository.setHapticPrefs(hapticPrefs.copy(custom = it)) } }
                        )
                    }
                    if (hapticPrefs.custom) {
                        SettingRow(label = stringResource(R.string.haptics_intensity), helper = "${hapticPrefs.intensity}%") {
                            androidx.compose.material3.Slider(
                                value = hapticPrefs.intensity.toFloat(),
                                onValueChange = { applyScope.launch { settingsRepository.setHapticPrefs(hapticPrefs.copy(intensity = it.toInt())) } },
                                onValueChangeFinished = { HapticUtils.performButtonPressFeedback(hapticView) },
                                valueRange = 5f..100f,
                                modifier = Modifier.width(CONTROL_WIDTH)
                            )
                        }
                        SettingRow(label = stringResource(R.string.haptics_duration), helper = "${hapticPrefs.durationMs} ms") {
                            androidx.compose.material3.Slider(
                                value = hapticPrefs.durationMs.toFloat(),
                                onValueChange = { applyScope.launch { settingsRepository.setHapticPrefs(hapticPrefs.copy(durationMs = it.toInt())) } },
                                onValueChangeFinished = { HapticUtils.performButtonPressFeedback(hapticView) },
                                valueRange = 5f..80f,
                                modifier = Modifier.width(CONTROL_WIDTH)
                            )
                        }
                    }
                    SettingRow(label = stringResource(R.string.haptics_press)) {
                        Switch(checked = hapticPrefs.press, onCheckedChange = { applyScope.launch { settingsRepository.setHapticPrefs(hapticPrefs.copy(press = it)) } })
                    }
                    SettingRow(label = stringResource(R.string.haptics_release)) {
                        Switch(checked = hapticPrefs.release, onCheckedChange = { applyScope.launch { settingsRepository.setHapticPrefs(hapticPrefs.copy(release = it)) } })
                    }
                    SettingRow(label = stringResource(R.string.haptics_sticks)) {
                        Switch(checked = hapticPrefs.sticks, onCheckedChange = { applyScope.launch { settingsRepository.setHapticPrefs(hapticPrefs.copy(sticks = it)) } })
                    }
                    SettingRow(label = stringResource(R.string.haptics_triggers)) {
                        Switch(checked = hapticPrefs.triggers, onCheckedChange = { applyScope.launch { settingsRepository.setHapticPrefs(hapticPrefs.copy(triggers = it)) } })
                    }
                    SettingRow(label = stringResource(R.string.haptics_test)) {
                        OutlinedButton(onClick = { HapticUtils.performButtonPressFeedback(hapticView) }) { Text(stringResource(R.string.haptics_test)) }
                    }
                }

                SettingRow(
                    label = stringResource(R.string.settings_full_screen),
                    helper = stringResource(R.string.settings_full_screen_desc)
                ) {
                    Switch(
                        checked = settingsChanges.fullScreenEnabled ?: fullScreenEnabled,
                        onCheckedChange = {
                            settingsChanges = settingsChanges.copy(fullScreenEnabled = it)
                            applyScope.launch { settingsRepository.setFullScreenEnabled(it) }
                        }
                    )
                }

                SectionHeader(stringResource(R.string.settings_section_connection))
                SettingRow(
                    label = stringResource(R.string.settings_save_connection_credentials),
                    helper = stringResource(R.string.settings_remember_desc),
                    showDivider = false
                ) {
                    Switch(
                        checked = settingsChanges.saveConnectionCredentials ?: saveCredentials,
                        onCheckedChange = {
                            settingsChanges = settingsChanges.copy(saveConnectionCredentials = it)
                            applyScope.launch { settingsRepository.setSaveConnectionCredentials(it) }
                        }
                    )
                }


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
