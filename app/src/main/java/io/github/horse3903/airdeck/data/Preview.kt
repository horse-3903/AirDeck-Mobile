package io.github.horse3903.airdeck.data

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import io.github.horse3903.airdeck.ui.theme.AirDeckTheme

// The width and height of the preview surface in dp
const val PreviewWidthDp = 960
const val PreviewHeightDp = 540

@Composable
fun PreviewBase(content: @Composable () -> Unit) {
    AirDeckTheme(darkMode = defaultColorScheme, baseColor = defaultBaseColor) {
        Surface(color = MaterialTheme.colorScheme.background) { content() }
    }
}
