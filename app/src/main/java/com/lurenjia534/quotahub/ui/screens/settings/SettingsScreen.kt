package com.lurenjia534.quotahub.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lurenjia534.quotahub.ui.components.rememberQuotaHaptics
import com.lurenjia534.quotahub.ui.theme.QuotaHubTheme
import kotlinx.coroutines.delay

private enum class RefreshProfile(
    val title: String,
    val caption: String,
    val summary: String,
    val icon: ImageVector
) {
    Live(
        title = "Live",
        caption = "15 min",
        summary = "Checks quota frequently and keeps fast-moving providers visible.",
        icon = Icons.Outlined.Bolt
    ),
    Balanced(
        title = "Balanced",
        caption = "Hourly",
        summary = "Uses the recommended rhythm for a cleaner dashboard and lighter battery use.",
        icon = Icons.Outlined.Update
    ),
    Manual(
        title = "Manual",
        caption = "Pull only",
        summary = "Refreshes only when you open details or explicitly ask for an update.",
        icon = Icons.Outlined.TouchApp
    )
}

@Composable
fun SettingsScreen(
    highEmphasisMetrics: Boolean,
    hapticConfirmation: Boolean,
    onHighEmphasisMetricsChange: (Boolean) -> Unit,
    onHapticConfirmationChange: (Boolean) -> Unit,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    var dynamicPaletteEnabled by rememberSaveable { mutableStateOf(true) }
    var usageAlerts by rememberSaveable { mutableStateOf(true) }
    var lowBalanceBanner by rememberSaveable { mutableStateOf(true) }
    var privacyShield by rememberSaveable { mutableStateOf(true) }
    var refreshProfile by rememberSaveable { mutableStateOf(RefreshProfile.Balanced) }
    val quotaHaptics = rememberQuotaHaptics(hapticConfirmation)

    var headerVisible by remember { mutableStateOf(false) }
    var displayVisible by remember { mutableStateOf(false) }
    var refreshVisible by remember { mutableStateOf(false) }
    var awarenessVisible by remember { mutableStateOf(false) }
    var transparencyVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        headerVisible = true
        delay(70)
        displayVisible = true
        delay(70)
        refreshVisible = true
        delay(70)
        awarenessVisible = true
        delay(70)
        transparencyVisible = true
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = 22.dp,
            end = 18.dp,
            bottom = 34.dp + bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            AnimatedSection(visible = headerVisible) {
                SettingsControlHeader(
                    refreshProfile = refreshProfile,
                    dynamicPaletteEnabled = dynamicPaletteEnabled,
                    usageAlerts = usageAlerts,
                    privacyShield = privacyShield
                )
            }
        }

        item {
            AnimatedSection(visible = displayVisible) {
                SettingsControlSection(
                    index = 0,
                    icon = Icons.Outlined.Palette,
                    title = "Display signal",
                    subtitle = "Tune how quota state reads before any provider-specific detail."
                ) {
                    ToggleControlRow(
                        icon = Icons.Outlined.Palette,
                        title = "Wallpaper palette",
                        description = "Borrow system-derived tones so provider accents feel native to the device.",
                        checked = dynamicPaletteEnabled,
                        onCheckedChange = { dynamicPaletteEnabled = it },
                        onAfterCheckedChange = { checked ->
                            quotaHaptics.toggle(checked)
                        }
                    )
                    ToggleControlRow(
                        icon = Icons.Outlined.Visibility,
                        title = "High-emphasis metrics",
                        description = "Use stronger type contrast for remaining quota, reset dates, and low balances.",
                        checked = highEmphasisMetrics,
                        onCheckedChange = onHighEmphasisMetricsChange,
                        onAfterCheckedChange = { checked ->
                            quotaHaptics.toggle(checked)
                        }
                    )
                    ToggleControlRow(
                        icon = Icons.Outlined.Tune,
                        title = "Haptic confirmation",
                        description = "Add tactile feedback when toggles or refresh controls change state.",
                        checked = hapticConfirmation,
                        onCheckedChange = onHapticConfirmationChange,
                        onAfterCheckedChange = { checked ->
                            quotaHaptics.toggle(checked, force = true)
                        }
                    )
                }
            }
        }

        item {
            AnimatedSection(visible = refreshVisible) {
                SettingsControlSection(
                    index = 1,
                    icon = Icons.Outlined.Update,
                    title = "Update rhythm",
                    subtitle = "Pick the cadence that should shape cached quota freshness."
                ) {
                    RefreshProfileDial(
                        selectedProfile = refreshProfile,
                        onProfileSelected = { profile ->
                            if (refreshProfile != profile) {
                                refreshProfile = profile
                                quotaHaptics.toggle(true)
                            }
                        }
                    )
                    ControlSummaryStrip(
                        icon = refreshProfile.icon,
                        title = refreshProfile.title,
                        body = refreshProfile.summary
                    )
                }
            }
        }

        item {
            AnimatedSection(visible = awarenessVisible) {
                SettingsControlSection(
                    index = 2,
                    icon = Icons.Outlined.NotificationsActive,
                    title = "Awareness",
                    subtitle = "Decide which changes should surface without turning Home into noise."
                ) {
                    ToggleControlRow(
                        icon = Icons.Outlined.NotificationsActive,
                        title = "Usage alerts",
                        description = "Notify when a provider crosses your preferred quota threshold.",
                        checked = usageAlerts,
                        onCheckedChange = { usageAlerts = it },
                        onAfterCheckedChange = { checked ->
                            quotaHaptics.toggle(checked)
                        }
                    )
                    ToggleControlRow(
                        icon = Icons.Outlined.Bolt,
                        title = "Low-balance banner",
                        description = "Pin a stronger warning at the top of Home when a limit is close.",
                        checked = lowBalanceBanner,
                        onCheckedChange = { lowBalanceBanner = it },
                        onAfterCheckedChange = { checked ->
                            quotaHaptics.toggle(checked)
                        }
                    )
                    ToggleControlRow(
                        icon = Icons.Outlined.PrivacyTip,
                        title = "Private summaries",
                        description = "Hide provider names in notifications while still showing urgency and reset times.",
                        checked = privacyShield,
                        onCheckedChange = { privacyShield = it },
                        onAfterCheckedChange = { checked ->
                            quotaHaptics.toggle(checked)
                        }
                    )
                }
            }
        }

        item {
            AnimatedSection(visible = transparencyVisible) {
                SettingsControlSection(
                    index = 3,
                    icon = Icons.Outlined.Storage,
                    title = "Data transparency",
                    subtitle = "Keep implementation facts visible as compact readouts."
                ) {
                    ReadoutControlRow(
                        icon = Icons.Outlined.Storage,
                        title = "Storage",
                        value = "On-device Room",
                        description = "The current build stores provider subscriptions and quota snapshots locally."
                    )
                    ReadoutControlRow(
                        icon = Icons.Outlined.Update,
                        title = "Recommended preset",
                        value = refreshProfile.title,
                        description = "Balanced is the default expressive rhythm for this page."
                    )
                    ReadoutControlRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Accent source",
                        value = if (dynamicPaletteEnabled) "Dynamic color" else "App controlled",
                        description = "Material Expressive uses motion, type, and tone together instead of heavy chrome."
                    )
                }
            }
        }

        item {
            RestoreTuningCommand(
                onClick = {
                    dynamicPaletteEnabled = true
                    onHighEmphasisMetricsChange(true)
                    onHapticConfirmationChange(true)
                    usageAlerts = true
                    lowBalanceBanner = true
                    privacyShield = true
                    refreshProfile = RefreshProfile.Balanced
                    quotaHaptics.refreshResult(success = true)
                }
            )
        }
    }
}

@Composable
private fun AnimatedSection(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = spring(stiffness = 420f, dampingRatio = 0.92f)
        ) + slideInVertically(
            animationSpec = spring(stiffness = 420f, dampingRatio = 0.92f),
            initialOffsetY = { it / 6 }
        )
    ) {
        content()
    }
}

@Composable
private fun SettingsControlHeader(
    refreshProfile: RefreshProfile,
    dynamicPaletteEnabled: Boolean,
    usageAlerts: Boolean,
    privacyShield: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val titleScale by animateFloatAsState(
        targetValue = if (dynamicPaletteEnabled) 1f else 0.985f,
        animationSpec = spring(stiffness = 360f, dampingRatio = 0.85f),
        label = "settingsTitleScale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(118.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colorScheme.primary,
                                colorScheme.tertiary,
                                colorScheme.secondary
                            )
                        ),
                        shape = RoundedCornerShape(99.dp)
                    )
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                    modifier = Modifier.graphicsLayer {
                        scaleX = titleScale
                        scaleY = titleScale
                    }
                )
                Text(
                    text = "Adjust display, refresh cadence, awareness, and local data behavior without entering a provider detail view.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HeaderSignalChip(
                label = if (dynamicPaletteEnabled) "Dynamic palette" else "App palette",
                icon = Icons.Outlined.Palette,
                index = 0
            )
            HeaderSignalChip(
                label = refreshProfile.title,
                icon = refreshProfile.icon,
                index = 1
            )
            HeaderSignalChip(
                label = if (usageAlerts) "Alerts armed" else "Alerts muted",
                icon = Icons.Outlined.NotificationsActive,
                index = 2
            )
            HeaderSignalChip(
                label = if (privacyShield) "Private labels" else "Full labels",
                icon = Icons.Outlined.PrivacyTip,
                index = 3
            )
        }
    }
}

@Composable
private fun HeaderSignalChip(
    label: String,
    icon: ImageVector,
    index: Int
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        color = colorScheme.surfaceContainerHigh,
        shape = expressiveSettingsShape(index),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}

@Composable
private fun SettingsControlSection(
    index: Int,
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        SectionControlRail(
            index = index,
            icon = icon
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SectionControlRail(
    index: Int,
    icon: ImageVector
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = when (index % 3) {
                0 -> colorScheme.primaryContainer
                1 -> colorScheme.tertiaryContainer
                else -> colorScheme.secondaryContainer
            },
            contentColor = when (index % 3) {
                0 -> colorScheme.onPrimaryContainer
                1 -> colorScheme.onTertiaryContainer
                else -> colorScheme.onSecondaryContainer
            },
            shape = expressiveSettingsShape(index + 2),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null
                )
            }
        }
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(34.dp)
                .background(
                    color = colorScheme.outlineVariant.copy(alpha = 0.54f),
                    shape = RoundedCornerShape(99.dp)
                )
        )
    }
}

@Composable
private fun ToggleControlRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onAfterCheckedChange: (Boolean) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (checked) colorScheme.primaryContainer.copy(alpha = 0.74f) else colorScheme.surfaceContainerLow,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "toggleControlContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) colorScheme.primary.copy(alpha = 0.72f) else colorScheme.outlineVariant.copy(alpha = 0.22f),
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "toggleControlBorder"
    )
    val shape = expressiveSettingsShape(title.length)

    fun commit(updated: Boolean) {
        onCheckedChange(updated)
        onAfterCheckedChange(updated)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .selectable(
                selected = checked,
                onClick = { commit(!checked) },
                role = Role.Switch
            ),
        color = containerColor,
        shape = shape,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = if (checked) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlGlyph(
                icon = icon,
                emphasized = checked
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colorScheme.onSurfaceVariant
                    )
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = ::commit
            )
        }
    }
}

@Composable
private fun ReadoutControlRow(
    icon: ImageVector,
    title: String,
    value: String,
    description: String
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = expressiveSettingsShape(value.length)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colorScheme.surfaceContainerLow,
        shape = shape,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlGlyph(icon = icon)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colorScheme.onSurfaceVariant
                    )
                )
            }
            Surface(
                color = colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.14f))
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.primary
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun RefreshProfileDial(
    selectedProfile: RefreshProfile,
    onProfileSelected: (RefreshProfile) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        color = colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 34.dp,
            bottomStart = 34.dp,
            bottomEnd = 28.dp
        ),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RefreshProfile.entries.forEachIndexed { index, profile ->
                RefreshProfileSegment(
                    profile = profile,
                    selected = selectedProfile == profile,
                    index = index,
                    onClick = { onProfileSelected(profile) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RefreshProfileSegment(
    profile: RefreshProfile,
    selected: Boolean,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (selected) colorScheme.tertiaryContainer else colorScheme.surface.copy(alpha = 0.0f),
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "refreshSegmentContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colorScheme.onTertiaryContainer else colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "refreshSegmentContent"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.85f),
        label = "refreshSegmentScale"
    )
    val shape = expressiveSettingsShape(index + 5)

    Surface(
        modifier = modifier
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
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = profile.icon,
                contentDescription = null
            )
            Text(
                text = profile.title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = profile.caption,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun ControlSummaryStrip(
    icon: ImageVector,
    title: String,
    body: String
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        color = colorScheme.tertiaryContainer.copy(alpha = 0.56f),
        contentColor = colorScheme.onTertiaryContainer,
        shape = expressiveSettingsShape(8)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colorScheme.onTertiaryContainer.copy(alpha = 0.82f)
                    )
                )
            }
        }
    }
}

@Composable
private fun RestoreTuningCommand(onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(
        topStart = 30.dp,
        topEnd = 20.dp,
        bottomStart = 20.dp,
        bottomEnd = 34.dp
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        color = colorScheme.primaryContainer,
        contentColor = colorScheme.onPrimaryContainer,
        shape = shape,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Restore recommended tuning",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun ControlGlyph(
    icon: ImageVector,
    emphasized: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (emphasized) colorScheme.primary else colorScheme.surfaceContainerHigh,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "controlGlyphContainer"
    )
    val iconTint by animateColorAsState(
        targetValue = if (emphasized) colorScheme.onPrimary else colorScheme.primary,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "controlGlyphTint"
    )

    Surface(
        color = containerColor,
        shape = expressiveSettingsShape(if (emphasized) 3 else 0),
        modifier = Modifier.size(46.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint
            )
        }
    }
}

private fun expressiveSettingsShape(index: Int): RoundedCornerShape {
    return when (index % 5) {
        0 -> RoundedCornerShape(
            topStart = 22.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 26.dp
        )
        1 -> RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 28.dp,
            bottomStart = 22.dp,
            bottomEnd = 16.dp
        )
        2 -> RoundedCornerShape(
            topStart = 26.dp,
            topEnd = 20.dp,
            bottomStart = 14.dp,
            bottomEnd = 24.dp
        )
        3 -> RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 24.dp,
            bottomStart = 28.dp,
            bottomEnd = 18.dp
        )
        else -> RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 18.dp,
            bottomStart = 20.dp,
            bottomEnd = 30.dp
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    QuotaHubTheme {
        SettingsScreen(
            highEmphasisMetrics = true,
            hapticConfirmation = true,
            onHighEmphasisMetricsChange = {},
            onHapticConfirmationChange = {}
        )
    }
}
