package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.BrassGold
import com.example.ui.theme.HarmonicCyan
import com.example.ui.theme.MinorRose
import java.util.Locale

@Composable
fun OscilloscopeStrip(
    waveform: FloatArray,
    peakLevel: Float,
    activeChordLabel: String,
    activeFrequencyHz: Double?,
    isMinorAccent: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp
) {
    val accentColor = when {
        activeChordLabel.isEmpty() -> HarmonicCyan
        isMinorAccent -> MinorRose
        else -> BrassGold
    }
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f)
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .border(1.dp, outlineColor, RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midY = h * 0.5f

            // Subtle studio grid lines
            drawLine(
                color = outlineColor.copy(alpha = 0.35f),
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1f
            )
            val quarterStep = w / 4f
            for (i in 1..3) {
                drawLine(
                    color = outlineColor.copy(alpha = 0.2f),
                    start = Offset(quarterStep * i, 0f),
                    end = Offset(quarterStep * i, h),
                    strokeWidth = 1f
                )
            }

            if (waveform.size > 1) {
                val path = Path()
                val stepX = w / (waveform.size - 1).toFloat()
                val amp = h * 0.40f
                for (i in waveform.indices) {
                    val x = i * stepX
                    val y = midY - (waveform[i].coerceIn(-1f, 1f) * amp)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                // Soft phosphor outer bloom
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        listOf(
                            HarmonicCyan.copy(alpha = 0.25f + peakLevel * 0.2f),
                            accentColor.copy(alpha = 0.35f + peakLevel * 0.25f),
                            HarmonicCyan.copy(alpha = 0.25f + peakLevel * 0.2f)
                        )
                    ),
                    style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Crisp inner waveform trace
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        listOf(HarmonicCyan, accentColor, HarmonicCyan)
                    ),
                    style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activeChordLabel.ifEmpty { stringResource(R.string.active_voices_empty) },
                    style = if (activeChordLabel.isNotEmpty()) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.bodySmall
                    },
                    color = if (activeChordLabel.isNotEmpty()) {
                        accentColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (activeFrequencyHz != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xAA0B0E14))
                        .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f Hz", activeFrequencyHz),
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor
                    )
                }
            }
        }
    }
}
