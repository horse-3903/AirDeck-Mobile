package io.github.horse3903.airdeck.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.horse3903.airdeck.R
import io.github.horse3903.airdeck.data.PreviewBase
import io.github.horse3903.airdeck.data.PreviewHeightDp
import io.github.horse3903.airdeck.data.PreviewWidthDp
import io.github.horse3903.airdeck.network.ConnectionState

@Composable
fun MainMenu(
    connection: ConnectionState = ConnectionState(),
    onReturnToGamepad: () -> Unit = {},
    onNavigateToConnectScreen: () -> Unit,
    onNavigateToSettingsScreen: () -> Unit,
    onNavigateToAboutScreen: () -> Unit
) {
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(
                        if (MaterialTheme.colorScheme.background.luminance() > 0.5f) R.drawable.hero_controller_light
                        else R.drawable.hero_controller
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .widthIn(max = 200.dp)
                        .fillMaxWidth(0.6f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.menu_title),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.menu_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                if (connection.connected) {
                    Text(
                        text = connection.controllerSlot?.let {
                            stringResource(R.string.menu_status_connected, connection.ipAddress, it)
                        } ?: stringResource(R.string.menu_status_connected_plain, connection.ipAddress),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = if (connection.connected) onReturnToGamepad else onNavigateToConnectScreen,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                ) {
                    Text(
                        text = if (connection.connected) {
                            stringResource(R.string.menu_return)
                        } else {
                            stringResource(R.string.menu_start)
                        }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToSettingsScreen,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(text = stringResource(R.string.menu_settings))
                    }
                    OutlinedButton(
                        onClick = onNavigateToAboutScreen,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(text = stringResource(R.string.menu_about))
                    }
                }
            }
        }
    }
}

@Preview(
    widthDp = PreviewWidthDp,
    heightDp = PreviewHeightDp,
)
@Composable
fun MainMenuPreview() {
    PreviewBase {
        MainMenu(
            onNavigateToConnectScreen = {},
            onNavigateToSettingsScreen = {},
            onNavigateToAboutScreen = {}
        )
    }
}
