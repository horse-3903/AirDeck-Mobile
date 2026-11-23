package io.github.kitswas.virtualgamepadmobile.ui.screens

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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
                labelExtra()
            }
            control()
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
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
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

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

                SettingRow(label = stringResource(R.string.settings_gamepad_style)) {
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
                    label = stringResource(R.string.settings_polling_interval),
                    labelExtra = {
                        val toolTipState = rememberTooltipState()
                        val scope = rememberCoroutineScope()
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Start
                            ),
                            tooltip = {
                                PlainTooltip(shadowElevation = 10.dp) {
                                    Text(
                                        stringResource(R.string.settings_polling_interval_desc),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            },
                            state = toolTipState
                        ) {
                            IconButton(
                                onClick = { scope.launch { toolTipState.show() } },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_info),
                                    contentDescription = stringResource(R.string.settings_polling_interval_info),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
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

                SettingRow(label = stringResource(R.string.settings_haptic_feedback)) {
                    Switch(
                        checked = settingsChanges.hapticFeedbackEnabled ?: hapticEnabled,
                        onCheckedChange = {
                            settingsChanges = settingsChanges.copy(hapticFeedbackEnabled = it)
                            applyScope.launch { settingsRepository.setHapticFeedbackEnabled(it) }
                        }
                    )
                }

                SettingRow(label = stringResource(R.string.settings_full_screen)) {
                    Switch(
                        checked = settingsChanges.fullScreenEnabled ?: fullScreenEnabled,
                        onCheckedChange = {
                            settingsChanges = settingsChanges.copy(fullScreenEnabled = it)
                            applyScope.launch { settingsRepository.setFullScreenEnabled(it) }
                        }
                    )
                }

                SettingRow(label = stringResource(R.string.settings_save_connection_credentials)) {
                    Switch(
                        checked = settingsChanges.saveConnectionCredentials ?: saveCredentials,
                        onCheckedChange = {
                            settingsChanges = settingsChanges.copy(saveConnectionCredentials = it)
                            applyScope.launch { settingsRepository.setSaveConnectionCredentials(it) }
                        }
                    )
                }

                SettingRow(
                    label = stringResource(R.string.settings_customize_layout),
                    onClick = onNavigateToGamepadCustomization,
                    showDivider = false
                ) {
                    Text(
                        text = "\u203A",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Text(stringResource(R.string.reset), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
