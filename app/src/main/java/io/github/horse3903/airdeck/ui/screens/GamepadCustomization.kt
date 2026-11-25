package io.github.horse3903.airdeck.ui.screens

import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import io.github.horse3903.airdeck.ui.composables.LinkStatus
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import android.os.SystemClock
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.github.horse3903.airdeck.data.LayoutGroup
import io.github.horse3903.airdeck.data.decodeGroups
import io.github.horse3903.airdeck.data.defaultLayoutGroups
import io.github.horse3903.airdeck.data.dissolveGroup
import io.github.horse3903.airdeck.data.encodeGroups
import io.github.horse3903.airdeck.data.groupOf
import io.github.horse3903.airdeck.data.moveToGroup
import io.github.horse3903.airdeck.data.snapPosition
import io.github.horse3903.airdeck.data.snapScale
import io.github.horse3903.airdeck.data.unionOf
import io.github.horse3903.airdeck.data.withNewGroup
import io.github.horse3903.airdeck.ui.composables.EditorHit
import io.github.horse3903.airdeck.ui.composables.GroupChoice
import io.github.horse3903.airdeck.ui.composables.GroupSettingsPanel
import io.github.horse3903.airdeck.ui.composables.LayoutEditorOverlay
import io.github.horse3903.airdeck.ui.composables.NEW_GROUP_ID
import io.github.horse3903.airdeck.ui.composables.groupArea
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.graphics.graphicsLayer
import io.github.horse3903.airdeck.ui.composables.TopControls
import io.github.horse3903.airdeck.ui.composables.ComponentSettingsPanel
import io.github.horse3903.airdeck.data.OFFSET_VALUE_RANGE
import io.github.horse3903.airdeck.data.ButtonAnchor
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
import io.github.horse3903.airdeck.R
import io.github.horse3903.airdeck.data.ButtonComponent
import io.github.horse3903.airdeck.data.ButtonConfig
import io.github.horse3903.airdeck.data.PreviewBase
import io.github.horse3903.airdeck.data.SettingsRepository
import io.github.horse3903.airdeck.data.defaultButtonConfigs
import io.github.horse3903.airdeck.ui.composables.ButtonConfigEditor
import io.github.horse3903.airdeck.ui.composables.DrawGamepad
import io.github.horse3903.airdeck.ui.composables.ResponsiveGrid
import kotlinx.coroutines.runBlocking
import io.github.horse3903.airdeck.data.exportLayout
import io.github.horse3903.airdeck.data.parseLayout
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
    var showDiscard by rememberSaveable { mutableStateOf(false) }
    var selectedComponent by rememberSaveable { mutableStateOf<ButtonComponent?>(null) }
    var selectedGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var modifiedGroupsJson by rememberSaveable { mutableStateOf<String?>(null) }
    val savedGroups by settingsRepository.layoutGroups.collectAsState(initial = defaultLayoutGroups)
    val groups = remember(modifiedGroupsJson, savedGroups) { decodeGroups(modifiedGroupsJson) ?: savedGroups }
    val hasChanges = modifiedConfigs != null || modifiedGroupsJson != null
    val presets by settingsRepository.presets.collectAsState(initial = emptyMap())
    val presetGroups by settingsRepository.presetGroups.collectAsState(initial = emptyMap())
    val presetScope = rememberCoroutineScope()
    var importJsonText by rememberSaveable { mutableStateOf("") }

    // Get the current configs to preview (modified or saved)
    val currentConfigs = modifiedConfigs ?: buttonConfigs

    // Undo and redo: a snapshot of the unsaved edits is taken before each change. A run of quick changes,
    // like dragging a slider, counts as one.
    val undoStack = remember { mutableStateListOf<EditSnapshot>() }
    val redoStack = remember { mutableStateListOf<EditSnapshot>() }
    var lastRecorded by remember { mutableLongStateOf(0L) }
    fun recordEdit(coalesce: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        val continuing = coalesce && now - lastRecorded < 800
        lastRecorded = now
        if (continuing) return
        undoStack.add(EditSnapshot(modifiedConfigs, modifiedGroupsJson))
        if (undoStack.size > 100) undoStack.removeAt(0)
        redoStack.clear()
    }
    fun undoEdit() {
        val snap = undoStack.removeLastOrNull() ?: return
        redoStack.add(EditSnapshot(modifiedConfigs, modifiedGroupsJson))
        modifiedConfigs = snap.configs
        modifiedGroupsJson = snap.groupsJson
        lastRecorded = 0L
    }
    fun redoEdit() {
        val snap = redoStack.removeLastOrNull() ?: return
        undoStack.add(EditSnapshot(modifiedConfigs, modifiedGroupsJson))
        modifiedConfigs = snap.configs
        modifiedGroupsJson = snap.groupsJson
        lastRecorded = 0L
    }

    // Handle back button to close preview
    BackHandler(enabled = showPreview) {
        showPreview = false
    }
    BackHandler(enabled = !showPreview && hasChanges) {
        showDiscard = true
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_message)) },
            confirmButton = {
                Button(
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    onClick = { showDiscard = false; onNavigateBack() }
                ) { Text(stringResource(R.string.discard_confirm)) }
            },
            dismissButton = {
                TextButton(
                    shape = MaterialTheme.shapes.small,
                    onClick = { showDiscard = false }
                ) { Text(stringResource(R.string.discard_keep)) }
            }
        )
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
                        modifiedGroupsJson = null
                        undoStack.clear()
                        redoStack.clear()
                        runBlocking {
                            settingsRepository.setAllButtonConfigs(defaultButtonConfigs)
                            settingsRepository.setLayoutGroups(defaultLayoutGroups)
                        }
                        selectedComponent = null
                        selectedGroupId = null
                        showResetConfirm = false
                        Log.i(logTag, "Button configs reset to defaults")
                    }
                ) { Text(stringResource(R.string.reset_layout_confirm)) }
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
            hasUnsavedChanges = hasChanges,
            onSave = { name ->
                val configs = currentConfigs
                val savedGroups = groups
                presetScope.launch { settingsRepository.savePreset(name, configs, savedGroups) }
            },
            onLoad = { name, configs ->
                recordEdit()
                modifiedConfigs = configs
                // A preset saved before groups existed leaves the current groups alone
                presetGroups[name]?.let { modifiedGroupsJson = encodeGroups(it) }
                showPresets = false
            },
            onDelete = { name -> presetScope.launch { settingsRepository.deletePreset(name) } },
            onDismiss = { showPresets = false }
        )
    }

    if (showExportDialog) {
        ExportConfigDialog(
            buttonConfigs = currentConfigs,
            groups = groups,
            onDismiss = { showExportDialog = false }
        )
    }

    if (showImportDialog) {
        ImportConfigDialog(
            importedJsonText = importJsonText,
            onJsonTextChange = { importJsonText = it },
            onImport = { configs, importedGroups ->
                recordEdit()
                modifiedConfigs = configs
                importedGroups?.let { modifiedGroupsJson = encodeGroups(it) }
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
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val canvasWidth = configuration.screenWidthDp
    val canvasHeight = configuration.screenHeightDp
    val unitPx = density.density * canvasHeight
    val snapPx = with(density) { 6.dp.toPx() }
    val bounds = remember { mutableStateMapOf<ButtonComponent, Rect>() }
    val session = remember { DragSession() }
    var guideX by remember { mutableStateOf<Float?>(null) }
    var guideY by remember { mutableStateOf<Float?>(null) }

    fun selectGroup(id: String) { selectedGroupId = id; selectedComponent = null }
    fun selectControl(component: ButtonComponent) { selectedGroupId = null; selectedComponent = component }
    fun setGroups(list: List<LayoutGroup>) { recordEdit(); modifiedGroupsJson = encodeGroups(list) }

    // A first tap on a grouped control picks its group. A tap on a control inside the picked group
    // (or beside an already picked control of it) edits that control alone.
    fun onTapHit(hit: EditorHit?) {
        when (hit) {
            null -> { selectedGroupId = null; selectedComponent = null }
            is EditorHit.Group -> selectGroup(hit.id)
            is EditorHit.Control -> {
                val group = groupOf(groups, hit.component)
                val inPicked = group != null &&
                    (selectedGroupId == group.id || selectedComponent?.let { it in group.members } == true)
                if (group == null || inPicked) selectControl(hit.component) else selectGroup(group.id)
            }
        }
    }

    // Dragging moves the picked group as a whole, or a single control
    fun onDragStartHit(hit: EditorHit?) {
        val members: List<ButtonComponent>? = when (hit) {
            null -> null
            is EditorHit.Group -> { selectGroup(hit.id); groups.firstOrNull { it.id == hit.id }?.members }
            is EditorHit.Control -> {
                val group = groupOf(groups, hit.component)
                when {
                    group == null -> { selectControl(hit.component); listOf(hit.component) }
                    selectedGroupId == group.id -> group.members
                    selectedComponent?.let { it in group.members } == true -> {
                        selectControl(hit.component); listOf(hit.component)
                    }
                    else -> { selectGroup(group.id); group.members }
                }
            }
        }
        if (members.orEmpty().isNotEmpty()) recordEdit()
        session.begin(members.orEmpty(), bounds, currentConfigs)
    }

    fun onDragBy(amount: Offset) {
        val start = session.startRect ?: return
        session.total += amount
        val snap = snapPosition(start.translate(session.total), session.others, snapPx)
        guideX = snap.xLine
        guideY = snap.yLine
        val fx = session.total.x + snap.dx
        val fy = session.total.y + snap.dy
        var next = session.base
        for (member in session.members) {
            val (ox, oy) = session.startOffsets[member] ?: continue
            val config = session.base[member] ?: ButtonConfig.default(member)
            next = next + (member to config.copy(
                offsetX = (ox + fx / unitPx).coerceIn(OFFSET_VALUE_RANGE),
                offsetY = (oy + fy / unitPx).coerceIn(OFFSET_VALUE_RANGE),
            ))
        }
        modifiedConfigs = next
    }

    // Same size as the play screen, so what you edit is what you get. Preview makes it playable.
    Box(modifier = Modifier.fillMaxSize()) {
        DrawGamepad(
            widthDp = canvasWidth,
            heightDp = canvasHeight,
            gamepadState = editorState,
            buttonConfigs = currentConfigs,
            editing = !showPreview,
            onComponentBounds = { component, rect -> bounds[component] = rect },
            selectedComponent = if (showPreview) null else selectedComponent,
            onBack = { showPreview = false },
            glowSlot = if (showPreview) 1 else null,
            topControls = TopControls(LinkStatus.CONNECTED, 1, {}, {}),
        )

        if (!showPreview) {
            LayoutEditorOverlay(
                bounds = bounds,
                groups = groups,
                selectedGroupId = selectedGroupId,
                guideX = guideX,
                guideY = guideY,
                onTap = { onTapHit(it) },
                onDragStart = { onDragStartHit(it) },
                onDrag = { onDragBy(it) },
                onDragEnd = { guideX = null; guideY = null },
            )
        }

        EditToolbar(
            previewing = showPreview,
            showHint = !showPreview && selectedComponent == null && selectedGroupId == null,
            onCancel = { if (hasChanges) showDiscard = true else onNavigateBack() },
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            onUndo = { undoEdit() },
            onRedo = { redoEdit() },
            onSelectEdit = { showPreview = false },
            onSelectPreview = { selectedComponent = null; selectedGroupId = null; showPreview = true },
            onPresets = { showPresets = true },
            onImport = { showImportDialog = true },
            onExport = { showExportDialog = true },
            onReset = { showResetConfirm = true },
            onDone = {
                runBlocking {
                    try {
                        modifiedConfigs?.let { settingsRepository.setAllButtonConfigs(it) }
                        decodeGroups(modifiedGroupsJson)?.let { settingsRepository.setLayoutGroups(it) }
                        Log.i(logTag, "Layout saved")
                    } catch (e: Exception) {
                        Log.e(logTag, "Error saving layout", e)
                    }
                }
                onNavigateBack()
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = (canvasHeight * 0.061f - 27f).coerceAtLeast(0f).dp)
        )

        val pickedGroup = selectedGroupId?.takeIf { !showPreview }?.let { id -> groups.firstOrNull { it.id == id } }
        if (pickedGroup != null) {
            val area = groupArea(pickedGroup, bounds, 0f)
            val onRight = area == null || area.center.x < canvasWidth * density.density / 2f
            val groupVisible = pickedGroup.members.all { (currentConfigs[it] ?: ButtonConfig.default(it)).visible }
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .align(if (onRight) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(8.dp)
                    .width(340.dp)
                    .fillMaxHeight()
            ) {
                GroupSettingsPanel(
                    title = groupLabel(pickedGroup),
                    members = pickedGroup.members.map { it to stringResource(it.nameRes) },
                    allVisible = groupVisible,
                    onVisibleChange = { visible ->
                        recordEdit()
                        modifiedConfigs = currentConfigs + pickedGroup.members.map {
                            it to (currentConfigs[it] ?: ButtonConfig.default(it)).copy(visible = visible)
                        }
                    },
                    onSelectMember = { selectControl(it) },
                    onUngroup = {
                        setGroups(dissolveGroup(groups, pickedGroup.id))
                        selectedGroupId = null
                    },
                    onDone = { selectedGroupId = null },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        selectedComponent?.takeIf { !showPreview }?.let { component ->
            val config = currentConfigs[component] ?: ButtonConfig.default(component)
            // Open on the side away from the control being edited
            val panelOnRight = when (config.anchor) {
                ButtonAnchor.TOP_LEFT, ButtonAnchor.CENTER_LEFT, ButtonAnchor.BOTTOM_LEFT -> true
                ButtonAnchor.TOP_RIGHT, ButtonAnchor.CENTER_RIGHT, ButtonAnchor.BOTTOM_RIGHT -> false
                else -> config.offsetX <= 0f
            }
            val noneChoice = GroupChoice(null, stringResource(R.string.group_none))
            val newChoice = GroupChoice(NEW_GROUP_ID, stringResource(R.string.group_new))
            val choices = listOf(noneChoice) +
                groups.map { GroupChoice(it.id, groupLabel(it)) } + newChoice
            val memberOf = groupOf(groups, component)
            val currentChoice = choices.first { it.id == memberOf?.id }
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
                        var next = newConfig
                        val own = bounds[component]
                        if (next.scale != config.scale && own != null) {
                            // A light pull towards another control's width or height
                            next = next.copy(
                                scale = snapScale(
                                    config.scale, next.scale, own,
                                    bounds.filterKeys { it != component }.values.toList()
                                )
                            )
                        }
                        recordEdit(coalesce = true)
                        modifiedConfigs = currentConfigs + (component to next)
                    },
                    onDone = { selectedComponent = null },
                    modifier = Modifier.fillMaxSize(),
                    groupChoices = choices,
                    currentGroup = currentChoice,
                    onGroupChange = { choice ->
                        setGroups(
                            when (choice.id) {
                                null -> moveToGroup(groups, component, null)
                                NEW_GROUP_ID -> withNewGroup(groups, component)
                                else -> moveToGroup(groups, component, choice.id)
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun groupLabel(group: LayoutGroup): String = when (group.id) {
    "left_shoulders" -> stringResource(R.string.group_left_shoulders)
    "right_shoulders" -> stringResource(R.string.group_right_shoulders)
    "left_controls" -> stringResource(R.string.group_left_controls)
    "right_controls" -> stringResource(R.string.group_right_controls)
    "top_bar" -> stringResource(R.string.group_top_bar)
    else -> group.name
}

/** The unsaved layout edits at one point in time, for undo and redo. */
private class EditSnapshot(val configs: Map<ButtonComponent, ButtonConfig>?, val groupsJson: String?)

/** What one drag gesture is moving, captured when it starts so snapping works from the original positions. */
private class DragSession {
    var members: List<ButtonComponent> = emptyList()
    var startRect: Rect? = null
    var others: List<Rect> = emptyList()
    var startOffsets: Map<ButtonComponent, Pair<Float, Float>> = emptyMap()
    var base: Map<ButtonComponent, ButtonConfig> = emptyMap()
    var total: Offset = Offset.Zero

    fun begin(
        members: List<ButtonComponent>,
        bounds: Map<ButtonComponent, Rect>,
        configs: Map<ButtonComponent, ButtonConfig>
    ) {
        this.members = members
        startRect = unionOf(members.mapNotNull { bounds[it] })
        others = bounds.filterKeys { it !in members }.values.toList()
        base = configs
        startOffsets = members.associateWith {
            val c = configs[it] ?: ButtonConfig.default(it)
            c.offsetX to c.offsetY
        }
        total = Offset.Zero
    }
}

/**
 * An icon-only bar that floats over the play screen's back, slot and gear cluster while editing:
 * Cancel, options, an Edit / Preview switch and Done. Drag it anywhere to move it.
 */
@Composable
private fun EditToolbar(
    previewing: Boolean,
    showHint: Boolean,
    onCancel: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSelectEdit: () -> Unit,
    onSelectPreview: () -> Unit,
    onPresets: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var overflowOpen by remember { mutableStateOf(false) }
    val outline = MaterialTheme.colorScheme.outlineVariant
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    var dragX by rememberSaveable { mutableStateOf(0f) }
    var dragY by rememberSaveable { mutableStateOf(0f) }
    val maxX = with(density) { (configuration.screenWidthDp / 2 - 120).dp.toPx() }
    val maxY = with(density) { (configuration.screenHeightDp - 90).dp.toPx() }
    Column(
        modifier = modifier
            .offset { IntOffset(dragX.roundToInt(), dragY.roundToInt()) }
            // Swallow touches on the bar so nothing underneath it reacts
            .pointerInput(Unit) { detectTapGestures { } }
            .pointerInput(Unit) {
                detectDragGestures { change, amount ->
                    change.consume()
                    dragX = (dragX + amount.x).coerceIn(-maxX, maxX)
                    dragY = (dragY + amount.y).coerceIn(0f, maxY)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, outline),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarIcon(R.drawable.ic_close, stringResource(R.string.cancel), onClick = onCancel)
                Box {
                    ToolbarIcon(
                        R.drawable.ic_more_horiz,
                        stringResource(R.string.customization_more),
                        onClick = { overflowOpen = true }
                    )
                    DropdownMenu(
                        expanded = overflowOpen,
                        onDismissRequest = { overflowOpen = false }
                    ) {
                        OverflowItem(R.drawable.ic_presets, stringResource(R.string.presets_button)) {
                            overflowOpen = false; onPresets()
                        }
                        OverflowItem(R.drawable.ic_file_upload, stringResource(R.string.customization_import)) {
                            overflowOpen = false; onImport()
                        }
                        OverflowItem(R.drawable.ic_file_download, stringResource(R.string.customization_export)) {
                            overflowOpen = false; onExport()
                        }
                        HorizontalDivider(color = outline)
                        OverflowItem(
                            R.drawable.ic_reset,
                            stringResource(R.string.reset_layout_menu),
                            destructive = true
                        ) {
                            overflowOpen = false; onReset()
                        }
                    }
                }
                ToolbarIcon(
                    R.drawable.ic_undo,
                    stringResource(R.string.customization_undo),
                    enabled = canUndo,
                    onClick = onUndo
                )
                ToolbarIcon(
                    R.drawable.ic_redo,
                    stringResource(R.string.customization_redo),
                    enabled = canRedo,
                    onClick = onRedo
                )
                ToolbarIcon(
                    R.drawable.ic_vertical_align_top,
                    stringResource(R.string.customization_reset_toolbar),
                    enabled = dragX != 0f || dragY != 0f,
                    onClick = { dragX = 0f; dragY = 0f }
                )
                ToolbarDivider(outline)
                // Edit and Preview share one rounded container
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, outline),
                ) {
                    Row(modifier = Modifier.height(40.dp)) {
                        ModeSegment(
                            iconRes = R.drawable.ic_edit,
                            label = stringResource(R.string.customization_mode_edit),
                            selected = !previewing,
                            onClick = onSelectEdit
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(outline)
                        )
                        ModeSegment(
                            iconRes = R.drawable.ic_visibility,
                            label = stringResource(R.string.customization_preview_short),
                            selected = previewing,
                            onClick = onSelectPreview
                        )
                    }
                }
                ToolbarDivider(outline)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClick = onDone),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = stringResource(R.string.customization_done),
                        tint = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color.White
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        if (showHint) {
            Text(
                text = stringResource(R.string.customization_tap_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ToolbarDivider(color: Color) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(color)
    )
}

@Composable
private fun ToolbarIcon(iconRes: Int, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(MaterialTheme.shapes.small)
            .alpha(if (enabled) 1f else 0.35f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ModeSegment(iconRes: Int, label: String, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    Box(
        modifier = Modifier
            .width(44.dp)
            .fillMaxHeight()
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun OverflowItem(
    iconRes: Int,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    DropdownMenuItem(
        modifier = Modifier.heightIn(min = 44.dp),
        leadingIcon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        },
        text = { Text(label, color = color) },
        onClick = onClick
    )
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
    onLoad: (String, Map<ButtonComponent, ButtonConfig>) -> Unit,
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
                    onClick = { presets[target]?.let { onLoad(target, it) }; pendingLoad = null }
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
                                            presets[presetName]?.let { onLoad(presetName, it) }
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
    groups: List<LayoutGroup>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    val jsonString = remember(buttonConfigs, groups) { exportLayout(buttonConfigs, groups) }
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
    onImport: (Map<ButtonComponent, ButtonConfig>, List<LayoutGroup>?) -> Unit,
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
                        val layout = parseLayout(importedJsonText)
                        val sanitizedConfigs = sanitizeButtonConfigs(layout.buttons)
                        hasError = false
                        Log.i(logTag, "Imported and sanitized button configs: $sanitizedConfigs")
                        onImport(sanitizedConfigs, layout.groups)
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
