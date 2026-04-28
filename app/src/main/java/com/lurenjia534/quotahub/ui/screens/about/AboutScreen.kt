package com.lurenjia534.quotahub.ui.screens.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lurenjia534.quotahub.R
import com.lurenjia534.quotahub.ui.theme.QuotaHubTheme
import kotlinx.coroutines.delay

private const val AuthorGithubUrl = "https://github.com/lurenjia534"
private const val AppHomepageUrl = "https://github.com/lurenjia534/QuotaHub/"

@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var headerVisible by remember { mutableStateOf(false) }
    var identityVisible by remember { mutableStateOf(false) }
    var githubVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        headerVisible = true
        delay(80)
        identityVisible = true
        delay(80)
        githubVisible = true
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colorScheme.surfaceContainerLowest,
                        colorScheme.secondaryContainer.copy(alpha = 0.18f),
                        colorScheme.surfaceContainerLowest
                    )
                )
            )
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = 14.dp,
            end = 18.dp,
            bottom = 34.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            AnimatedAboutSection(visible = headerVisible) {
                AboutTopBar(onBackClick = onBackClick)
            }
        }

        item {
            AnimatedAboutSection(visible = identityVisible) {
                AppIdentityPanel()
            }
        }

        item {
            AnimatedAboutSection(visible = githubVisible) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AboutLinkRow(
                        title = "Author GitHub",
                        urlText = "github.com/lurenjia534",
                        url = AuthorGithubUrl
                    )
                    AboutLinkRow(
                        title = "App GitHub",
                        urlText = "github.com/lurenjia534/QuotaHub",
                        url = AppHomepageUrl
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedAboutSection(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = spring(stiffness = 420f, dampingRatio = 0.92f)
        ) + slideInVertically(
            animationSpec = spring(stiffness = 420f, dampingRatio = 0.92f),
            initialOffsetY = { it / 7 }
        )
    ) {
        content()
    }
}

@Composable
private fun AboutTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f)
            )
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
        Text(
            text = "About",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun AppIdentityPanel() {
    val colorScheme = MaterialTheme.colorScheme
    var iconSettled by remember { mutableStateOf(false) }
    val iconScale by animateFloatAsState(
        targetValue = if (iconSettled) 1f else 0.9f,
        animationSpec = spring(stiffness = 300f, dampingRatio = 0.74f),
        label = "aboutIconScale"
    )

    LaunchedEffect(Unit) {
        iconSettled = true
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = 42.dp,
            topEnd = 26.dp,
            bottomStart = 30.dp,
            bottomEnd = 46.dp
        ),
        color = colorScheme.primaryContainer.copy(alpha = 0.66f),
        contentColor = colorScheme.onPrimaryContainer,
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.18f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(104.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                shape = RoundedCornerShape(
                    topStart = 30.dp,
                    topEnd = 22.dp,
                    bottomStart = 22.dp,
                    bottomEnd = 34.dp
                ),
                color = colorScheme.surface,
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.18f)),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.codex_color),
                        contentDescription = "QuotaHub app icon",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "QuotaHub",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = colorScheme.primary
                    )
                    Text(
                        text = "lurenjia534",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutLinkRow(
    title: String,
    urlText: String,
    url: String
) {
    val uriHandler = LocalUriHandler.current
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(
        topStart = 26.dp,
        topEnd = 34.dp,
        bottomStart = 34.dp,
        bottomEnd = 22.dp
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(shape)
            .clickable { uriHandler.openUri(url) },
        shape = shape,
        color = colorScheme.surfaceContainerLow,
        contentColor = colorScheme.onSurface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Code,
                contentDescription = null,
                tint = colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = urlText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colorScheme.onSurfaceVariant
                    )
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    QuotaHubTheme {
        AboutScreen(onBackClick = {})
    }
}
