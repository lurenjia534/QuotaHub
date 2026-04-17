package com.lurenjia534.quotahub.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            AnimatedSection(visible = headerVisible) {
                SettingsHero(
                    refreshProfile = refreshProfile,
                    dynamicPaletteEnabled = dynamicPaletteEnabled,
                    usageAlerts = usageAlerts
                )
            }
        }

        item {
            AnimatedSection(visible = displayVisible) {
                SettingsGroup(
                    title = "Look & feel",
                    subtitle = "Dynamic color and emphasized type keep quota changes easier to scan."
                ) {
                    TogglePreferenceRow(
                        icon = Icons.Outlined.Palette,
                        title = "Wallpaper palette",
                        description = "Borrow system-derived tones so provider accents feel native to the device.",
                        checked = dynamicPaletteEnabled,
                        onCheckedChange = { dynamicPaletteEnabled = it },
                        onAfterCheckedChange = { checked ->
                            quotaHaptics.toggle(checked)
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    TogglePreferenceRow(
                        icon = Icons.Outlined.Visibility,
                        title = "High-emphasis metrics",
                        description = "Use stronger type contrast for remaining quota, reset dates, and low balances.",
                        checked = highEmphasisMetrics,
                        onCheckedChange = onHighEmphasisMetricsChange,
                        onAfterCheckedChange = { checked ->
                            quotaHaptics.toggle(checked)
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    TogglePreferenceRow(
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
                SettingsGroup(
                    title = "Update rhythm",
                    subtitle = "Material Expressive favors glanceable choices, so the refresh strategy stays visible."
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RefreshProfile.entries.forEach { profile ->
                            RefreshProfileTile(
                                profile = profile,
                                selected = refreshProfile == profile,
                                onClick = { refreshProfile = profile },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Text(
                        text = refreshProfile.summary,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                    )
                }
            }
        }

        item {
            AnimatedSection(visible = awarenessVisible) {
                SettingsGroup(
                    title = "Awareness",
                    subtitle = "Keep the important status changes loud without making the page feel busy."
                ) {
                    TogglePreferenceRow(
                        icon = Icons.Outlined.NotificationsActive,
                        title = "Usage alerts",
                        description = "Notify when a provider crosses your preferred quota threshold.",
                        checked = usageAlerts,
                        onCheckedChange = { usageAlerts = it },
                        onAfterCheckedChange = { checked ->
                            quotaHaptics.toggle(checked)
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    TogglePreferenceRow(
                        icon = Icons.Outlined.Bolt,
                        title = "Low-balance banner",
                        description = "Pin a stronger warning at the top of Home when a limit is close.",
                        checked = lowBalanceBanner,
                        onCheckedChange = { lowBalanceBanner = it },
                        onAfterCheckedChange = { checked ->
                            quotaHaptics.toggle(checked)
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    TogglePreferenceRow(
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
                SettingsGroup(
                    title = "Data transparency",
                    subtitle = "A compact system readout keeps the settings page useful instead of ornamental."
                ) {
                    ValuePreferenceRow(
                        icon = Icons.Outlined.Storage,
                        title = "Storage",
                        value = "On-device Room",
                        description = "The current build stores provider subscriptions and quota snapshots locally."
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ValuePreferenceRow(
                        icon = Icons.Outlined.Update,
                        title = "Recommended preset",
                        value = refreshProfile.title,
                        description = "Balanced is the default expressive rhythm for this page."
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ValuePreferenceRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Accent source",
                        value = if (dynamicPaletteEnabled) "Dynamic color" else "App controlled",
                        description = "Material Expressive uses motion, type, and tone together instead of heavy chrome."
                    )
                }
            }
        }

        item {
            FilledTonalButton(
                onClick = {
                    dynamicPaletteEnabled = true
                    onHighEmphasisMetricsChange(true)
                    onHapticConfirmationChange(true)
                    usageAlerts = true
                    lowBalanceBanner = true
                    privacyShield = true
                    refreshProfile = RefreshProfile.Balanced
                    quotaHaptics.refreshResult(success = true)
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
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
private fun SettingsHero(
    refreshProfile: RefreshProfile,
    dynamicPaletteEnabled: Boolean,
    usageAlerts: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val titleScale by animateFloatAsState(
        targetValue = if (dynamicPaletteEnabled) 1f else 0.985f,
        animationSpec = spring(stiffness = 360f, dampingRatio = 0.85f),
        label = "heroTitleScale"
    )

    Surface(
        color = colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(36.dp),
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colorScheme.primaryContainer.copy(alpha = 0.92f),
                            colorScheme.tertiaryContainer.copy(alpha = 0.58f),
                            colorScheme.surfaceContainerHigh
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Surface(
                    color = colorScheme.surface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Material Expressive preset",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        ),
                        modifier = Modifier.graphicsLayer {
                            scaleX = titleScale
                            scaleY = titleScale
                        }
                    )
                    Text(
                        text = "Theme, refresh cadence, alerting, and privacy controls tuned for quick scanning.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = colorScheme.onSurfaceVariant
                        )
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroBadge(
                        label = if (dynamicPaletteEnabled) "Dynamic color on" else "Static palette",
                        icon = Icons.Outlined.Palette
                    )
                    HeroBadge(
                        label = refreshProfile.title,
                        icon = refreshProfile.icon
                    )
                    HeroBadge(
                        label = if (usageAlerts) "Alerts active" else "Alerts muted",
                        icon = Icons.Outlined.NotificationsActive
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBadge(
    label: String,
    icon: ImageVector
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        color = colorScheme.surface.copy(alpha = 0.78f),
        shape = RoundedCornerShape(18.dp)
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
private fun SettingsGroup(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(30.dp),
            tonalElevation = 1.dp
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun TogglePreferenceRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onAfterCheckedChange: (Boolean) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PreferenceIcon(icon = icon)
        Spacer(modifier = Modifier.width(16.dp))
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = { updated ->
                onCheckedChange(updated)
                onAfterCheckedChange(updated)
            }
        )
    }
}

@Composable
private fun ValuePreferenceRow(
    icon: ImageVector,
    title: String,
    value: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PreferenceIcon(icon = icon)
        Spacer(modifier = Modifier.width(16.dp))
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun RefreshProfileTile(
    profile: RefreshProfile,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (selected) colorScheme.secondaryContainer else colorScheme.surface,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "refreshContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colorScheme.primary.copy(alpha = 0.85f) else colorScheme.outlineVariant,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "refreshBorder"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.98f,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.85f),
        label = "refreshScale"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        color = containerColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PreferenceIcon(
                icon = profile.icon,
                emphasized = selected
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = profile.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = profile.caption,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun PreferenceIcon(
    icon: ImageVector,
    emphasized: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (emphasized) colorScheme.primaryContainer else colorScheme.secondaryContainer.copy(alpha = 0.78f),
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "preferenceIconContainer"
    )
    val iconTint by animateColorAsState(
        targetValue = if (emphasized) colorScheme.onPrimaryContainer else colorScheme.primary,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "preferenceIconTint"
    )

    Surface(
        color = containerColor,
        shape = CircleShape,
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
