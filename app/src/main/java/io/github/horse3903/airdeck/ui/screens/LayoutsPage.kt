package io.github.horse3903.airdeck.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.DpOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.horse3903.airdeck.R
import io.github.horse3903.airdeck.data.ButtonAnchor
import io.github.horse3903.airdeck.data.ButtonComponent
import io.github.horse3903.airdeck.data.ButtonConfig
import io.github.horse3903.airdeck.data.FACTORY_LAYOUT
import io.github.horse3903.airdeck.data.SettingsRepository
import io.github.horse3903.airdeck.data.TriggerMode
import io.github.horse3903.airdeck.data.defaultButtonConfigs
import io.github.horse3903.airdeck.data.defaultLayoutGroups
import io.github.horse3903.airdeck.data.suggestedLayouts
import io.github.horse3903.airdeck.data.exportLayout
import io.github.horse3903.airdeck.data.exportPresets
import io.github.horse3903.airdeck.data.parsePresets
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val PageWidth = 560.dp

/** When a row lands in a new spot, it glides there from the old one instead of jumping. */
private fun Modifier.animatePlacement(enabled: Boolean = true): Modifier = composed {
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf(IntOffset.Zero) }
    var animatable by remember { mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null) }
    this
        .onPlaced { target = it.positionInParent().round() }
        .offset {
            val anim = animatable ?: Animatable(target, IntOffset.VectorConverter).also { animatable = it }
            if (anim.targetValue != target) {
                if (enabled) scope.launch { anim.animateTo(target, tween(320)) } else scope.launch { anim.snapTo(target) }
            }
            // Drawn at the old spot, then it catches up to zero. A row held by the finger just follows it.
            if (enabled) anim.value - target else IntOffset.Zero
        }
}

/** A miniature of a layout, drawn from where its controls actually sit. */
@Composable
internal fun LayoutThumbnail(configs: Map<ButtonComponent, ButtonConfig>, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    // Same proportions as the real screen, so every control lands where it does on the controller
    val aspect = maxOf(config.screenWidthDp, config.screenHeightDp).toFloat() / minOf(config.screenWidthDp, config.screenHeightDp)
    Canvas(modifier.aspectRatio(aspect)) {
        val unit = size.height
        val worldW = size.width / unit
        val pad = 1f / 18f
        for (component in ButtonComponent.entries) {
            val c = configs[component] ?: ButtonConfig.default(component)
            if (!c.visible) continue
            var w: Float
            var h: Float
            var round = false
            when (component) {
                ButtonComponent.LEFT_ANALOG_STICK, ButtonComponent.RIGHT_ANALOG_STICK -> { w = 0.375f * c.scale; h = w; round = true }
                ButtonComponent.DPAD -> { w = 0.36f * c.scale; h = w; round = true }
                ButtonComponent.FACE_BUTTONS -> { w = 0.45f * c.scale; h = w; round = true }
                ButtonComponent.LEFT_TRIGGER, ButtonComponent.RIGHT_TRIGGER -> {
                    w = c.scale / 6f
                    h = w * if (c.triggerMode == TriggerMode.DIGITAL) 1.25f else 1.5f
                }
                ButtonComponent.LEFT_SHOULDER, ButtonComponent.RIGHT_SHOULDER -> { w = 0.185f * c.scale; h = 0.11f * c.scale }
                ButtonComponent.SELECT_BUTTON, ButtonComponent.START_BUTTON -> { w = 0.1f * c.scale; h = w }
                ButtonComponent.CONNECTION_BADGE -> { w = 0.3f * c.scale; h = 0.125f * c.scale }
                else -> { w = 0.125f * c.scale; h = w }
            }
            w *= c.widthScale
            h *= c.heightScale
            val ax = when (c.anchor) {
                ButtonAnchor.TOP_LEFT, ButtonAnchor.CENTER_LEFT, ButtonAnchor.BOTTOM_LEFT -> pad
                ButtonAnchor.TOP_CENTER, ButtonAnchor.CENTER, ButtonAnchor.BOTTOM_CENTER -> (worldW - w) / 2f
                else -> worldW - w - pad
            }
            val ay = when (c.anchor) {
                ButtonAnchor.TOP_LEFT, ButtonAnchor.TOP_CENTER, ButtonAnchor.TOP_RIGHT -> pad
                ButtonAnchor.CENTER_LEFT, ButtonAnchor.CENTER, ButtonAnchor.CENTER_RIGHT -> (1f - h) / 2f
                else -> 1f - h - pad
            }
            val topLeft = Offset((ax + c.offsetX) * unit, (ay + c.offsetY) * unit)
            val box = Size(w * unit, h * unit)
            val stroke = Stroke(width = 1.6f)
            when (component) {
                ButtonComponent.LEFT_ANALOG_STICK, ButtonComponent.RIGHT_ANALOG_STICK -> {
                    drawOval(ink, topLeft, box, style = stroke)
                    val d = box.width * 0.38f
                    drawOval(
                        ink.copy(alpha = 0.55f),
                        topLeft + Offset((box.width - d) / 2f, (box.height - d) / 2f),
                        Size(d, d)
                    )
                }
                ButtonComponent.DPAD -> {
                    val a = box.width / 3f
                    drawRoundRect(ink, topLeft + Offset(a, 0f), Size(a, box.height), CornerRadius(a / 4f), style = stroke)
                    drawRoundRect(ink, topLeft + Offset(0f, a), Size(box.width, a), CornerRadius(a / 4f), style = stroke)
                }
                ButtonComponent.FACE_BUTTONS -> {
                    val r = box.width * 0.15f
                    val cx = topLeft.x + box.width / 2f
                    val cy = topLeft.y + box.height / 2f
                    val o = box.width * 0.33f
                    for ((dx, dy) in listOf(0f to -o, o to 0f, 0f to o, -o to 0f)) {
                        drawCircle(ink, r, Offset(cx + dx, cy + dy), style = stroke)
                    }
                }
                else -> drawRoundRect(ink, topLeft, box, CornerRadius(box.minDimension / 4f), style = stroke)
            }
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    description: String?,
    initial: String,
    confirmLabel: String,
    taken: Set<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    val trimmed = name.trim()
    val clash = trimmed in taken && trimmed != initial
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    label = { Text(stringResource(R.string.layout_name_label)) },
                    singleLine = true,
                    isError = clash,
                    supportingText = if (clash) {
                        { Text(stringResource(R.string.layout_name_taken)) }
                    } else null
                )
            }
        },
        confirmButton = {
            Button(
                shape = MaterialTheme.shapes.small,
                enabled = trimmed.isNotEmpty() && !clash,
                onClick = { onConfirm(trimmed) }
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(shape = MaterialTheme.shapes.small, onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun ExportDialog(title: String, json: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    var copied by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.layout_export_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                ) {
                    Text(
                        text = json,
                        softWrap = false,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(shape = MaterialTheme.shapes.small, onClick = onDismiss) { Text(stringResource(R.string.done)) }
        },
        dismissButton = {
            OutlinedButton(shape = MaterialTheme.shapes.small, onClick = {
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("AirDeck layouts", json))
                copied = true
            }) { Text(stringResource(if (copied) R.string.customization_copied else R.string.customization_copy_json)) }
        }
    )
}

@Composable
private fun HeaderIcon(iconRes: Int, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(painter = painterResource(iconRes), contentDescription = description, modifier = Modifier.size(22.dp))
    }
}

/** The library of saved layouts: pick one, edit it, make a new one, or share them. */
@Composable
fun LayoutsPage(repo: SettingsRepository, onEdit: () -> Unit) {
    val scope = rememberCoroutineScope()
    val presets by repo.presets.collectAsState(initial = emptyMap())
    val presetGroups by repo.presetGroups.collectAsState(initial = emptyMap())
    val active by repo.activeLayout.collectAsState(initial = null)
    val currentButtons by repo.buttonConfigs.collectAsState(initial = defaultButtonConfigs)
    val currentGroups by repo.layoutGroups.collectAsState(initial = defaultLayoutGroups)
    val currentBehaviour by repo.layoutBehaviour.collectAsState(initial = emptyMap())
    val presetBehaviour by repo.presetBehaviour.collectAsState(initial = emptyMap())
    val defaultName = FACTORY_LAYOUT
    val importedName = stringResource(R.string.layout_imported_name)
    val copySuffix = stringResource(R.string.layout_copy_suffix)

    // The factory layout is kept as a saved layout, so it can always be picked again
    LaunchedEffect(Unit) {
        if (repo.presets.first().isEmpty()) {
            repo.savePreset(defaultName, defaultButtonConfigs, defaultLayoutGroups)
            if (repo.buttonConfigs.first() == defaultButtonConfigs && repo.layoutGroups.first() == defaultLayoutGroups) {
                repo.setActiveLayout(defaultName)
            }
        }
    }

    var showNew by rememberSaveable { mutableStateOf(false) }
    var saveCurrent by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingApply by rememberSaveable { mutableStateOf<String?>(null) }
    var exporting by rememberSaveable { mutableStateOf<String?>(null) }
    var exportAll by rememberSaveable { mutableStateOf(false) }
    var showImport by rememberSaveable { mutableStateOf(false) }
    var importText by rememberSaveable { mutableStateOf("") }
    var importError by rememberSaveable { mutableStateOf(false) }
    val names = presets.keys

    fun uniqueName(base: String): String {
        if (base !in names) return base
        var n = 2
        while ("$base $n" in names) n++
        return "$base $n"
    }

    fun apply(name: String) {
        scope.launch { repo.applyPreset(name) }
    }

    if (showNew) {
        NameDialog(
            title = stringResource(R.string.layout_new_title),
            description = stringResource(R.string.layout_new_desc),
            initial = "",
            confirmLabel = stringResource(R.string.layout_create),
            taken = names,
            onConfirm = { name ->
                showNew = false
                scope.launch {
                    repo.savePreset(name, defaultButtonConfigs, defaultLayoutGroups, emptyMap())
                    repo.applyPreset(name)
                    onEdit()
                }
            },
            onDismiss = { showNew = false }
        )
    }
    if (saveCurrent) {
        NameDialog(
            title = stringResource(R.string.layout_save_title),
            description = stringResource(R.string.layout_save_desc),
            initial = "",
            confirmLabel = stringResource(R.string.layout_save),
            taken = names,
            onConfirm = { name ->
                saveCurrent = false
                scope.launch {
                    repo.savePreset(name, currentButtons, currentGroups, currentBehaviour)
                    repo.setActiveLayout(name)
                }
            },
            onDismiss = { saveCurrent = false }
        )
    }
    renaming?.let { target ->
        NameDialog(
            title = stringResource(R.string.layout_rename),
            description = null,
            initial = target,
            confirmLabel = stringResource(R.string.layout_rename),
            taken = names,
            onConfirm = { name ->
                renaming = null
                scope.launch { repo.renamePreset(target, name) }
            },
            onDismiss = { renaming = null }
        )
    }
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.layout_delete_title)) },
            text = { Text(stringResource(R.string.presets_delete_message, target)) },
            confirmButton = {
                Button(shape = MaterialTheme.shapes.small, onClick = {
                    scope.launch { repo.deletePreset(target) }
                    pendingDelete = null
                }) { Text(stringResource(R.string.layout_delete)) }
            },
            dismissButton = {
                TextButton(shape = MaterialTheme.shapes.small, onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    pendingApply?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingApply = null },
            title = { Text(stringResource(R.string.presets_apply_title)) },
            text = { Text(stringResource(R.string.presets_apply_message, target)) },
            confirmButton = {
                Button(shape = MaterialTheme.shapes.small, onClick = { apply(target); pendingApply = null }) {
                    Text(stringResource(R.string.layout_apply))
                }
            },
            dismissButton = {
                TextButton(shape = MaterialTheme.shapes.small, onClick = { pendingApply = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    exporting?.let { target ->
        val buttons = presets[target]
        if (buttons != null) {
            ExportDialog(
                title = stringResource(R.string.layout_export_one_title, target),
                json = exportLayout(buttons, presetGroups[target] ?: defaultLayoutGroups, presetBehaviour[target] ?: emptyMap()),
                onDismiss = { exporting = null }
            )
        }
    }
    if (exportAll) {
        ExportDialog(
            title = stringResource(R.string.layout_export_all_title),
            json = exportPresets(presets, presetGroups, presetBehaviour),
            onDismiss = { exportAll = false }
        )
    }
    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false; importText = ""; importError = false },
            title = { Text(stringResource(R.string.layout_import_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.layout_import_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it; importError = false },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        shape = MaterialTheme.shapes.small,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        isError = importError,
                        supportingText = if (importError) {
                            { Text(stringResource(R.string.customization_import_error)) }
                        } else null
                    )
                }
            },
            confirmButton = {
                Button(
                    shape = MaterialTheme.shapes.small,
                    enabled = importText.isNotBlank(),
                    onClick = {
                        try {
                            val found = parsePresets(importText, importedName)
                            scope.launch {
                                for ((n, bundle) in found) {
                                    val target = if (found.size == 1 && n == importedName) uniqueName(n) else n
                                    repo.savePreset(
                                        target,
                                        ButtonComponent.entries.associateWith { bundle.buttons[it] ?: ButtonConfig.default(it) },
                                        bundle.groups ?: defaultLayoutGroups,
                                        bundle.behaviour ?: emptyMap()
                                    )
                                }
                            }
                            showImport = false; importText = ""; importError = false
                        } catch (e: Exception) {
                            importError = true
                        }
                    }
                ) { Text(stringResource(R.string.customization_import)) }
            },
            dismissButton = {
                TextButton(shape = MaterialTheme.shapes.small, onClick = { showImport = false; importText = ""; importError = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    val unsaved = active == null || active !in presets

    Column(
        modifier = Modifier
            .widthIn(max = PageWidth)
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .animateContentSize(tween(250)),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Only when the layout in use has never been saved under a name
        if (unsaved) {
            SectionLabel(stringResource(R.string.layout_current))
            LayoutRow(
                name = stringResource(R.string.layout_custom),
                subtitle = stringResource(R.string.layout_unsaved),
                configs = currentButtons,
                isActive = true,
                onClick = {},
                trailing = {
                    Button(
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(40.dp),
                        onClick = { saveCurrent = true }
                    ) { Text(stringResource(R.string.layout_save_short), fontSize = 14.sp) }
                    RowIcon(R.drawable.ic_lucide_pencil, stringResource(R.string.layout_edit_current)) { onEdit() }
                }
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(stringResource(R.string.layout_header) + " (" + presets.size + ")", Modifier.weight(1f))
            HeaderIcon(R.drawable.ic_lucide_file_input, stringResource(R.string.layout_import_title)) { showImport = true }
            HeaderIcon(R.drawable.ic_lucide_file_output, stringResource(R.string.layout_export_all_title), enabled = presets.isNotEmpty()) {
                exportAll = true
            }
            OutlinedButton(
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(40.dp),
                onClick = { showNew = true }
            ) { Text("+  " + stringResource(R.string.layout_new), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) }
        }

        // Your layouts in your order, then the built-in one, which always stays at the bottom
        val stored = presets.keys.filter { it != FACTORY_LAYOUT }
        var order by remember(stored) { mutableStateOf(stored) }
        var dragging by remember { mutableStateOf<String?>(null) }
        var dragY by remember { mutableFloatStateOf(0f) }
        var rowStep by remember { mutableFloatStateOf(0f) }
        val gap = with(LocalDensity.current) { 8.dp.toPx() }
        val shown = order + listOfNotNull(FACTORY_LAYOUT.takeIf { it in presets })

        fun swap(list: List<String>, a: Int, b: Int): List<String> =
            list.toMutableList().also { val x = it[a]; it[a] = it[b]; it[b] = x }

        shown.forEach { name -> presets[name]?.let { configs -> key(name) {
            val isActive = name == active
            val factory = name == FACTORY_LAYOUT
            val index = order.indexOf(name)
            val held = dragging == name
            var menuOpen by rememberSaveable(name) { mutableStateOf(false) }
            LayoutRow(
                modifier = Modifier
                    .animatePlacement(enabled = !held)
                    .onGloballyPositioned { if (!factory && rowStep == 0f) rowStep = it.size.height + gap }
                    .zIndex(if (held) 1f else 0f)
                    .graphicsLayer { translationY = if (held) dragY else 0f }
                    .then(if (held) Modifier.shadow(8.dp, RoundedCornerShape(8.dp)) else Modifier),
                leading = {
                    // Three lines to grab: drag them to move the layout. The built-in layout stays put.
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 44.dp)
                            .then(
                                if (factory) Modifier else Modifier
                                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { }
                                    .pointerInput(name) {
                                        detectDragGestures(
                                            onDragStart = { dragging = name; dragY = 0f },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragY += amount.y
                                                val at = order.indexOf(name)
                                                if (rowStep > 0f && dragY > rowStep / 2f && at < order.lastIndex) {
                                                    order = swap(order, at, at + 1); dragY -= rowStep
                                                } else if (rowStep > 0f && dragY < -rowStep / 2f && at > 0) {
                                                    order = swap(order, at, at - 1); dragY += rowStep
                                                }
                                            },
                                            onDragEnd = {
                                                dragging = null; dragY = 0f
                                                scope.launch { repo.reorderPresets(order) }
                                            },
                                            onDragCancel = { dragging = null; dragY = 0f; order = stored }
                                        )
                                    }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!factory) {
                            Icon(
                                painter = painterResource(R.drawable.ic_menu),
                                contentDescription = stringResource(R.string.layout_drag_handle),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                name = name,
                subtitle = when {
                    isActive -> stringResource(R.string.layout_active)
                    factory -> stringResource(R.string.layout_factory)
                    else -> null
                },
                configs = configs,
                isActive = isActive,
                onClick = {
                    if (!isActive) {
                        if (unsaved) pendingApply = name else apply(name)
                    }
                },
                trailing = {
                    // Three fixed slots, so the actions line up from row to row
                    Crossfade(targetState = isActive, animationSpec = tween(250), label = "state") { active ->
                        if (active) {
                            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_lucide_circle_check),
                                    contentDescription = stringResource(R.string.layout_active),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            RowIcon(R.drawable.ic_lucide_circle, stringResource(R.string.layout_apply)) {
                                if (unsaved) pendingApply = name else apply(name)
                            }
                        }
                    }
                    RowIcon(R.drawable.ic_lucide_pencil, stringResource(R.string.layout_edit), enabled = !factory) {
                        scope.launch { repo.applyPreset(name); onEdit() }
                    }
                    Box {
                        RowIcon(R.drawable.ic_lucide_more, stringResource(R.string.layout_more)) { menuOpen = true }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            offset = DpOffset(x = 44.dp - 176.dp, y = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.width(176.dp)
                        ) {
                            CompactMenuItem(stringResource(R.string.layout_move_up), R.drawable.ic_lucide_arrow_up, enabled = !factory && index > 0) {
                                menuOpen = false
                                val next = swap(order, index, index - 1)
                                scope.launch { repo.reorderPresets(next) }
                            }
                            CompactMenuItem(stringResource(R.string.layout_move_down), R.drawable.ic_lucide_arrow_down, enabled = !factory && index in 0 until order.lastIndex) {
                                menuOpen = false
                                val next = swap(order, index, index + 1)
                                scope.launch { repo.reorderPresets(next) }
                            }
                            CompactMenuItem(stringResource(R.string.layout_rename), R.drawable.ic_lucide_type, enabled = !factory) {
                                menuOpen = false; renaming = name
                            }
                            CompactMenuItem(stringResource(R.string.layout_duplicate), R.drawable.ic_lucide_copy) {
                                menuOpen = false
                                scope.launch {
                                    repo.savePreset(uniqueName(name + copySuffix), configs, presetGroups[name] ?: defaultLayoutGroups, presetBehaviour[name] ?: emptyMap())
                                }
                            }
                            CompactMenuItem(stringResource(R.string.layout_export), R.drawable.ic_lucide_download) {
                                menuOpen = false; exporting = name
                            }
                            CompactMenuItem(stringResource(R.string.layout_delete), R.drawable.ic_lucide_trash, destructive = true, enabled = !factory) {
                                menuOpen = false; pendingDelete = name
                            }
                        }
                    }
                }
            )
        } } }

        // Ready-made layouts for kinds of games. Using one saves a copy that can be edited freely.
        Spacer(Modifier.height(8.dp))
        SectionLabel(stringResource(R.string.layout_suggested))
        suggestedLayouts.forEach { s ->
            LayoutRow(
                name = s.name,
                subtitle = s.description,
                configs = s.buttons,
                isActive = false,
                onClick = {},
                trailing = {
                    OutlinedButton(
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.height(40.dp),
                        onClick = {
                            scope.launch {
                                val to = uniqueName(s.name)
                                repo.savePreset(to, s.buttons, defaultLayoutGroups, s.behaviour)
                                repo.applyPreset(to)
                            }
                        }
                    ) { Text(stringResource(R.string.layout_use), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) }
                }
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RowIcon(iconRes: Int, description: String, destructive: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(44.dp)) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = (if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                .copy(alpha = if (enabled) 1f else 0.35f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CompactMenuItem(label: String, iconRes: Int, destructive: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    DropdownMenuItem(
        enabled = enabled,
        modifier = Modifier.heightIn(min = 40.dp),
        leadingIcon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        },
        text = {
            Text(
                label,
                fontSize = 14.sp,
                color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        },
        onClick = onClick
    )
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(top = 8.dp, bottom = 2.dp),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, letterSpacing = 0.8.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun LayoutRow(
    modifier: Modifier = Modifier,
    name: String,
    subtitle: String?,
    configs: Map<ButtonComponent, ButtonConfig>,
    isActive: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val fill by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        tween(250), label = "fill"
    )
    val edge by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
        tween(250), label = "edge"
    )
    Surface(
        shape = shape,
        color = fill,
        border = BorderStroke(1.dp, edge),
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.heightIn(min = 64.dp).padding(start = 2.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading()
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                LayoutThumbnail(configs, Modifier.width(96.dp).padding(3.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium))
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { trailing() }
        }
    }
}
