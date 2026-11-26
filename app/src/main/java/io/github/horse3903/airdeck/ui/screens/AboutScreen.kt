package io.github.horse3903.airdeck.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import io.github.horse3903.airdeck.R
import io.github.horse3903.airdeck.data.PreviewBase
import io.github.horse3903.airdeck.data.PreviewHeightDp
import io.github.horse3903.airdeck.data.PreviewWidthDp

@Composable
fun AboutScreen(
    versionName: String? = LocalContext.current.packageManager.getPackageInfo(
        LocalContext.current.packageName, 0
    ).versionName,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val projectUrl = "https://horse-3903.github.io/showcase/airdeck/"
    val mobileRepoUrl = "https://github.com/horse-3903/AirDeck-Mobile/"
    val mobileLicenseUrl = "https://github.com/horse-3903/AirDeck-Mobile/blob/modern-ui/LICENCE.TXT"
    val issuesUrl = "https://github.com/horse-3903/AirDeck-Mobile/issues/new"
    val releaseUrl = "https://github.com/horse-3903/AirDeck-Mobile/releases/latest"
    val serverDownloadUrl = "https://github.com/horse-3903/AirDeck-PC/releases/latest"
    val upstreamUrl = "https://github.com/kitswas/VirtualGamePad"

    fun open(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    val version = stringResource(R.string.about_version, versionName ?: "")
    val copiedMessage = stringResource(R.string.about_version_copied)

    val branding: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_gamepad),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.about_app_name),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = version,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("AirDeck version", version))
                        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.about_attribution),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable { open(upstreamUrl) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    val links: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier = modifier) {
            SectionLabel(R.string.about_section_resources)
            LinkRow(R.drawable.ic_lucide_monitor_down, R.string.about_download_server) { open(serverDownloadUrl) }
            LinkRow(R.drawable.ic_lucide_package, R.string.about_latest_release) { open(releaseUrl) }
            LinkRow(R.drawable.ic_lucide_globe, R.string.about_project_website) { open(projectUrl) }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            SectionLabel(R.string.about_section_source)
            LinkRow(R.drawable.ic_lucide_github, R.string.about_source_code) { open(mobileRepoUrl) }
            LinkRow(R.drawable.ic_lucide_bug, R.string.about_report_issues) { open(issuesUrl) }
            LinkRow(R.drawable.ic_lucide_scale, R.string.about_view_license) { open(mobileLicenseUrl) }
        }
    }

    Scaffold { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val wide = maxWidth >= 600.dp
            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 56.dp, end = 32.dp, top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(0.45f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        branding(Modifier.padding(horizontal = 8.dp))
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .width(1.dp)
                            .fillMaxHeight(0.7f)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Box(
                        modifier = Modifier.weight(0.55f).fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Centred when it fits, scrolls when it does not
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            links(Modifier.fillMaxWidth())
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    branding(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(24.dp))
                    links(Modifier.fillMaxWidth())
                }
            }
            IconButton(onClick = onNavigateBack, modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(@StringRes label: Int) {
    Text(
        text = stringResource(label).uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, letterSpacing = 0.8.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun LinkRow(@DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(R.drawable.ic_open_in_new),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp)
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
