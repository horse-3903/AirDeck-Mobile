package io.github.horse3903.airdeck.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import io.github.horse3903.airdeck.R

private val DangerRed = Color(0xFFD03030)

/** Same unplug mark as the browser controller: a plug with a slash through it. */
@Composable
private fun UnplugIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier) {
        scale(size.width / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawPath(PathParser().parsePathString("M9 3v4M15 3v4M7 7h10v4a5 5 0 0 1-10 0V7zM12 16v5").toPath(), color, style = stroke)
            drawPath(PathParser().parsePathString("M3 3l18 18").toPath(), color, style = stroke)
        }
    }
}

/** The browser controller's disconnect confirmation: icon, title, text and two equal buttons. */
@Composable
fun DisconnectDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0.5f) }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .widthIn(min = 300.dp, max = 340.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DangerRed.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    UnplugIcon(Modifier.size(22.dp), DangerRed)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.disconnect_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.disconnect_message),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val buttonShape = RoundedCornerShape(12.dp)
                    val buttonModifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                    OutlinedButton(
                        onClick = onCancel,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = buttonShape,
                        modifier = buttonModifier
                    ) {
                        Text(stringResource(R.string.leave_gamepad_stay), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = onConfirm,
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                        modifier = buttonModifier
                    ) {
                        Text(stringResource(R.string.disconnect_confirm), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}
