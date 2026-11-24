package io.github.horse3903.airdeck.ui.composables

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.horse3903.airdeck.R

/**
 * A dropdown list picker for selecting an item from a list.
 * @param modifier The modifier to be applied to the layout.
 * @param list List of items to choose from.
 * @param default Default item to display.
 * @param label Label to display above the picker.
 * @param isHorizontal If true, arranges label left of button (Row). If false, arranges label above button (Column).
 * @param formattedDisplay A composable function to format and display the item.
 */
@Composable
@Suppress("unused")
fun <T> ListItemPicker(
    modifier: Modifier = Modifier,
    list: Iterable<T>,
    selectedItem: T,
    label: String,
    isHorizontal: Boolean = false,
    showLabel: Boolean = true,
    buttonModifier: Modifier = Modifier,
    formattedDisplay: @Composable (T) -> Unit = { item ->
        Text(
            item.toString(), modifier = Modifier.width(IntrinsicSize.Max)
        )
    },
    onItemSelected: (T) -> Unit = { _ -> }
) {

    var expanded by rememberSaveable { mutableStateOf(false) }

    val labelContent = @Composable {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }

    val buttonContent = @Composable {
        val density = LocalDensity.current
        var buttonWidth by remember { mutableStateOf(0.dp) }
        Box {
            OutlinedButton(
                shape = MaterialTheme.shapes.small,
                modifier = buttonModifier.onSizeChanged {
                    buttonWidth = with(density) { it.width.toDp() }
                },
                onClick = { expanded = true }
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        formattedDisplay(selectedItem)
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_drop_down),
                        contentDescription = stringResource(R.string.content_desc_expand),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = 14.dp)
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.widthIn(min = buttonWidth)
            ) {
                list.forEach { item ->
                    DropdownMenuItem(text = { formattedDisplay(item) }, onClick = {
                        expanded = false
                        onItemSelected(item)
                    })
                }
            }
        }
    }

    if (isHorizontal) {
        Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labelContent()
            buttonContent()
        }
    } else {
        Column(
            modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (showLabel) labelContent()
            buttonContent()
        }
    }
}

/**
 * Overloaded function to accept default index instead of default item.
 * @see ListItemPicker
 */
@Composable
@Suppress("unused")
fun <T> ListItemPicker(
    modifier: Modifier = Modifier,
    list: Iterable<T>,
    defaultIndex: Int = 0,
    label: String,
    isHorizontal: Boolean = false,
    formattedDisplay: @Composable (T) -> Unit = { item -> Text(item.toString()) },
    onItemSelected: (T) -> Unit = { _ -> }
) {
    ListItemPicker(
        modifier = modifier,
        list = list,
        selectedItem = list.elementAt(defaultIndex),
        label = label,
        isHorizontal = isHorizontal,
        formattedDisplay = formattedDisplay,
        onItemSelected = onItemSelected
    )
}
