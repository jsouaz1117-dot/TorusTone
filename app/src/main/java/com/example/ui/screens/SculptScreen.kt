package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.SynthTimbre
import com.example.model.Temperament
import com.example.model.TonnetzMath
import com.example.ui.TorusToneUiState
import com.example.ui.components.OscilloscopeStrip
import com.example.ui.theme.BrassGold
import com.example.ui.theme.HarmonicCyan
import com.example.ui.theme.MinorRose
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun SculptScreen(
    uiState: TorusToneUiState,
    waveform: FloatArray,
    peakLevel: Float,
    onTimbreChange: (SynthTimbre) -> Unit,
    onWarmthChange: (Float) -> Unit,
    onEchoMixChange: (Float) -> Unit,
    onEchoFeedbackChange: (Float) -> Unit,
    onAttackChange: (Float) -> Unit,
    onReleaseChange: (Float) -> Unit,
    onDetuneChange: (Float) -> Unit,
    onMasterGainChange: (Float) -> Unit,
    onTemperamentChange: (Temperament) -> Unit,
    onReferenceA4Change: (Int) -> Unit,
    onAuditionChord: (isMinor: Boolean, addSeventh: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val params = uiState.synthParams
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Studio Hero Banner with Embedded Oscilloscope & Live Audition Pads
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_torus_banner_1795134749828),
                contentDescription = stringResource(R.string.banner_content_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xBB0B0E14),
                                Color(0xCC0B0E14),
                                Color(0xEE0B0E14)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.sculpt_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.sculpt_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = HarmonicCyan
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = null,
                        tint = BrassGold,
                        modifier = Modifier.size(26.dp)
                    )
                }

                OscilloscopeStrip(
                    waveform = waveform,
                    peakLevel = peakLevel,
                    activeChordLabel = uiState.activeChordLabel,
                    activeFrequencyHz = uiState.activeFrequencyHz,
                    isMinorAccent = uiState.lastTriggeredIsMinor,
                    height = 76.dp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuditionButton(
                        label = stringResource(R.string.audition_major),
                        accent = BrassGold,
                        onClick = { onAuditionChord(false, false) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("audition_major_button")
                    )
                    AuditionButton(
                        label = stringResource(R.string.audition_minor),
                        accent = MinorRose,
                        onClick = { onAuditionChord(true, false) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("audition_minor_button")
                    )
                    AuditionButton(
                        label = stringResource(R.string.audition_maj7),
                        accent = HarmonicCyan,
                        onClick = { onAuditionChord(false, true) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("audition_maj7_button")
                    )
                }
            }
        }

        // Oscillator Timbre Cards
        SectionHeader(
            title = stringResource(R.string.section_timbre),
            icon = Icons.Filled.GraphicEq
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SynthTimbre.entries.forEach { timbre ->
                val selected = timbre == params.timbre
                Surface(
                    onClick = { onTimbreChange(timbre) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) BrassGold.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(
                        1.dp,
                        if (selected) BrassGold else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sculpt_timbre_${timbre.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        TimbreWaveGlyph(
                            timbre = timbre,
                            color = if (selected) BrassGold else HarmonicCyan,
                            modifier = Modifier.size(width = 48.dp, height = 30.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = timbre.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected) BrassGold else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = timbre.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Filter & Spatial Echo Panel
        SectionHeader(
            title = stringResource(R.string.section_tone_space),
            icon = Icons.Filled.Waves
        )
        StudioPanelCard {
            val cutoffHz = TonnetzMath.filterCutoffHz(params.warmth)
            val cutoffText = if (cutoffHz >= 1000) {
                String.format(java.util.Locale.US, "%.1f kHz", cutoffHz / 1000f)
            } else {
                "$cutoffHz Hz"
            }
            StudioSliderRow(
                label = stringResource(R.string.param_warmth),
                valueText = cutoffText,
                value = params.warmth,
                valueRange = 0f..1f,
                accent = BrassGold,
                onValueChange = onWarmthChange,
                testTag = "slider_warmth"
            )
            StudioSliderRow(
                label = stringResource(R.string.param_echo_mix),
                valueText = "${(params.echoMix / 0.75f * 100f).roundToInt()}%",
                value = params.echoMix,
                valueRange = 0f..0.75f,
                accent = HarmonicCyan,
                onValueChange = onEchoMixChange,
                testTag = "slider_echo_mix"
            )
            StudioSliderRow(
                label = stringResource(R.string.param_echo_feedback),
                valueText = "${(params.echoFeedback / 0.76f * 100f).roundToInt()}%",
                value = params.echoFeedback,
                valueRange = 0f..0.76f,
                accent = HarmonicCyan,
                onValueChange = onEchoFeedbackChange,
                testTag = "slider_echo_feedback"
            )
        }

        // Envelope & Modulation Panel
        SectionHeader(
            title = stringResource(R.string.section_envelope),
            icon = Icons.Filled.Tune
        )
        StudioPanelCard {
            StudioSliderRow(
                label = stringResource(R.string.param_attack),
                valueText = "${params.attackMs.roundToInt()} ms",
                value = params.attackMs,
                valueRange = 4f..400f,
                accent = BrassGold,
                onValueChange = onAttackChange,
                testTag = "slider_attack"
            )
            StudioSliderRow(
                label = stringResource(R.string.param_release),
                valueText = "${params.releaseMs.roundToInt()} ms",
                value = params.releaseMs,
                valueRange = 80f..2200f,
                accent = MinorRose,
                onValueChange = onReleaseChange,
                testTag = "slider_release"
            )
            StudioSliderRow(
                label = stringResource(R.string.param_detune),
                valueText = "+${params.detuneCents.roundToInt()} ¢",
                value = params.detuneCents,
                valueRange = 0f..35f,
                accent = HarmonicCyan,
                onValueChange = onDetuneChange,
                testTag = "slider_detune"
            )
            StudioSliderRow(
                label = stringResource(R.string.param_master_gain),
                valueText = "${(params.masterGain * 100f).roundToInt()}%",
                value = params.masterGain,
                valueRange = 0.15f..1.0f,
                accent = BrassGold,
                onValueChange = onMasterGainChange,
                testTag = "slider_master_gain"
            )
        }

        // Temperament & Concert Reference
        SectionHeader(
            title = stringResource(R.string.section_tuning),
            icon = Icons.Filled.GraphicEq
        )
        StudioPanelCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChoicePill(
                    label = stringResource(R.string.temperament_equal),
                    selected = params.temperament == Temperament.EQUAL_12TET,
                    accent = BrassGold,
                    onClick = { onTemperamentChange(Temperament.EQUAL_12TET) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("temperament_equal_button")
                )
                ChoicePill(
                    label = stringResource(R.string.temperament_just),
                    selected = params.temperament == Temperament.JUST_INTONATION,
                    accent = HarmonicCyan,
                    onClick = { onTemperamentChange(Temperament.JUST_INTONATION) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("temperament_just_button")
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChoicePill(
                    label = "A4 = 440 Hz",
                    selected = params.referenceA4Hz == 440,
                    accent = BrassGold,
                    onClick = { onReferenceA4Change(440) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ref_440_button")
                )
                ChoicePill(
                    label = "A4 = 432 Hz",
                    selected = params.referenceA4Hz == 432,
                    accent = MinorRose,
                    onClick = { onReferenceA4Change(432) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ref_432_button")
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrassGold,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun StudioPanelCard(
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun StudioSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accent: Color,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun AuditionButton(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
        modifier = modifier.height(44.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ChoicePill(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, if (selected) accent else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.height(46.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimbreWaveGlyph(
    timbre: SynthTimbre,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h * 0.5f
        val amp = h * 0.36f
        val path = Path()
        val samples = 48
        for (i in 0..samples) {
            val t = i.toFloat() / samples
            val rad = t * 2.0 * PI * 2.0
            val yNorm = when (timbre) {
                SynthTimbre.CELESTIAL_SINE -> sin(rad)
                SynthTimbre.WARM_RHODES -> sin(rad + 0.9 * sin(rad * 2.0)) * 0.9
                SynthTimbre.ANALOG_BRASS -> (sin(rad) + 0.5 * sin(2 * rad) + 0.33 * sin(3 * rad)) * 0.62
                SynthTimbre.GLASS_ORGAN -> (sin(rad) * 0.6 + sin(2 * rad) * 0.3 + sin(3 * rad) * 0.25)
                SynthTimbre.SUB_DRONE -> {
                    val phase = (t * 2.0) % 1.0
                    4.0 * kotlin.math.abs(phase - 0.5) - 1.0
                }
            }
            val x = t * w
            val y = midY - (yNorm.toFloat() * amp)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.4f, cap = StrokeCap.Round)
        )
    }
}
