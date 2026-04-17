package com.lurenjia534.quotahub.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

enum class MetricEmphasisLevel {
    Hero,
    Standard,
    Compact
}

@Composable
fun QuotaMetricText(
    text: String,
    emphasized: Boolean,
    level: MetricEmphasisLevel,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val resolvedColor = if (color == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurface
    } else {
        color
    }

    val style = when (level) {
        MetricEmphasisLevel.Hero -> {
            if (emphasized) {
                MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black)
            } else {
                MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            }
        }

        MetricEmphasisLevel.Standard -> {
            if (emphasized) {
                MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            }
        }

        MetricEmphasisLevel.Compact -> {
            if (emphasized) {
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            }
        }
    }

    Text(
        text = text,
        modifier = modifier,
        style = style.copy(color = resolvedColor),
        maxLines = maxLines,
        overflow = overflow
    )
}
