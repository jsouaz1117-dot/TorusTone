package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.TorusAudioEngine
import com.example.data.PersistedStudioConfig
import com.example.data.SynthPreferencesRepository
import com.example.model.CanvasMode
import com.example.model.MusicalScale
import com.example.model.OrbitPattern
import com.example.model.OrbitVoicing
import com.example.model.SynthParams
import com.example.model.SynthTimbre
import com.example.model.Temperament
import com.example.model.TonnetzMath
import com.example.model.TouchMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TorusToneUiState(
    val synthParams: SynthParams = SynthParams(),
    val octave: Int = 4,
    val scale: MusicalScale = MusicalScale.MAJOR,
    val canvasMode: CanvasMode = CanvasMode.TORUS_3D,
    val touchMode: TouchMode = TouchMode.TRIAD,
    val latchEnabled: Boolean = false,
    val activeMidiNotes: Set<Int> = emptySet(),
    val activeChordLabel: String = "",
    val activeFrequencyHz: Double? = null,
    val lastTriggeredIsMinor: Boolean = false,
    // Orbit Sequencer State
    val orbitPlaying: Boolean = false,
    val orbitBpm: Int = 92,
    val orbitRootPitchClass: Int = 0,
    val orbitPattern: OrbitPattern = OrbitPattern.NEO_RIEMANNIAN_PLR,
    val orbitVoicing: OrbitVoicing = OrbitVoicing.CHORD,
    val orbitCurrentStep: Int = -1,
    val orbitEnabledSteps: List<Boolean> = List(8) { true }
)

class TorusToneViewModel(application: Application) : AndroidViewModel(application) {

    private val audioEngine = TorusAudioEngine()
    private val preferencesRepo = SynthPreferencesRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(TorusToneUiState())
    val uiState: StateFlow<TorusToneUiState> = _uiState.asStateFlow()

    val waveform: StateFlow<FloatArray> = audioEngine.waveform
    val peakLevel: StateFlow<Float> = audioEngine.peakLevel

    private var orbitJob: Job? = null
    private var auditionJob: Job? = null
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val saved = preferencesRepo.configFlow.first()
            _uiState.update { current ->
                current.copy(
                    synthParams = saved.synthParams,
                    octave = saved.octave,
                    scale = saved.scale,
                    canvasMode = saved.canvasMode,
                    touchMode = saved.touchMode,
                    orbitBpm = saved.orbitBpm,
                    orbitRootPitchClass = saved.orbitRootPitchClass,
                    orbitPattern = saved.orbitPattern,
                    orbitVoicing = saved.orbitVoicing
                )
            }
            audioEngine.updateParams(saved.synthParams, saved.orbitRootPitchClass)
        }
        audioEngine.start()
    }

    fun onAppResumed() {
        audioEngine.start()
    }

    fun onAppPaused() {
        stopOrbit()
        silenceAll()
        audioEngine.stop()
    }

    // --- Play Instrument Interactions ---

    fun triggerNotes(midiNotes: List<Int>, isMinor: Boolean = false) {
        if (midiNotes.isEmpty()) return
        val state = _uiState.value
        val newNotes: Set<Int> = if (state.latchEnabled) {
            val current = state.activeMidiNotes.toMutableSet()
            if (midiNotes.all { it in current }) {
                current.removeAll(midiNotes.toSet())
            } else {
                current.addAll(midiNotes)
            }
            current
        } else {
            midiNotes.toSet()
        }

        audioEngine.setActiveNotes(newNotes)
        updateActiveNoteReadout(newNotes, isMinor)
    }

    fun releaseTouchNotes() {
        if (_uiState.value.latchEnabled || _uiState.value.orbitPlaying) return
        audioEngine.allNotesOff()
        updateActiveNoteReadout(emptySet(), isMinor = false)
    }

    fun triggerPitchNode(pitchClass: Int, forceSingle: Boolean = false) {
        val state = _uiState.value
        val useTriad = !forceSingle && state.touchMode == TouchMode.TRIAD
        if (useTriad) {
            // Pick Major or Minor triad naturally matching current scale if possible
            val isMinor = chooseNaturalMinorQuality(pitchClass, state.scale, state.orbitRootPitchClass)
            val notes = TonnetzMath.buildTriadMidi(pitchClass, state.octave, isMinor)
            triggerNotes(notes, isMinor = isMinor)
        } else {
            val midi = TonnetzMath.buildSingleMidi(pitchClass, state.octave)
            triggerNotes(listOf(midi), isMinor = false)
        }
    }

    fun triggerTriad(rootPitchClass: Int, isMinor: Boolean) {
        val state = _uiState.value
        val notes = TonnetzMath.buildTriadMidi(rootPitchClass, state.octave, isMinor)
        triggerNotes(notes, isMinor = isMinor)
    }

    fun toggleLatch() {
        val next = !_uiState.value.latchEnabled
        _uiState.update { it.copy(latchEnabled = next) }
        if (!next && !_uiState.value.orbitPlaying) {
            silenceAll()
        }
    }

    fun silenceAll() {
        auditionJob?.cancel()
        audioEngine.allNotesOff()
        updateActiveNoteReadout(emptySet(), isMinor = false)
    }

    fun setCanvasMode(mode: CanvasMode) {
        _uiState.update { it.copy(canvasMode = mode) }
        schedulePersist()
    }

    fun setTouchMode(mode: TouchMode) {
        _uiState.update { it.copy(touchMode = mode) }
        schedulePersist()
    }

    fun setScale(scale: MusicalScale) {
        _uiState.update { it.copy(scale = scale) }
        schedulePersist()
    }

    fun shiftOctave(delta: Int) {
        _uiState.update { it.copy(octave = (it.octave + delta).coerceIn(2, 6)) }
        schedulePersist()
    }

    // --- Synth Sculpt Controls ---

    fun setTimbre(timbre: SynthTimbre) {
        updateSynthParams { it.copy(timbre = timbre) }
    }

    fun setWarmth(value: Float) {
        updateSynthParams { it.copy(warmth = value.coerceIn(0f, 1f)) }
    }

    fun setEchoMix(value: Float) {
        updateSynthParams { it.copy(echoMix = value.coerceIn(0f, 0.75f)) }
    }

    fun setEchoFeedback(value: Float) {
        updateSynthParams { it.copy(echoFeedback = value.coerceIn(0f, 0.76f)) }
    }

    fun setAttackMs(value: Float) {
        updateSynthParams { it.copy(attackMs = value.coerceIn(4f, 400f)) }
    }

    fun setReleaseMs(value: Float) {
        updateSynthParams { it.copy(releaseMs = value.coerceIn(80f, 2200f)) }
    }

    fun setDetuneCents(value: Float) {
        updateSynthParams { it.copy(detuneCents = value.coerceIn(0f, 35f)) }
    }

    fun setMasterGain(value: Float) {
        updateSynthParams { it.copy(masterGain = value.coerceIn(0.15f, 1f)) }
    }

    fun setTemperament(temperament: Temperament) {
        updateSynthParams { it.copy(temperament = temperament) }
    }

    fun setReferenceA4Hz(hz: Int) {
        updateSynthParams { it.copy(referenceA4Hz = hz) }
    }

    fun auditionChord(isMinor: Boolean, addSeventh: Boolean) {
        auditionJob?.cancel()
        val state = _uiState.value
        val notes = TonnetzMath.buildTriadMidi(
            rootPitchClass = state.orbitRootPitchClass,
            octave = state.octave,
            isMinor = isMinor,
            addSeventh = addSeventh
        )
        auditionJob = viewModelScope.launch {
            audioEngine.setActiveNotes(notes.toSet())
            updateActiveNoteReadout(notes.toSet(), isMinor)
            delay(880L)
            if (!_uiState.value.latchEnabled && !_uiState.value.orbitPlaying) {
                audioEngine.allNotesOff()
                updateActiveNoteReadout(emptySet(), false)
            }
        }
    }

    // --- Harmonic Orbit Sequencer ---

    fun toggleOrbitPlayback() {
        if (_uiState.value.orbitPlaying) {
            stopOrbit()
        } else {
            startOrbit()
        }
    }

    fun setOrbitBpm(bpm: Int) {
        _uiState.update { it.copy(orbitBpm = bpm.coerceIn(45, 160)) }
        schedulePersist()
    }

    fun setOrbitRoot(pitchClass: Int) {
        val pc = TonnetzMath.positiveMod12(pitchClass)
        _uiState.update { it.copy(orbitRootPitchClass = pc) }
        audioEngine.updateParams(_uiState.value.synthParams, pc)
        schedulePersist()
    }

    fun setOrbitPattern(pattern: OrbitPattern) {
        _uiState.update { it.copy(orbitPattern = pattern) }
        schedulePersist()
    }

    fun setOrbitVoicing(voicing: OrbitVoicing) {
        _uiState.update { it.copy(orbitVoicing = voicing) }
        schedulePersist()
    }

    fun toggleOrbitStep(index: Int) {
        _uiState.update { state ->
            val updated = state.orbitEnabledSteps.toMutableList()
            if (index in updated.indices) {
                updated[index] = !updated[index]
            }
            state.copy(orbitEnabledSteps = updated)
        }
    }

    private fun startOrbit() {
        orbitJob?.cancel()
        _uiState.update { it.copy(orbitPlaying = true, orbitCurrentStep = 0) }
        orbitJob = viewModelScope.launch {
            var stepIndex = 0
            while (isActive) {
                val state = _uiState.value
                val patternSteps = state.orbitPattern.steps
                val safeIndex = stepIndex % patternSteps.size
                val enabled = state.orbitEnabledSteps.getOrElse(safeIndex) { true }
                val stepMs = (60_000L / state.orbitBpm.coerceIn(45, 160)) * 2L // 2 beats per harmonic step

                _uiState.update { it.copy(orbitCurrentStep = safeIndex) }

                if (enabled) {
                    val step = patternSteps[safeIndex]
                    val rootPc = TonnetzMath.positiveMod12(state.orbitRootPitchClass + step.rootOffset)
                    val chordNotes = TonnetzMath.buildTriadMidi(
                        rootPitchClass = rootPc,
                        octave = state.octave,
                        isMinor = step.isMinor,
                        addSeventh = step.addSeventh
                    )

                    when (state.orbitVoicing) {
                        OrbitVoicing.CHORD -> {
                            audioEngine.setActiveNotes(chordNotes.toSet())
                            updateActiveNoteReadout(chordNotes.toSet(), step.isMinor)
                            delay((stepMs * 0.82).toLong())
                            if (!state.latchEnabled) {
                                audioEngine.allNotesOff()
                            }
                            delay((stepMs * 0.18).toLong())
                        }

                        OrbitVoicing.ARP_UP -> {
                            val seq = chordNotes + (chordNotes.first() + 12)
                            val subMs = (stepMs / seq.size).coerceAtLeast(80L)
                            for (midi in seq) {
                                if (!isActive) break
                                audioEngine.setActiveNotes(setOf(midi))
                                updateActiveNoteReadout(setOf(midi), step.isMinor)
                                delay((subMs * 0.85).toLong())
                                audioEngine.allNotesOff()
                                delay((subMs * 0.15).toLong())
                            }
                        }

                        OrbitVoicing.TORUS_WAVE -> {
                            // Strummed rolling wave that accumulates into full lush chord
                            val strumGap = (stepMs / 8L).coerceIn(55L, 160L)
                            val held = mutableSetOf<Int>()
                            for (midi in chordNotes) {
                                if (!isActive) break
                                held.add(midi)
                                audioEngine.noteOn(midi, 0.82f)
                                updateActiveNoteReadout(held.toSet(), step.isMinor)
                                delay(strumGap)
                            }
                            val remaining = (stepMs - strumGap * chordNotes.size).coerceAtLeast(120L)
                            delay((remaining * 0.8).toLong())
                            audioEngine.allNotesOff()
                            delay((remaining * 0.2).toLong())
                        }
                    }
                } else {
                    audioEngine.allNotesOff()
                    updateActiveNoteReadout(emptySet(), false)
                    delay(stepMs)
                }

                stepIndex = (safeIndex + 1) % patternSteps.size
            }
        }
    }

    private fun stopOrbit() {
        orbitJob?.cancel()
        orbitJob = null
        _uiState.update { it.copy(orbitPlaying = false, orbitCurrentStep = -1) }
        if (!_uiState.value.latchEnabled) {
            audioEngine.allNotesOff()
            updateActiveNoteReadout(emptySet(), false)
        }
    }

    private fun updateSynthParams(transform: (SynthParams) -> SynthParams) {
        val updated = transform(_uiState.value.synthParams)
        _uiState.update { it.copy(synthParams = updated) }
        audioEngine.updateParams(updated, _uiState.value.orbitRootPitchClass)
        schedulePersist()
    }

    private fun updateActiveNoteReadout(notes: Set<Int>, isMinor: Boolean) {
        val state = _uiState.value
        val freq = notes.minOrNull()?.let {
            TonnetzMath.midiToFrequency(
                midi = it,
                temperament = state.synthParams.temperament,
                referenceA4Hz = state.synthParams.referenceA4Hz,
                justRootPitchClass = state.orbitRootPitchClass
            )
        }
        _uiState.update {
            it.copy(
                activeMidiNotes = notes,
                activeChordLabel = TonnetzMath.detectChordLabel(notes),
                activeFrequencyHz = freq,
                lastTriggeredIsMinor = isMinor
            )
        }
    }

    private fun chooseNaturalMinorQuality(
        pitchClass: Int,
        scale: MusicalScale,
        keyRoot: Int
    ): Boolean {
        if (scale == MusicalScale.CHROMATIC) return false
        val relMinorThird = TonnetzMath.positiveMod12(pitchClass + 3 - keyRoot)
        val relMajorThird = TonnetzMath.positiveMod12(pitchClass + 4 - keyRoot)
        return relMinorThird in scale.intervals && relMajorThird !in scale.intervals
    }

    private fun schedulePersist() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(280L)
            val s = _uiState.value
            preferencesRepo.saveConfig(
                PersistedStudioConfig(
                    synthParams = s.synthParams,
                    octave = s.octave,
                    scale = s.scale,
                    canvasMode = s.canvasMode,
                    touchMode = s.touchMode,
                    orbitBpm = s.orbitBpm,
                    orbitRootPitchClass = s.orbitRootPitchClass,
                    orbitPattern = s.orbitPattern,
                    orbitVoicing = s.orbitVoicing
                )
            )
        }
    }

    override fun onCleared() {
        orbitJob?.cancel()
        auditionJob?.cancel()
        audioEngine.stop()
        super.onCleared()
    }
}
