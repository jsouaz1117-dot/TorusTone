package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CanvasMode
import com.example.model.MusicalScale
import com.example.model.SynthTimbre
import com.example.model.TonnetzMath
import com.example.model.TouchMode
import com.example.ui.TorusToneUiState
import com.example.ui.components.OscilloscopeStrip
import com.example.ui.components.TonnetzGridCanvas
import com.example.ui.components.Torus3DCanvas
import com.example.ui.theme.BrassGold
import com.example.ui.theme.HarmonicCyan
import com.example.ui.theme.MinorRose

@Composable
fun PlayScreen(
    uiState: TorusToneUiState,
    waveform: FloatArray,
    peakLevel: Float,
    onCanvasModeChange: (CanvasMode) -> Unit,
    onTouchModeChange: (TouchMode) -> Unit,
    onScaleChange: (MusicalScale) -> Unit,
    onTimbreChange: (SynthTimbre) -> Unit,
    onToggleLatch: () -> Unit,
    onSilenceAll: () -> Unit,
    onShiftOctave: (Int) -> Unit,
    onNodePressed: (Int) -> Unit,
    onTriadPressed: (Int, Boolean) -> Unit,
    onSingleNotePressed: (Int) -> Unit,
    onReleased: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePitchClasses = remember(uiState.activeMidiNotes) {
        uiState.activeMidiNotes.map { TonnetzMath.positiveMod12(it) }.toSet()
    }
    var showInfoDialog by remember { mutableStateOf(false) }
    var scaleMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (uiState.orbitPlaying) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(HarmonicCyan.copy(alpha = 0.18f))
                                .border(1.dp, HarmonicCyan.copy(alpha = 0.55f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ORBIT LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = HarmonicCyan
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.play_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box {
                    Surface(
                        onClick = { scaleMenuExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("scale_selector_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = stringResource(R.string.scale_label),
                                tint = HarmonicCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${TonnetzMath.noteName(uiState.orbitRootPitchClass)} ${uiState.scale.displayName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = scaleMenuExpanded,
                        onDismissRequest = { scaleMenuExpanded = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        MusicalScale.entries.forEach { scale ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = scale.displayName,
                                        color = if (scale == uiState.scale) BrassGold else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onScaleChange(scale)
                                    scaleMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showInfoDialog = true },
                    modifier = Modifier.testTag("geometry_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = stringResource(R.string.info_torus_theory),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Live Oscilloscope & Telemetry Strip
        OscilloscopeStrip(
            waveform = waveform,
            peakLevel = peakLevel,
            activeChordLabel = uiState.activeChordLabel,
            activeFrequencyHz = uiState.activeFrequencyHz,
            isMinorAccent = uiState.lastTriggeredIsMinor,
            height = 58.dp
        )

        // Instrument Mode Toggle Strip (3D Torus / Tonnetz + Triad / Note)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModePillButton(
                    label = stringResource(R.string.mode_torus_3d),
                    icon = Icons.Filled.Public,
                    selected = uiState.canvasMode == CanvasMode.TORUS_3D,
                    accent = BrassGold,
                    onClick = { onCanvasModeChange(CanvasMode.TORUS_3D) },
                    modifier = Modifier.testTag("mode_torus_3d_button")
                )
                ModePillButton(
                    label = stringResource(R.string.mode_tonnetz_grid),
                    icon = Icons.Filled.GridOn,
                    selected = uiState.canvasMode == CanvasMode.TONNETZ_GRID,
                    accent = BrassGold,
                    onClick = { onCanvasModeChange(CanvasMode.TONNETZ_GRID) },
                    modifier = Modifier.testTag("mode_tonnetz_button")
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactToggleChip(
                    label = stringResource(R.string.touch_mode_triad),
                    selected = uiState.touchMode == TouchMode.TRIAD,
                    accent = HarmonicCyan,
                    onClick = { onTouchModeChange(TouchMode.TRIAD) },
                    modifier = Modifier.testTag("touch_mode_triad_button")
                )
                CompactToggleChip(
                    label = stringResource(R.string.touch_mode_note),
                    selected = uiState.touchMode == TouchMode.SINGLE_NOTE,
                    accent = HarmonicCyan,
                    onClick = { onTouchModeChange(TouchMode.SINGLE_NOTE) },
                    modifier = Modifier.testTag("touch_mode_note_button")
                )
            }
        }

        // Main Interactive Torus / Tonnetz Instrument Stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF151E2E),
                            Color(0xFF0D121B),
                            Color(0xFF090C12)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            AnimatedContent(
                targetState = uiState.canvasMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "canvas_mode_transition"
            ) { mode ->
                when (mode) {
                    CanvasMode.TORUS_3D -> {
                        Torus3DCanvas(
                            activePitchClasses = activePitchClasses,
                            scale = uiState.scale,
                            rootPitchClass = uiState.orbitRootPitchClass,
                            isMinorAccent = uiState.lastTriggeredIsMinor,
                            onNodePressed = onNodePressed,
                            onNodeReleased = onReleased
                        )
                    }

                    CanvasMode.TONNETZ_GRID -> {
                        TonnetzGridCanvas(
                            activePitchClasses = activePitchClasses,
                            scale = uiState.scale,
                            rootPitchClass = uiState.orbitRootPitchClass,
                            touchMode = uiState.touchMode,
                            onTriadPressed = onTriadPressed,
                            onSingleNotePressed = onSingleNotePressed,
                            onReleased = onReleased
                        )
                    }
                }
            }

            // Subtle Gesture Hint Pill at bottom edge of canvas
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xB30B0E14))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (uiState.canvasMode == CanvasMode.TORUS_3D) {
                        stringResource(R.string.hint_torus_3d)
                    } else {
                        stringResource(R.string.hint_tonnetz)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tactile Performance Dock (Latch, Silence, Octave)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onToggleLatch,
                shape = RoundedCornerShape(14.dp),
                color = if (uiState.latchEnabled) BrassGold.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    if (uiState.latchEnabled) BrassGold else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("latch_toggle_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (uiState.latchEnabled) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = stringResource(R.string.action_latch),
                        tint = if (uiState.latchEnabled) BrassGold else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_latch),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (uiState.latchEnabled) BrassGold else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                onClick = onSilenceAll,
                shape = RoundedCornerShape(14.dp),
                color = if (uiState.activeMidiNotes.isNotEmpty()) MinorRose.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    if (uiState.activeMidiNotes.isNotEmpty()) MinorRose.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("silence_all_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolumeOff,
                        contentDescription = stringResource(R.string.action_silence),
                        tint = if (uiState.activeMidiNotes.isNotEmpty()) MinorRose else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_silence),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (uiState.activeMidiNotes.isNotEmpty()) MinorRose else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Octave Stepper
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onShiftOctave(-1) },
                    enabled = uiState.octave > 2,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("octave_down_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = stringResource(R.string.octave_down_desc),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.octave_label, uiState.octave),
                    style = MaterialTheme.typography.labelMedium,
                    color = HarmonicCyan,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = { onShiftOctave(1) },
                    enabled = uiState.octave < 6,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("octave_up_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.octave_up_desc),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Quick Timbre Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SynthTimbre.entries.forEach { timbre ->
                val selected = timbre == uiState.synthParams.timbre
                FilterChip(
                    selected = selected,
                    onClick = { onTimbreChange(timbre) },
                    label = {
                        Text(
                            text = timbre.shortLabel,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrassGold.copy(alpha = 0.22f),
                        selectedLabelColor = BrassGold,
                        selectedLeadingIconColor = BrassGold,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = BrassGold
                    ),
                    modifier = Modifier.testTag("timbre_chip_${timbre.name.lowercase()}")
                )
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = stringResource(R.string.info_torus_theory),
                    style = MaterialTheme.typography.headlineSmall,
                    color = BrassGold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.info_torus_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showInfoDialog = false },
                    modifier = Modifier.testTag("close_info_dialog_button")
                ) {
                    Text(
                        text = stringResource(R.string.action_close),
                        color = HarmonicCyan
                    )
                }
            }
        )
    }
}

@Composable
private fun ModePillButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            1.dp,
            if (selected) accent else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CompactToggleChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainer)
            .border(
                1.dp,
                if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
