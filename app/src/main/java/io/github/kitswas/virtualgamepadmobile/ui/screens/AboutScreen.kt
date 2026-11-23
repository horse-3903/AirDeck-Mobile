package io.github.kitswas.virtualgamepadmobile.ui.screens

import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.kitswas.virtualgamepadmobile.R
import io.github.kitswas.virtualgamepadmobile.data.PreviewBase
import io.github.kitswas.virtualgamepadmobile.data.PreviewHeightDp
import io.github.kitswas.virtualgamepadmobile.data.PreviewWidthDp

@Composable
fun AboutScreen(
    versionName: String? = LocalContext.current.packageManager.getPackageInfo(
        LocalContext.current.packageName, 0
    ).versionName,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // GitHub URLs
    val projectUrl = "https://kitswas.github.io/VirtualGamePad/"
    val mobileRepoUrl = "https://github.com/kitswas/VirtualGamePad-Mobile/"
    val mobileLicenseUrl = "https://github.com/kitswas/VirtualGamePad-Mobile/blob/main/LICENCE.TXT"
    val issuesUrl = "https://github.com/kitswas/VirtualGamePad-Mobile/issues/new"
    val releaseUrl = "https://github.com/kitswas/VirtualGamePad-Mobile/releases/latest"
    val serverDownloadUrl = "https://kitswas.github.io/VirtualGamePad/#installation"

    fun open(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.about_title),
                    style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.about_version, versionName ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.about_description),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val buttonWidth = (maxWidth - 16.dp) / 3
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            LinkButton(R.string.about_source_code, Modifier.width(buttonWidth)) { open(mobileRepoUrl) }
                            LinkButton(R.string.about_project_website, Modifier.width(buttonWidth)) { open(projectUrl) }
                            LinkButton(R.string.about_report_issues, Modifier.width(buttonWidth)) { open(issuesUrl) }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            LinkButton(R.string.about_view_license, Modifier.width(buttonWidth)) { open(mobileLicenseUrl) }
                            LinkButton(R.string.about_latest_release, Modifier.width(buttonWidth)) { open(releaseUrl) }
                            LinkButton(R.string.connect_download_server, Modifier.width(buttonWidth)) { open(serverDownloadUrl) }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    shape = MaterialTheme.shapes.small,
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .width(160.dp)
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.back))
                }
            }
        }
    }
}

@Composable
private fun LinkButton(
    @StringRes label: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = modifier.height(44.dp),
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = 8.dp),
        onClick = onClick
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            painter = painterResource(R.drawable.ic_open_in_new),
            contentDescription = null,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Preview(
    widthDp = PreviewWidthDp,
    heightDp = PreviewHeightDp,
)
@Composable
fun AboutScreenPreview() {
    PreviewBase {
        AboutScreen(onNavigateBack = {}, versionName = stringResource(R.string.about_dev_version))
    }
}
