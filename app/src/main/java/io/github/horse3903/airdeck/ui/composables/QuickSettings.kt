package io.github.horse3903.airdeck.ui.composables

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.LaunchedEffect
import io.github.horse3903.airdeck.ui.screens.LayoutThumbnail
import io.github.horse3903.airdeck.data.ButtonComponent
import io.github.horse3903.airdeck.data.ButtonConfig
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.horse3903.airdeck.R
import io.github.horse3903.airdeck.data.GamepadStyle
import io.github.horse3903.airdeck.data.SettingsRepository
import io.github.horse3903.airdeck.data.defaultButtonConfigs
import io.github.horse3903.airdeck.data.defaultColorScheme
import io.github.horse3903.airdeck.data.defaultGamepadStyle
import io.github.horse3903.airdeck.data.defaultLayoutGroups
import io.github.horse3903.airdeck.data.exportLayout
import io.github.horse3903.airdeck.data.parseLayout
import kotlinx.coroutines.launch

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        letterSpacing = 0.08.em,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
    )
}

/** One saved layout as a card with its miniature. */
@Composable
private fun LayoutCard(
    name: String,
    configs: Map<ButtonComponent, ButtonConfig>,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(170.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainer)
            .border(
                1.dp,
                if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            LayoutThumbnail(configs, Modifier.width(148.dp).padding(3.dp))
        }
        Text(name, fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
    }
}

/** The two button styles as side by side cards, the chosen one outlined, like the browser controller. */
@Composable
private fun StyleCards(selected: GamepadStyle, onSelect: (GamepadStyle) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (style in listOf(GamepadStyle.XBOX, GamepadStyle.PLAYSTATION, GamepadStyle.NINTENDO)) {
            val picked = style == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (picked) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                    .border(
                        1.dp,
                        if (picked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(style) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                when (style) {
                    GamepadStyle.XBOX -> XboxSymbols()
                    GamepadStyle.PLAYSTATION -> PlayStationSymbols()
                    GamepadStyle.NINTENDO -> NintendoSymbols()
                }
                Text(stringResource(style.nameRes), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActionButton(iconRes: Int, label: String, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(18.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Quick settings from inside the game, laid out like the browser controller's settings sheet. */
@Composable
fun QuickSettingsDialog(settingsRepository: SettingsRepository, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme by settingsRepository.colorScheme.collectAsState(initial = defaultColorScheme)
    val style by settingsRepository.gamepadStyle.collectAsState(initial = defaultGamepadStyle)
    val configs by settingsRepository.buttonConfigs.collectAsState(initial = defaultButtonConfigs)
    val groups by settingsRepository.layoutGroups.collectAsState(initial = defaultLayoutGroups)
    val presets by settingsRepository.presets.collectAsState(initial = emptyMap())
    val behaviour by settingsRepository.layoutBehaviour.collectAsState(initial = emptyMap())
    val active by settingsRepository.activeLayout.collectAsState(initial = null)
    LaunchedEffect(Unit) { settingsRepository.ensureFactoryLayout() }
    val copied = stringResource(R.string.quick_layout_copied)
    val imported = stringResource(R.string.quick_layout_imported)
    val invalid = stringResource(R.string.quick_layout_invalid)
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.94f).dp

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        androidx.compose.runtime.SideEffect { window?.setDimAmount(0.5f) }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth(0.94f)
                .heightIn(max = maxHeight)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.quick_settings_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(android.R.string.ok),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, bottom = 18.dp)
                ) {
                    SectionTitle(stringResource(R.string.sec_appearance))
                    Text(stringResource(R.string.quick_style), fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    StyleCards(selected = style, onSelect = { scope.launch { settingsRepository.setGamepadStyle(it) } })
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_color_scheme), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(
                                stringResource(R.string.quick_theme_hint),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        ColorSchemeChoice(
                            selected = scheme,
                            onSelect = { scope.launch { settingsRepository.setColorScheme(it) } },
                            labelFor = { stringResource(it.nameRes) },
                            modifier = Modifier.width(232.dp)
                        )
                    }
                    HorizontalDivider(Modifier.padding(top = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    SectionTitle(stringResource(R.string.quick_section_layout))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // A layout that was never saved under a name comes first
                        if (active == null || active !in presets) {
                            LayoutCard(stringResource(R.string.layout_custom), configs, active = true, onClick = {})
                        }
                        presets.forEach { (name, layout) ->
                            LayoutCard(name, layout, active = name == active) {
                                scope.launch { settingsRepository.applyPreset(name) }
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(top = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    val appPrefs by settingsRepository.appPrefs.collectAsState(initial = io.github.horse3903.airdeck.data.AppPrefs())
                    if (appPrefs.tiltTarget != 0) {
                        SectionTitle(stringResource(R.string.quick_section_tilt))
                        val calibrated = stringResource(R.string.quick_tilt_calibrated)
                        ActionButton(R.drawable.ic_lucide_circle, stringResource(R.string.quick_tilt_calibrate), Modifier.fillMaxWidth()) {
                            io.github.horse3903.airdeck.ui.utils.TiltSteering.calibrate()
                            Toast.makeText(context, calibrated, Toast.LENGTH_SHORT).show()
                        }
                        HorizontalDivider(Modifier.padding(top = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    SectionTitle(stringResource(R.string.quick_section_config))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionButton(R.drawable.ic_file_download, stringResource(R.string.customization_export), Modifier.weight(1f)) {
                            val json = exportLayout(configs, groups, behaviour)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("gamepad layout", json))
                            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                        }
                        ActionButton(R.drawable.ic_file_upload, stringResource(R.string.customization_import), Modifier.weight(1f)) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                            val parsed = runCatching { parseLayout(text) }.getOrNull()
                            if (parsed == null) {
                                Toast.makeText(context, invalid, Toast.LENGTH_SHORT).show()
                            } else {
                                scope.launch {
                                    settingsRepository.setAllButtonConfigs(parsed.buttons)
                                    parsed.groups?.let { settingsRepository.setLayoutGroups(it) }
                                    parsed.behaviour?.let { settingsRepository.setLayoutBehaviour(it) }
                                }
                                Toast.makeText(context, imported, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }
}
