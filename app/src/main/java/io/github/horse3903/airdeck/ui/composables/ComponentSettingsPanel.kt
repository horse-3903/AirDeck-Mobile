package io.github.horse3903.airdeck.ui.composables

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.clickable
import io.github.horse3903.airdeck.data.TriggerMode
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.horse3903.airdeck.R
import io.github.horse3903.airdeck.data.ButtonAnchor
import io.github.horse3903.airdeck.data.ButtonComponent
import io.github.horse3903.airdeck.data.ButtonConfig
import io.github.horse3903.airdeck.data.OFFSET_VALUE_RANGE
import io.github.horse3903.airdeck.data.SCALE_VALUE_RANGE
import io.github.horse3903.airdeck.data.STRETCH_VALUE_RANGE
import androidx.compose.material3.ExperimentalMaterial3Api
import java.util.Locale

@Composable
private fun SettingsRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        content()
    }
}

/**
 * A titled slider with a value readout, a conventional round thumb, labelled ends and a reset action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledSlider(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    startLabel: String,
    centerLabel: String?,
    endLabel: String,
    defaultValue: Float,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = valueText, style = MaterialTheme.typography.bodyMedium)
                TextButton(
                    shape = MaterialTheme.shapes.small,
                    onClick = { onValueChange(defaultValue) },
                    enabled = value != defaultValue
                ) {
                    Text(
                        stringResource(R.string.panel_reset),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        val interactionSource = remember { MutableInteractionSource() }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    thumbSize = DpSize(20.dp, 20.dp)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(6.dp),
                    thumbTrackGapSize = 0.dp,
                    drawStopIndicator = null
                )
            },
            modifier = Modifier.height(32.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val small = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
            val muted = MaterialTheme.colorScheme.onSurfaceVariant
            Text(startLabel, style = small, color = muted)
            if (centerLabel != null) Text(centerLabel, style = small, color = muted)
            Text(endLabel, style = small, color = muted)
        }
    }
}

/**
 * Side panel with the settings of one gamepad component.
 */
@Composable
fun ComponentSettingsPanel(
    component: ButtonComponent,
    config: ButtonConfig,
    onConfigChange: (ButtonConfig) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    groupChoices: List<GroupChoice> = emptyList(),
    currentGroup: GroupChoice? = null,
    onGroupChange: (GroupChoice) -> Unit = {},
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = stringResource(component.nameRes),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsRow(stringResource(R.string.button_visible)) {
                Switch(
                    checked = config.visible,
                    onCheckedChange = { onConfigChange(config.copy(visible = it)) }
                )
            }
            SettingsRow(stringResource(R.string.panel_anchor)) {
                ListItemPicker(
                    list = ButtonAnchor.entries.asIterable(),
                    selectedItem = config.anchor,
                    label = "",
                    showLabel = false,
                    buttonModifier = Modifier
                        .width(168.dp)
                        .height(40.dp),
                    formattedDisplay = { item -> Text(text = stringResource(item.nameRes)) },
                    onItemSelected = { onConfigChange(config.copy(anchor = it)) }
                )
            }
            if (currentGroup != null) {
                SettingsRow(stringResource(R.string.panel_group)) {
                    ListItemPicker(
                        list = groupChoices,
                        selectedItem = currentGroup,
                        label = "",
                        showLabel = false,
                        buttonModifier = Modifier
                            .width(168.dp)
                            .height(40.dp),
                        formattedDisplay = { item -> Text(text = item.label) },
                        onItemSelected = onGroupChange
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (component == ButtonComponent.LEFT_TRIGGER || component == ButtonComponent.RIGHT_TRIGGER) {
                Text(
                    text = stringResource(R.string.panel_trigger_mode),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                TriggerMode.offered.forEach { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfigChange(config.copy(triggerMode = m)) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = config.triggerMode == m,
                            onClick = { onConfigChange(config.copy(triggerMode = m)) }
                        )
                        Column {
                            Text(stringResource(m.nameRes), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stringResource(m.descRes),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            LabeledSlider(
                title = stringResource(R.string.panel_size),
                valueText = "${(config.scale * 100).toInt()}%",
                value = config.scale,
                range = SCALE_VALUE_RANGE,
                startLabel = "${(SCALE_VALUE_RANGE.start * 100).toInt()}%",
                centerLabel = null,
                endLabel = "${(SCALE_VALUE_RANGE.endInclusive * 100).toInt()}%",
                defaultValue = 1f,
                onValueChange = { onConfigChange(config.copy(scale = it)) }
            )
            LabeledSlider(
                title = stringResource(R.string.panel_width),
                valueText = "${(config.widthScale * 100).toInt()}%",
                value = config.widthScale,
                range = STRETCH_VALUE_RANGE,
                startLabel = "${(STRETCH_VALUE_RANGE.start * 100).toInt()}%",
                centerLabel = null,
                endLabel = "${(STRETCH_VALUE_RANGE.endInclusive * 100).toInt()}%",
                defaultValue = 1f,
                onValueChange = { onConfigChange(config.copy(widthScale = it)) }
            )
            LabeledSlider(
                title = stringResource(R.string.panel_height),
                valueText = "${(config.heightScale * 100).toInt()}%",
                value = config.heightScale,
                range = STRETCH_VALUE_RANGE,
                startLabel = "${(STRETCH_VALUE_RANGE.start * 100).toInt()}%",
                centerLabel = null,
                endLabel = "${(STRETCH_VALUE_RANGE.endInclusive * 100).toInt()}%",
                defaultValue = 1f,
                onValueChange = { onConfigChange(config.copy(heightScale = it)) }
            )
            LabeledSlider(
                title = stringResource(R.string.panel_horizontal),
                valueText = String.format(Locale.US, "%.2f", config.offsetX),
                value = config.offsetX,
                range = OFFSET_VALUE_RANGE,
                startLabel = stringResource(R.string.panel_left),
                centerLabel = stringResource(R.string.panel_center),
                endLabel = stringResource(R.string.panel_right),
                defaultValue = 0f,
                onValueChange = { onConfigChange(config.copy(offsetX = it)) }
            )
            LabeledSlider(
                title = stringResource(R.string.panel_vertical),
                valueText = String.format(Locale.US, "%.2f", config.offsetY),
                value = config.offsetY,
                range = OFFSET_VALUE_RANGE,
                startLabel = stringResource(R.string.panel_up),
                centerLabel = stringResource(R.string.panel_center),
                endLabel = stringResource(R.string.panel_down),
                defaultValue = 0f,
                onValueChange = { onConfigChange(config.copy(offsetY = it)) }
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Button(
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
            onClick = onDone
        ) {
            Text(stringResource(R.string.customization_done))
        }
    }
}

/** One entry of a control's group picker. A null [id] means no group, [NEW_GROUP_ID] starts a new one. */
data class GroupChoice(val id: String?, val label: String)

const val NEW_GROUP_ID = "__new"

/**
 * Side panel for a selected group: show or hide it as a whole, jump to one of its controls, or break it up.
 */
@Composable
fun GroupSettingsPanel(
    title: String,
    members: List<Pair<ButtonComponent, String>>,
    allVisible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    onSelectMember: (ButtonComponent) -> Unit,
    onUngroup: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.group_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SettingsRow(stringResource(R.string.button_visible)) {
                Switch(checked = allVisible, onCheckedChange = onVisibleChange)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = stringResource(R.string.group_members),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            members.forEach { (component, label) ->
                OutlinedButton(
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    onClick = { onSelectMember(component) }
                ) { Text(label) }
            }
            TextButton(
                shape = MaterialTheme.shapes.small,
                onClick = onUngroup
            ) {
                Text(
                    stringResource(R.string.group_ungroup),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Button(
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
            onClick = onDone
        ) {
            Text(stringResource(R.string.customization_done))
        }
    }
}
