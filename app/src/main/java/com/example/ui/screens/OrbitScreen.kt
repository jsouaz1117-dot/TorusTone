package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.OrbitPattern
import com.example.model.OrbitVoicing
import com.example.model.TonnetzMath
import com.example.ui.TorusToneUiState
import com.example.ui.theme.BrassGold
import com.example.ui.theme.HarmonicCyan
import com.example.ui.theme.MinorRose
import com.example.ui.theme.MonoFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun OrbitScreen(
    uiState: TorusToneUiState,
    onToggleOrbit: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onRootChange: (Int) -> Unit,
    onPatternChange: (OrbitPattern) -> Unit,
    onVoicingChange: (OrbitVoicing) -> Unit,
    onToggleStep: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val activePitchClasses = uiState.activeMidiNotes.map { TonnetzMath.positiveMod12(it) }.toSet()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.orbit_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.orbit_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = HarmonicCyan
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, BrassGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.orbit_bpm, uiState.orbitBpm),
                    style = MaterialTheme.typography.labelMedium,
                    color = BrassGold
                )
            }
        }

        // Animated Toroidal Orbit Ring Visualizer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(238.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF172133), Color(0xFF0C1018))
                    )
                )
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
        ) {
            OrbitRingCanvas(
                rootPitchClass = uiState.orbitRootPitchClass,
                activePitchClasses = activePitchClasses,
                currentStep = uiState.orbitCurrentStep,
                pattern = uiState.orbitPattern,
                enabledSteps = uiState.orbitEnabledSteps,
                isMinorAccent = uiState.lastTriggeredIsMinor,
                centerChordLabel = uiState.activeChordLabel.ifEmpty {
                    TonnetzMath.noteName(uiState.orbitRootPitchClass)
                },
                orbitPlaying = uiState.orbitPlaying
            )
        }

        // Primary Transport + BPM + Voicing Console
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onToggleOrbit,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.orbitPlaying) MinorRose else BrassGold,
                        contentColor = Color(0xFF120C04)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("orbit_play_toggle_button")
                ) {
                    Icon(
                        imageVector = if (uiState.orbitPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (uiState.orbitPlaying) {
                            stringResource(R.string.orbit_stop)
                        } else {
                            stringResource(R.string.orbit_play)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // BPM Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = null,
                        tint = HarmonicCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Slider(
                        value = uiState.orbitBpm.toFloat(),
                        onValueChange = { onBpmChange(it.roundToInt()) },
                        valueRange = 45f..160f,
                        colors = SliderDefaults.colors(
                            thumbColor = HarmonicCyan,
                            activeTrackColor = HarmonicCyan,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("orbit_bpm_slider")
                    )
                    Text(
                        text = "${uiState.orbitBpm}",
                        style = MaterialTheme.typography.labelMedium,
                        color = HarmonicCyan,
                        modifier = Modifier.width(34.dp)
                    )
                }

                // Voicing Style Row
                Text(
                    text = stringResource(R.string.orbit_arp_mode),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoicingOption(
                        label = stringResource(R.string.voicing_chord),
                        selected = uiState.orbitVoicing == OrbitVoicing.CHORD,
                        onClick = { onVoicingChange(OrbitVoicing.CHORD) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voicing_chord_button")
                    )
                    VoicingOption(
                        label = stringResource(R.string.voicing_arp_up),
                        selected = uiState.orbitVoicing == OrbitVoicing.ARP_UP,
                        onClick = { onVoicingChange(OrbitVoicing.ARP_UP) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voicing_arp_button")
                    )
                    VoicingOption(
                        label = stringResource(R.string.voicing_arp_wave),
                        selected = uiState.orbitVoicing == OrbitVoicing.TORUS_WAVE,
                        onClick = { onVoicingChange(OrbitVoicing.TORUS_WAVE) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voicing_wave_button")
                    )
                }
            }
        }

        // Interactive 8-Step Orbit Gate Strip
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.orbit_steps_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                uiState.orbitPattern.steps.forEachIndexed { idx, step ->
                    val enabled = uiState.orbitEnabledSteps.getOrElse(idx) { true }
                    val isCurrent = uiState.orbitPlaying && uiState.orbitCurrentStep == idx
                    val chordPc = TonnetzMath.positiveMod12(uiState.orbitRootPitchClass + step.rootOffset)
                    val chordText = TonnetzMath.noteName(chordPc) + if (step.isMinor) "m" else ""
                    val accent = if (step.isMinor) MinorRose else BrassGold

                    Surface(
                        onClick = { onToggleStep(idx) },
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            isCurrent && enabled -> accent
                            enabled -> accent.copy(alpha = 0.16f)
                            else -> MaterialTheme.colorScheme.surfaceContainer
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                isCurrent -> Color.White
                                enabled -> accent.copy(alpha = 0.55f)
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("orbit_step_$idx")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = chordText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isCurrent && enabled -> Color(0xFF100B04)
                                    enabled -> accent
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                },
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Root Key Selector (12 Pitch Classes)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.orbit_root_key),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (pc in 0..11) {
                    val selected = pc == uiState.orbitRootPitchClass
                    Surface(
                        onClick = { onRootChange(pc) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) HarmonicCyan.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(
                            1.dp,
                            if (selected) HarmonicCyan else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .size(width = 48.dp, height = 44.dp)
                            .testTag("root_key_$pc")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = TonnetzMath.noteName(pc),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) HarmonicCyan else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Geometric Trajectory Patterns
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Loop,
                    contentDescription = null,
                    tint = BrassGold,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.orbit_patterns),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            OrbitPattern.entries.forEach { pattern ->
                val selected = pattern == uiState.orbitPattern
                Surface(
                    onClick = { onPatternChange(pattern) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) BrassGold.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(
                        1.dp,
                        if (selected) BrassGold else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("orbit_pattern_${pattern.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pattern.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected) BrassGold else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = pattern.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${pattern.steps.size} STEPS",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) HarmonicCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun OrbitRingCanvas(
    rootPitchClass: Int,
    activePitchClasses: Set<Int>,
    currentStep: Int,
    pattern: OrbitPattern,
    enabledSteps: List<Boolean>,
    isMinorAccent: Boolean,
    centerChordLabel: String,
    orbitPlaying: Boolean
) {
    val textMeasurer = rememberTextMeasurer()
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val bgDeep = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val outerR = minOf(size.width, size.height) * 0.39f
        val innerR = outerR * 0.62f

        // Concentric torus meridian rings
        drawCircle(
            color = outlineColor.copy(alpha = 0.45f),
            radius = outerR,
            center = Offset(cx, cy),
            style = Stroke(width = 1.6f)
        )
        drawCircle(
            color = HarmonicCyan.copy(alpha = 0.22f),
            radius = innerR,
            center = Offset(cx, cy),
            style = Stroke(width = 1.2f)
        )

        // Compute Circle-of-Fifths positions around outer ring (torus meridian)
        val fifthsPositions = HashMap<Int, Offset>(12)
        for (i in 0 until 12) {
            val pc = TonnetzMath.positiveMod12(i * 7)
            val angle = (i.toDouble() / 12.0) * 2.0 * PI - PI / 2.0
            val px = cx + (outerR * cos(angle)).toFloat()
            val py = cy + (outerR * sin(angle)).toFloat()
            fifthsPositions[pc] = Offset(px, py)
        }

        // Draw glowing geometric chord polygon connecting active pitch classes
        val activeOffsets = activePitchClasses.mapNotNull { fifthsPositions[it] }
        val accent = if (isMinorAccent) MinorRose else BrassGold
        if (activeOffsets.size >= 2) {
            val polyPath = Path().apply {
                moveTo(activeOffsets.first().x, activeOffsets.first().y)
                for (i in 1 until activeOffsets.size) {
                    lineTo(activeOffsets[i].x, activeOffsets[i].y)
                }
                close()
            }
            drawPath(
                path = polyPath,
                color = accent.copy(alpha = 0.18f),
                style = Fill
            )
            drawPath(
                path = polyPath,
                color = accent.copy(alpha = 0.85f),
                style = Stroke(width = 2.6f, cap = StrokeCap.Round)
            )
        }

        // Draw 8 step dots on the inner orbital ring
        val stepCount = pattern.steps.size
        for (s in 0 until stepCount) {
            val angle = (s.toDouble() / stepCount.toDouble()) * 2.0 * PI - PI / 2.0
            val sx = cx + (innerR * cos(angle)).toFloat()
            val sy = cy + (innerR * sin(angle)).toFloat()
            val enabled = enabledSteps.getOrElse(s) { true }
            val isNow = orbitPlaying && s == currentStep

            if (isNow) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(HarmonicCyan.copy(alpha = 0.65f), Color.Transparent),
                        center = Offset(sx, sy),
                        radius = 22f
                    ),
                    radius = 22f,
                    center = Offset(sx, sy)
                )
            }
            drawCircle(
                color = when {
                    isNow -> HarmonicCyan
                    enabled -> BrassGold.copy(alpha = 0.72f)
                    else -> outlineColor.copy(alpha = 0.4f)
                },
                radius = if (isNow) 7.5f else 4.5f,
                center = Offset(sx, sy)
            )
        }

        // Draw 12 pitch nodes on the outer meridian ring
        for (i in 0 until 12) {
            val pc = TonnetzMath.positiveMod12(i * 7)
            val pos = fifthsPositions[pc] ?: continue
            val isActive = pc in activePitchClasses
            val isRoot = pc == rootPitchClass
            val r = when {
                isActive -> 16f
                isRoot -> 14f
                else -> 11.5f
            }

            if (isActive) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.55f), Color.Transparent),
                        center = pos,
                        radius = r * 2.0f
                    ),
                    radius = r * 2.0f,
                    center = pos
                )
            }

            drawCircle(
                color = when {
                    isActive -> accent
                    isRoot -> HarmonicCyan.copy(alpha = 0.9f)
                    else -> bgDeep
                },
                radius = r,
                center = pos
            )
            drawCircle(
                color = when {
                    isActive -> Color.White
                    isRoot -> HarmonicCyan
                    else -> outlineColor
                },
                radius = r,
                center = pos,
                style = Stroke(width = if (isActive) 2f else 1.2f)
            )

            val label = TonnetzMath.noteName(pc)
            val measured = textMeasurer.measure(
                text = label,
                style = TextStyle(
                    fontFamily = MonoFontFamily,
                    fontSize = 9.5.sp,
                    fontWeight = if (isActive || isRoot) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive || isRoot) bgDeep else textPrimary
                )
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(pos.x - measured.size.width / 2f, pos.y - measured.size.height / 2f)
            )
        }

        // Center Active Chord Readout
        val centerMeasured = textMeasurer.measure(
            text = centerChordLabel,
            style = TextStyle(
                fontFamily = MonoFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (orbitPlaying) accent else textPrimary
            )
        )
        drawText(
            textLayoutResult = centerMeasured,
            topLeft = Offset(
                cx - centerMeasured.size.width / 2f,
                cy - centerMeasured.size.height / 2f
            )
        )
    }
}

@Composable
private fun VoicingOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) HarmonicCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            if (selected) HarmonicCyan else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.height(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) HarmonicCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
