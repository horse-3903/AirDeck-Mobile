package io.github.kitswas.virtualgamepadmobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import io.github.kitswas.virtualgamepadmobile.ui.composables.ComponentSettingsPanel
import io.github.kitswas.virtualgamepadmobile.data.OFFSET_VALUE_RANGE
import io.github.kitswas.virtualgamepadmobile.data.ButtonAnchor
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.kitswas.virtualgamepadmobile.R
import io.github.kitswas.virtualgamepadmobile.data.ButtonComponent
import io.github.kitswas.virtualgamepadmobile.data.ButtonConfig
import io.github.kitswas.virtualgamepadmobile.data.PreviewBase
import io.github.kitswas.virtualgamepadmobile.data.SettingsRepository
import io.github.kitswas.virtualgamepadmobile.data.defaultButtonConfigs
import io.github.kitswas.virtualgamepadmobile.ui.composables.ButtonConfigEditor
import io.github.kitswas.virtualgamepadmobile.ui.composables.DrawGamepad
import io.github.kitswas.virtualgamepadmobile.ui.composables.ResponsiveGrid
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

private const val logTag = "GamepadCustomization"

/**
 * Sanitizes button configurations to ensure all components have a valid config.
 */
private fun sanitizeButtonConfigs(configs: Map<ButtonComponent, ButtonConfig>): Map<ButtonComponent, ButtonConfig> {
    return ButtonComponent.entries.associateWith { component ->
        configs[component] ?: ButtonConfig.default(component)
    }
}

/**
 * Full-screen gamepad preview overlay
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun GamepadPreview(
    buttonConfigs: Map<ButtonComponent, ButtonConfig>,
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val screenWidth = configuration.screenWidthDp

    val gamepadState = remember { GamepadReading() }

    // Draw the gamepad with current configuration
    DrawGamepad(
        widthDp = screenWidth,
        heightDp = screenHeight,
        gamepadState = gamepadState,
        buttonConfigs = buttonConfigs
    )
}

@Composable
fun GamepadCustomizationScreen(
    onNavigateBack: () -> Unit,
    settingsRepository: SettingsRepository
) {
    val buttonConfigs by settingsRepository.buttonConfigs.collectAsState(initial = defaultButtonConfigs)
    var modifiedConfigs by rememberSaveable {
        mutableStateOf<Map<ButtonComponent, ButtonConfig>?>(
            null
        )
    }
    var showPreview by rememberSaveable { mutableStateOf(false) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var showPresets by rememberSaveable { mutableStateOf(false) }
    var showResetConfirm by rememberSaveable { mutableStateOf(false) }
    var selectedComponent by rememberSaveable { mutableStateOf<ButtonComponent?>(null) }
    val presets by settingsRepository.presets.collectAsState(initial = emptyMap())
    val presetScope = rememberCoroutineScope()
    var importJsonText by rememberSaveable { mutableStateOf("") }

    // Get the current configs to preview (modified or saved)
    val currentConfigs = modifiedConfigs ?: buttonConfigs

    // Handle back button to close preview
    BackHandler(enabled = showPreview) {
        showPreview = false
    }

    if (showPreview) {
        // Full-screen preview overlay
        GamepadPreview(buttonConfigs = currentConfigs)
        return
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.reset_layout_title)) },
            text = { Text(stringResource(R.string.reset_layout_message)) },
            confirmButton = {
                Button(
                    shape = MaterialTheme.shapes.small,
                    onClick = {
                        modifiedConfigs = null
                        runBlocking {
                            settingsRepository.setAllButtonConfigs(defaultButtonConfigs)
                        }
                        selectedComponent = null
                        showResetConfirm = false
                        Log.i(logTag, "Button configs reset to defaults")
                    }
                ) { Text(stringResource(R.string.reset)) }
            },
            dismissButton = {
                TextButton(
                    shape = MaterialTheme.shapes.small,
                    onClick = { showResetConfirm = false }
                ) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showPresets) {
        PresetsDialog(
            presets = presets,
            hasUnsavedChanges = modifiedConfigs != null,
            onSave = { name ->
                val configs = currentConfigs
                presetScope.launch { settingsRepository.savePreset(name, configs) }
            },
            onLoad = { configs ->
                modifiedConfigs = configs
                showPresets = false
            },
            onDelete = { name -> presetScope.launch { settingsRepository.deletePreset(name) } },
            onDismiss = { showPresets = false }
        )
    }

    if (showExportDialog) {
        ExportConfigDialog(
            buttonConfigs = currentConfigs,
            onDismiss = { showExportDialog = false }
        )
    }

    if (showImportDialog) {
        ImportConfigDialog(
            importedJsonText = importJsonText,
            onJsonTextChange = { importJsonText = it },
            onImport = { configs ->
                modifiedConfigs = configs
                showImportDialog = false
                importJsonText = ""
                Log.i(logTag, "Button configs imported")
            },
            onDismiss = {
                showImportDialog = false
                importJsonText = ""
            }
        )
    }

    val editorState = remember { GamepadReading() }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top action bar
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compact = maxWidth < 640.dp
                var overflowOpen by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.height(44.dp),
                            onClick = onNavigateBack
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.height(44.dp),
                            onClick = { showResetConfirm = true }
                        ) {
                            Text(
                                stringResource(R.string.reset),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (compact) {
                            Box {
                                IconButton(onClick = { overflowOpen = true }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_more_vert),
                                        contentDescription = stringResource(R.string.customization_more)
                                    )
                                }
                                DropdownMenu(
                                    expanded = overflowOpen,
                                    onDismissRequest = { overflowOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.presets_button)) },
                                        onClick = { overflowOpen = false; showPresets = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.customization_import)) },
                                        onClick = { overflowOpen = false; showImportDialog = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.customization_export)) },
                                        onClick = { overflowOpen = false; showExportDialog = true }
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.height(44.dp),
                                onClick = { showPresets = true }
                            ) {
                                Text(stringResource(R.string.presets_button))
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { showImportDialog = true }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_file_upload),
                                            contentDescription = stringResource(R.string.customization_import)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(24.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
                                    IconButton(onClick = { showExportDialog = true }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_file_download),
                                            contentDescription = stringResource(R.string.customization_export)
                                        )
                                    }
                                }
                            }
                        }
                        OutlinedButton(
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.height(44.dp),
                            onClick = { showPreview = true }
                        ) {
                            Text(stringResource(R.string.customization_preview_short))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .height(44.dp)
                                .widthIn(min = 88.dp),
                            onClick = {
                                modifiedConfigs?.let { configs ->
                                    runBlocking {
                                        try {
                                            settingsRepository.setAllButtonConfigs(configs)
                                            Log.i(logTag, "Button configs saved")
                                        } catch (e: Exception) {
                                            Log.e(logTag, "Error saving button configs", e)
                                        }
                                    }
                                }
                                onNavigateBack()
                            }
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Interactive layout: tap or drag any component to edit it
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val canvasHeight = maxHeight.value.toInt()
                DrawGamepad(
                    widthDp = maxWidth.value.toInt(),
                    heightDp = canvasHeight,
                    gamepadState = editorState,
                    buttonConfigs = currentConfigs,
                    onComponentTap = { selectedComponent = it },
                    selectedComponent = selectedComponent,
                    onComponentDrag = { component, dx, dy ->
                        val config = currentConfigs[component] ?: ButtonConfig.default(component)
                        val updated = config.copy(
                            offsetX = (config.offsetX + dx.value / canvasHeight)
                                .coerceIn(OFFSET_VALUE_RANGE),
                            offsetY = (config.offsetY + dy.value / canvasHeight)
                                .coerceIn(OFFSET_VALUE_RANGE),
                        )
                        modifiedConfigs = currentConfigs + (component to updated)
                    }
                )
                if (selectedComponent == null) {
                    Text(
                        text = stringResource(R.string.customization_tap_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                selectedComponent?.let { component ->
                    val config = currentConfigs[component] ?: ButtonConfig.default(component)
                    val panelOnRight = when (config.anchor) {
                        ButtonAnchor.TOP_LEFT, ButtonAnchor.CENTER_LEFT, ButtonAnchor.BOTTOM_LEFT -> true
                        ButtonAnchor.TOP_RIGHT, ButtonAnchor.CENTER_RIGHT, ButtonAnchor.BOTTOM_RIGHT -> false
                        else -> component != ButtonComponent.RIGHT_SHOULDER &&
                                component != ButtonComponent.START_BUTTON
                    }
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .align(if (panelOnRight) Alignment.CenterEnd else Alignment.CenterStart)
                            .padding(8.dp)
                            .width(340.dp)
                            .fillMaxHeight()
                    ) {
                        ComponentSettingsPanel(
                            component = component,
                            config = config,
                            onConfigChange = { newConfig ->
                                modifiedConfigs = currentConfigs + (component to newConfig)
                            },
                            onDone = { selectedComponent = null },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

private val DialogWidth = 460.dp
private val WideDialogWidth = 700.dp

@Composable
private fun ConfigDialog(
    title: String,
    onDismiss: () -> Unit,
    maxWidth: Dp = DialogWidth,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                content()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions()
                }
            }
        }
    }
}

/**
 * Dialog for saving, loading and deleting named layout presets
 */
@Composable
fun PresetsDialog(
    presets: Map<String, Map<ButtonComponent, ButtonConfig>>,
    hasUnsavedChanges: Boolean,
    onSave: (String) -> Unit,
    onLoad: (Map<ButtonComponent, ButtonConfig>) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingLoad by rememberSaveable { mutableStateOf<String?>(null) }
    val trimmed = name.trim()
    val exists = trimmed in presets

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.presets_delete_title)) },
            text = { Text(stringResource(R.string.presets_delete_message, target)) },
            confirmButton = {
                Button(
                    shape = MaterialTheme.shapes.small,
                    onClick = { onDelete(target); pendingDelete = null }
                ) { Text(stringResource(R.string.presets_delete)) }
            },
            dismissButton = {
                TextButton(shape = MaterialTheme.shapes.small, onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    pendingLoad?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingLoad = null },
            title = { Text(stringResource(R.string.presets_load_title)) },
            text = { Text(stringResource(R.string.presets_load_message, target)) },
            confirmButton = {
                Button(
                    shape = MaterialTheme.shapes.small,
                    onClick = { presets[target]?.let(onLoad); pendingLoad = null }
                ) { Text(stringResource(R.string.presets_load)) }
            },
            dismissButton = {
                TextButton(shape = MaterialTheme.shapes.small, onClick = { pendingLoad = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    ConfigDialog(
        title = stringResource(R.string.presets_title),
        onDismiss = onDismiss,
        maxWidth = WideDialogWidth,
        actions = {
            Button(shape = MaterialTheme.shapes.small, onClick = onDismiss) {
                Text(stringResource(R.string.presets_close))
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    label = { Text(stringResource(R.string.presets_name_label)) },
                    singleLine = true
                )
                Button(
                    shape = MaterialTheme.shapes.small,
                    enabled = trimmed.isNotEmpty(),
                    modifier = Modifier.height(56.dp),
                    onClick = {
                        onSave(trimmed)
                        name = ""
                    }
                ) {
                    Text(
                        stringResource(
                            if (exists) R.string.presets_update else R.string.presets_save_current
                        )
                    )
                }
            }
            if (presets.isEmpty()) {
                Text(
                    stringResource(R.string.presets_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.keys.sorted().forEach { presetName ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp, end = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    presetName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    shape = MaterialTheme.shapes.small,
                                    onClick = {
                                        if (hasUnsavedChanges) {
                                            pendingLoad = presetName
                                        } else {
                                            presets[presetName]?.let(onLoad)
                                        }
                                    }
                                ) { Text(stringResource(R.string.presets_load)) }
                                IconButton(onClick = { pendingDelete = presetName }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_delete),
                                        contentDescription = stringResource(R.string.presets_delete),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog for exporting button configurations as JSON
 */
@Composable
fun ExportConfigDialog(
    buttonConfigs: Map<ButtonComponent, ButtonConfig>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    val jsonString = remember(buttonConfigs) {
        Json { prettyPrint = true }.encodeToString(buttonConfigs)
    }
    val exportLabel = stringResource(R.string.customization_export_label)
    var copied by remember { mutableStateOf(false) }

    ConfigDialog(
        title = stringResource(R.string.customization_export_title),
        onDismiss = onDismiss,
        maxWidth = WideDialogWidth,
        actions = {
            OutlinedButton(
                shape = MaterialTheme.shapes.small,
                onClick = {
                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText(exportLabel, jsonString)
                    )
                    copied = true
                    Log.i(logTag, "Exported JSON copied to clipboard")
                }
            ) {
                Text(
                    stringResource(
                        if (copied) R.string.customization_copied else R.string.customization_copy_json
                    )
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(shape = MaterialTheme.shapes.small, onClick = onDismiss) {
                Text(stringResource(R.string.customization_done))
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.customization_export_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Text(
                    text = jsonString,
                    softWrap = false,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                )
            }
        }
    }
}

/**
 * Dialog for importing button configurations from JSON
 */
@Composable
fun ImportConfigDialog(
    importedJsonText: String,
    onJsonTextChange: (String) -> Unit,
    onImport: (Map<ButtonComponent, ButtonConfig>) -> Unit,
    onDismiss: () -> Unit
) {
    var hasError by rememberSaveable { mutableStateOf(false) }

    ConfigDialog(
        title = stringResource(R.string.customization_import_title),
        onDismiss = onDismiss,
        maxWidth = WideDialogWidth,
        actions = {
            OutlinedButton(shape = MaterialTheme.shapes.small, onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                shape = MaterialTheme.shapes.small,
                enabled = importedJsonText.isNotBlank(),
                onClick = {
                    try {
                        val configs: Map<ButtonComponent, ButtonConfig> =
                            Json.decodeFromString(importedJsonText)
                        val sanitizedConfigs = sanitizeButtonConfigs(configs)
                        hasError = false
                        Log.i(logTag, "Imported and sanitized button configs: $sanitizedConfigs")
                        onImport(sanitizedConfigs)
                    } catch (e: Exception) {
                        hasError = true
                        Log.e(logTag, "Error parsing JSON", e)
                    }
                }
            ) {
                Text(stringResource(R.string.customization_import))
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.customization_import_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = importedJsonText,
                onValueChange = {
                    onJsonTextChange(it)
                    hasError = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = MaterialTheme.shapes.small,
                placeholder = { Text(stringResource(R.string.customization_import_placeholder)) },
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ),
                isError = hasError,
                supportingText = if (hasError) {
                    { Text(stringResource(R.string.customization_import_error)) }
                } else null,
                singleLine = false
            )
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Preview(
    showBackground = true,
    widthDp = 800,
    heightDp = 400
)
@Composable
fun GamepadCustomizationScreenPreview() {
    PreviewBase {
        GamepadCustomizationScreen(
            onNavigateBack = {},
            settingsRepository = SettingsRepository(LocalContext.current)
        )
    }
}
