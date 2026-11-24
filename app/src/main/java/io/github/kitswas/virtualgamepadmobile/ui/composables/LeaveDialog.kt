package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import io.github.kitswas.virtualgamepadmobile.R

/**
 * Confirmation shown when leaving the controller: go back to the home screen while staying
 * connected, or disconnect from the PC. Labels never wrap and the dialog is wide enough for
 * all three actions.
 */
@Composable
fun LeaveDialog(
    onCancel: () -> Unit,
    onKeepConnected: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val compactPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp)
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Keep the controller visible behind the dialog
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0.32f) }

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .widthIn(min = 440.dp, max = 520.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                Text(
                    text = stringResource(R.string.leave_gamepad_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.leave_gamepad_message),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = MaterialTheme.shapes.small,
                        contentPadding = compactPadding,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            stringResource(R.string.leave_gamepad_stay),
                            fontSize = 15.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    OutlinedButton(
                        onClick = onDisconnect,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        contentPadding = compactPadding,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            stringResource(R.string.leave_gamepad_confirm),
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                    }
                    Button(
                        onClick = onKeepConnected,
                        shape = MaterialTheme.shapes.small,
                        contentPadding = compactPadding,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            stringResource(R.string.leave_gamepad_keep),
                            fontSize = 15.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
