package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.kitswas.virtualgamepadmobile.R

/**
 * A small confirmation shown when leaving the gamepad: keep the connection alive in the
 * background, or disconnect from the PC.
 */
@Composable
fun LeaveDialog(
    onCancel: () -> Unit,
    onKeepConnected: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val compactPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.widthIn(max = 360.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = stringResource(R.string.leave_gamepad_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.leave_gamepad_message),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.padding(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onCancel,
                        shape = MaterialTheme.shapes.small,
                        contentPadding = compactPadding,
                        modifier = Modifier.height(36.dp)
                    ) { Text(stringResource(R.string.leave_gamepad_stay), fontSize = 13.sp) }
                    Button(
                        onClick = onKeepConnected,
                        shape = MaterialTheme.shapes.small,
                        contentPadding = compactPadding,
                        modifier = Modifier.height(36.dp)
                    ) { Text(stringResource(R.string.leave_gamepad_keep), fontSize = 13.sp) }
                    Button(
                        onClick = onDisconnect,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = MaterialTheme.shapes.small,
                        contentPadding = compactPadding,
                        modifier = Modifier.height(36.dp)
                    ) { Text(stringResource(R.string.leave_gamepad_confirm), fontSize = 13.sp) }
                }
            }
        }
    }
}
