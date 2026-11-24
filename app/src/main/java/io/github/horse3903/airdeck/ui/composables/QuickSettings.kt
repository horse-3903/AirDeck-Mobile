package io.github.horse3903.airdeck.ui.composables

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.horse3903.airdeck.R
import io.github.horse3903.airdeck.data.ColorScheme
import io.github.horse3903.airdeck.data.GamepadStyle
import io.github.horse3903.airdeck.data.SettingsRepository
import io.github.horse3903.airdeck.data.defaultButtonConfigs
import io.github.horse3903.airdeck.data.defaultColorScheme
import io.github.horse3903.airdeck.data.defaultGamepadStyle
import io.github.horse3903.airdeck.data.parseLenientButtonConfigs
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
private fun <T> Choice(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (option in options) {
            val modifier = Modifier.weight(1f)
            if (option == selected) {
                Button(onClick = { onSelect(option) }, modifier = modifier, shape = RoundedCornerShape(12.dp)) {
                    Text(label(option), maxLines = 1)
                }
            } else {
                OutlinedButton(onClick = { onSelect(option) }, modifier = modifier, shape = RoundedCornerShape(12.dp)) {
                    Text(label(option), maxLines = 1)
                }
            }
        }
    }
}

/** Quick settings from inside the game: theme, button style and layout import and export. */
@Composable
fun QuickSettingsDialog(settingsRepository: SettingsRepository, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme by settingsRepository.colorScheme.collectAsState(initial = defaultColorScheme)
    val style by settingsRepository.gamepadStyle.collectAsState(initial = defaultGamepadStyle)
    val configs by settingsRepository.buttonConfigs.collectAsState(initial = defaultButtonConfigs)
    val copied = stringResource(R.string.quick_layout_copied)
    val imported = stringResource(R.string.quick_layout_imported)
    val invalid = stringResource(R.string.quick_layout_invalid)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        androidx.compose.runtime.SideEffect { window?.setDimAmount(0.5f) }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.62f)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.quick_settings_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.quick_theme), style = MaterialTheme.typography.labelLarge)
                Choice(
                    options = listOf(ColorScheme.SYSTEM, ColorScheme.LIGHT, ColorScheme.DARK),
                    selected = scheme,
                    label = { stringResource(it.nameRes) },
                    onSelect = { scope.launch { settingsRepository.setColorScheme(it) } }
                )
                Text(stringResource(R.string.quick_style), style = MaterialTheme.typography.labelLarge)
                Choice(
                    options = GamepadStyle.entries,
                    selected = style,
                    label = { stringResource(it.nameRes) },
                    onSelect = { scope.launch { settingsRepository.setGamepadStyle(it) } }
                )
                Text(stringResource(R.string.quick_layout), style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            val json = Json { prettyPrint = true }.encodeToString(configs)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("gamepad layout", json))
                            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                        }
                    ) { Text(stringResource(R.string.customization_export), maxLines = 1) }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                            val parsed = runCatching { parseLenientButtonConfigs(text) }.getOrNull()
                            if (parsed == null) {
                                Toast.makeText(context, invalid, Toast.LENGTH_SHORT).show()
                            } else {
                                scope.launch { settingsRepository.setAllButtonConfigs(parsed) }
                                Toast.makeText(context, imported, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) { Text(stringResource(R.string.customization_import), maxLines = 1) }
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }
}
