package com.lurenjia534.quotahub.ui.screens.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lurenjia534.quotahub.data.preferences.ThemeColorSource
import com.lurenjia534.quotahub.data.preferences.ThemePalette
import com.lurenjia534.quotahub.ui.components.rememberQuotaHaptics
import com.lurenjia534.quotahub.ui.theme.QuotaHubTheme
import com.lurenjia534.quotahub.ui.theme.appThemePalettePreviewColors
import kotlinx.coroutines.delay

private data class PaletteSourceOption(
    val source: ThemeColorSource,
    val label: String
)

private val PaletteSourceOptions = listOf(
    PaletteSourceOption(
        source = ThemeColorSource.System,
        label = "System"
    ),
    PaletteSourceOption(
        source = ThemeColorSource.AppPalette,
        label = "App"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPaletteScreen(
    themeColorSource: ThemeColorSource,
    themePalette: ThemePalette,
    hapticConfirmation: Boolean,
    onThemeColorSourceChange: (ThemeColorSource) -> Unit,
    onThemePaletteChange: (ThemePalette) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val quotaHaptics = rememberQuotaHaptics(hapticConfirmation)
    val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var introVisible by remember { mutableStateOf(false) }
    var sourceVisible by remember { mutableStateOf(false) }
    var previewVisible by remember { mutableStateOf(false) }
    var paletteVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        introVisible = true
        delay(70)
        sourceVisible = true
        delay(70)
        previewVisible = true
        delay(70)
        paletteVisible = true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colorScheme.surfaceContainerLowest,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "App palette",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surfaceContainerLowest,
                    navigationIconContentColor = colorScheme.onSurface,
                    titleContentColor = colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 18.dp,
                top = 10.dp,
                end = 18.dp,
                bottom = 34.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                PaletteAnimatedSection(visible = introVisible) {
                    PaletteIntro(
                        themeColorSource = themeColorSource,
                        themePalette = themePalette,
                        dynamicColorAvailable = dynamicColorAvailable
                    )
                }
            }

            item {
                PaletteAnimatedSection(visible = sourceVisible) {
                    SourceSection(
                        themeColorSource = themeColorSource,
                        themePalette = themePalette,
                        dynamicColorAvailable = dynamicColorAvailable,
                        onSourceSelected = { source ->
                            if (source != themeColorSource) {
                                onThemeColorSourceChange(source)
                                quotaHaptics.toggle(source == ThemeColorSource.AppPalette)
                            }
                        }
                    )
                }
            }

            item {
                PaletteAnimatedSection(visible = previewVisible) {
                    ActiveRolesPreview(
                        themeColorSource = themeColorSource,
                        themePalette = themePalette,
                        dynamicColorAvailable = dynamicColorAvailable
                    )
                }
            }

            item {
                PaletteAnimatedSection(visible = paletteVisible) {
                    PaletteList(
                        title = if (themeColorSource == ThemeColorSource.System) {
                            "Fallback palette"
                        } else {
                            "App palette"
                        },
                        selectedPalette = themePalette,
                        onPaletteSelected = { palette ->
                            if (palette != themePalette) {
                                onThemePaletteChange(palette)
                                quotaHaptics.toggle(true)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteAnimatedSection(
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
private fun PaletteIntro(
    themeColorSource: ThemeColorSource,
    themePalette: ThemePalette,
    dynamicColorAvailable: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val body = when {
        themeColorSource == ThemeColorSource.AppPalette ->
            "${themePalette.title} is filling QuotaHub's Material color roles."
        dynamicColorAvailable ->
            "System dynamic color is active. ${themePalette.title} remains the fallback."
        else ->
            "This device cannot provide dynamic color, so ${themePalette.title} is the fallback."
    }

    Surface(
        color = colorScheme.surfaceContainerLow,
        contentColor = colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.16f)),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Palette,
                            contentDescription = null
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Material color roles",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoleSwatch(
                    label = "Primary",
                    color = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
                RoleSwatch(
                    label = "Secondary",
                    color = colorScheme.secondary,
                    contentColor = colorScheme.onSecondary,
                    modifier = Modifier.weight(1f)
                )
                RoleSwatch(
                    label = "Tertiary",
                    color = colorScheme.tertiary,
                    contentColor = colorScheme.onTertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SourceSection(
    themeColorSource: ThemeColorSource,
    themePalette: ThemePalette,
    dynamicColorAvailable: Boolean,
    onSourceSelected: (ThemeColorSource) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(
            title = "Color source",
            body = "Choose whether QuotaHub follows Android dynamic color or uses its own palette."
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            PaletteSourceOptions.forEachIndexed { index, option ->
                SegmentedButton(
                    modifier = Modifier.weight(1f),
                    selected = themeColorSource == option.source,
                    onClick = { onSourceSelected(option.source) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = PaletteSourceOptions.size
                    ),
                    label = { Text(option.label) }
                )
            }
        }
        SourceStatus(
            themeColorSource = themeColorSource,
            themePalette = themePalette,
            dynamicColorAvailable = dynamicColorAvailable
        )
    }
}

@Composable
private fun SourceStatus(
    themeColorSource: ThemeColorSource,
    themePalette: ThemePalette,
    dynamicColorAvailable: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val status = when {
        themeColorSource == ThemeColorSource.AppPalette ->
            "${themePalette.title} is applied consistently across supported Android versions."
        dynamicColorAvailable ->
            "Android 12+ dynamic color is active; ${themePalette.title} is used only as fallback."
        else ->
            "Dynamic color is unavailable on this device; ${themePalette.title} is active as fallback."
    }

    Surface(
        color = colorScheme.secondaryContainer.copy(alpha = 0.58f),
        contentColor = colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colorScheme.onSecondaryContainer.copy(alpha = 0.86f)
                )
            )
        }
    }
}

@Composable
private fun ActiveRolesPreview(
    themeColorSource: ThemeColorSource,
    themePalette: ThemePalette,
    dynamicColorAvailable: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val previewBody = when {
        themeColorSource == ThemeColorSource.AppPalette ->
            "These roles update immediately from ${themePalette.title}."
        dynamicColorAvailable ->
            "These roles currently come from Android dynamic color."
        else ->
            "These roles come from ${themePalette.title} because dynamic color is unavailable."
    }

    Surface(
        color = colorScheme.surfaceContainer,
        contentColor = colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionTitle(
                title = "Active roles",
                body = previewBody
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoleSwatch(
                        label = "Primary",
                        color = colorScheme.primary,
                        contentColor = colorScheme.onPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    RoleSwatch(
                        label = "On primary",
                        color = colorScheme.onPrimary,
                        contentColor = colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContainerRoleSwatch(
                        label = "Primary container",
                        color = colorScheme.primaryContainer,
                        contentColor = colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    ContainerRoleSwatch(
                        label = "Surface high",
                        color = colorScheme.surfaceContainerHigh,
                        contentColor = colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteList(
    title: String,
    selectedPalette: ThemePalette,
    onPaletteSelected: (ThemePalette) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(
            title = title,
            body = "Each option provides paired light and dark ColorScheme roles."
        )
        ThemePalette.entries.forEach { palette ->
            PaletteOption(
                palette = palette,
                selected = selectedPalette == palette,
                onClick = { onPaletteSelected(palette) }
            )
        }
    }
}

@Composable
private fun PaletteOption(
    palette: ThemePalette,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (selected) colorScheme.primaryContainer else colorScheme.surfaceContainerLow,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "paletteOptionContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "paletteOptionContent"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.985f,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.86f),
        label = "paletteOptionScale"
    )
    val shape = if (selected) {
        MaterialTheme.shapes.extraLarge
    } else {
        MaterialTheme.shapes.large
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        color = containerColor,
        contentColor = contentColor,
        shape = shape,
        border = BorderStroke(
            1.dp,
            if (selected) colorScheme.primary.copy(alpha = 0.42f) else colorScheme.outlineVariant.copy(alpha = 0.14f)
        ),
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                PaletteSwatches(
                    colors = appThemePalettePreviewColors(palette),
                    swatchSize = 28.dp
                )
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = palette.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = palette.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = contentColor.copy(alpha = 0.78f)
                        )
                    )
                }
            }
            RadioButton(
                selected = selected,
                onClick = null
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    body: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun PaletteSwatches(
    colors: List<Color>,
    swatchSize: Dp
) {
    val outline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        colors.forEach { color ->
            Surface(
                modifier = Modifier.size(swatchSize),
                color = color,
                shape = CircleShape,
                border = BorderStroke(1.dp, outline)
            ) {}
        }
    }
}

@Composable
private fun RoleSwatch(
    label: String,
    color: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun ContainerRoleSwatch(
    label: String,
    color: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.14f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppPaletteScreenPreview() {
    QuotaHubTheme(
        themeColorSource = ThemeColorSource.AppPalette,
        themePalette = ThemePalette.Grove
    ) {
        AppPaletteScreen(
            themeColorSource = ThemeColorSource.AppPalette,
            themePalette = ThemePalette.Grove,
            hapticConfirmation = false,
            onThemeColorSourceChange = {},
            onThemePaletteChange = {},
            onBackClick = {}
        )
    }
}
