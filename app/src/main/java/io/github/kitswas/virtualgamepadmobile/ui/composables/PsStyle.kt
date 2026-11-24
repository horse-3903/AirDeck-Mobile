package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.kitswas.virtualgamepadmobile.data.GamepadStyle

val LocalGamepadStyle = staticCompositionLocalOf { GamepadStyle.PLAYSTATION }

/** True when touching anywhere inside a stick's circle moves the stick there. */
val LocalStickSnap = staticCompositionLocalOf { true }

/** Gamepad palette that follows the app theme (dark or light). */
internal object PsStyle {
    private val isLight: Boolean
        @Composable get() = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val Body: Color @Composable get() = if (isLight) Color(0xFFF1F1F4) else Color(0xFF17171B)
    val BodyPressed: Color @Composable get() = if (isLight) Color(0xFFD9D9E0) else Color(0xFF34343C)
    val Border: Color @Composable get() = if (isLight) Color(0xFFC9C9D2) else Color(0xFF34343C)
    val BorderPressed: Color @Composable get() = if (isLight) Color(0xFF5E5E6A) else Color(0xFF9A9AA6)
    val Glyph: Color @Composable get() = if (isLight) Color(0xFF1F1F24) else Color(0xFFE6E6EA)
    val StickBase: Color @Composable get() = if (isLight) Color(0xFFE7E7EC) else Color(0xFF101013)
    val StickRing: Color @Composable get() = if (isLight) Color(0xFFC4C4CC) else Color(0xFF2C2C33)
    val StickCap: Color @Composable get() = if (isLight) Color(0xFFB9B9C2) else Color(0xFF2E2E35)
    val StickCapInner: Color @Composable get() = if (isLight) Color(0xFFD3D3DB) else Color(0xFF202026)
    val Cross: Color @Composable get() = if (isLight) Color(0xFF2F6FE0) else Color(0xFF6AA2FF)
    val Circle: Color @Composable get() = if (isLight) Color(0xFFE0394B) else Color(0xFFFF6B7A)
    val Square: Color @Composable get() = if (isLight) Color(0xFFC24FA8) else Color(0xFFE58AD1)
    val Triangle: Color @Composable get() = if (isLight) Color(0xFF1FA67A) else Color(0xFF5FD2A0)
    val XboxA: Color @Composable get() = if (isLight) Color(0xFF2E9E3C) else Color(0xFF6BCB5B)
    val XboxB: Color @Composable get() = if (isLight) Color(0xFFD0362C) else Color(0xFFE5534B)
    val XboxX: Color @Composable get() = if (isLight) Color(0xFF2F6FE0) else Color(0xFF4C8DF6)
    val XboxY: Color @Composable get() = if (isLight) Color(0xFFC99A12) else Color(0xFFF2C94C)
}
