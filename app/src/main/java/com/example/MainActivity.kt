package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.TorusToneViewModel
import com.example.ui.screens.OrbitScreen
import com.example.ui.screens.PlayScreen
import com.example.ui.screens.SculptScreen
import com.example.ui.theme.BrassGold
import com.example.ui.theme.TorusToneTheme

private enum class TorusTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    PLAY(R.string.nav_play, Icons.Filled.Public, Icons.Outlined.Public, "nav_play"),
    SCULPT(R.string.nav_sculpt, Icons.Filled.Tune, Icons.Outlined.Tune, "nav_sculpt"),
    ORBIT(R.string.nav_orbit, Icons.Filled.Loop, Icons.Outlined.Loop, "nav_orbit")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            TorusToneTheme {
                TorusToneApp()
            }
        }
    }
}

@Composable
fun TorusToneApp(
    viewModel: TorusToneViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val waveform by viewModel.waveform.collectAsStateWithLifecycle()
    val peakLevel by viewModel.peakLevel.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(TorusTab.PLAY) }

    // Return to primary Torus screen when pressing system Back from secondary tabs
    BackHandler(enabled = currentTab != TorusTab.PLAY) {
        currentTab = TorusTab.PLAY
    }

    // Lifecycle-aware audio engine management
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onAppResumed()
                Lifecycle.Event.ON_PAUSE -> viewModel.onAppPaused()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        if (useNavigationRail) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        TorusTab.entries.forEach { tab ->
                            val selected = tab == currentTab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = BrassGold,
                                    selectedTextColor = BrassGold,
                                    indicatorColor = BrassGold.copy(alpha = 0.16f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        TorusTabContent(
                            currentTab = currentTab,
                            uiState = uiState,
                            waveform = waveform,
                            peakLevel = peakLevel,
                            viewModel = viewModel
                        )
                    }
                }
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        TorusTab.entries.forEach { tab ->
                            val selected = tab == currentTab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BrassGold,
                                    selectedTextColor = BrassGold,
                                    indicatorColor = BrassGold.copy(alpha = 0.16f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    TorusTabContent(
                        currentTab = currentTab,
                        uiState = uiState,
                        waveform = waveform,
                        peakLevel = peakLevel,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun TorusTabContent(
    currentTab: TorusTab,
    uiState: com.example.ui.TorusToneUiState,
    waveform: FloatArray,
    peakLevel: Float,
    viewModel: TorusToneViewModel
) {
    when (currentTab) {
        TorusTab.PLAY -> {
            PlayScreen(
                uiState = uiState,
                waveform = waveform,
                peakLevel = peakLevel,
                onCanvasModeChange = viewModel::setCanvasMode,
                onTouchModeChange = viewModel::setTouchMode,
                onScaleChange = viewModel::setScale,
                onTimbreChange = viewModel::setTimbre,
                onToggleLatch = viewModel::toggleLatch,
                onSilenceAll = viewModel::silenceAll,
                onShiftOctave = viewModel::shiftOctave,
                onNodePressed = { pc -> viewModel.triggerPitchNode(pc) },
                onTriadPressed = viewModel::triggerTriad,
                onSingleNotePressed = { pc -> viewModel.triggerPitchNode(pc, forceSingle = true) },
                onReleased = viewModel::releaseTouchNotes
            )
        }

        TorusTab.SCULPT -> {
            SculptScreen(
                uiState = uiState,
                waveform = waveform,
                peakLevel = peakLevel,
                onTimbreChange = viewModel::setTimbre,
                onWarmthChange = viewModel::setWarmth,
                onEchoMixChange = viewModel::setEchoMix,
                onEchoFeedbackChange = viewModel::setEchoFeedback,
                onAttackChange = viewModel::setAttackMs,
                onReleaseChange = viewModel::setReleaseMs,
                onDetuneChange = viewModel::setDetuneCents,
                onMasterGainChange = viewModel::setMasterGain,
                onTemperamentChange = viewModel::setTemperament,
                onReferenceA4Change = viewModel::setReferenceA4Hz,
                onAuditionChord = viewModel::auditionChord
            )
        }

        TorusTab.ORBIT -> {
            OrbitScreen(
                uiState = uiState,
                onToggleOrbit = viewModel::toggleOrbitPlayback,
                onBpmChange = viewModel::setOrbitBpm,
                onRootChange = viewModel::setOrbitRoot,
                onPatternChange = viewModel::setOrbitPattern,
                onVoicingChange = viewModel::setOrbitVoicing,
                onToggleStep = viewModel::toggleOrbitStep
            )
        }
    }
}
